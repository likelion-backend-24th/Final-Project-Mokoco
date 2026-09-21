package com.team2.postservice;

import com.team2.common.exception.CustomException;
import com.team2.postservice.client.*;
import com.team2.postservice.fixDeal.entity.*;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.fixDeal.service.FixDealService;
import com.team2.postservice.notification.service.NotificationService;
import com.team2.postservice.post.entity.*;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import com.team2.postservice.proposal.service.ProposalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({ProposalService.class, FixDealService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProposalConcurrencyTest extends FlywaySchemaTest {
    @Autowired ProposalService service;
    @Autowired FixDealService dealService;
    @Autowired ProposalRepository proposals;
    @Autowired PostRepository posts;
    @Autowired FixDealRepository deals;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean UserClient users;
    @MockitoBean ChatClient chat;
    @MockitoBean PaymentClient payments;
    @MockitoBean NotificationService notifications;
    Long postId;
    Long firstId;
    Long secondId;

    @BeforeEach void seed() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            Post post = posts.save(Post.builder().title("Repair").content("Repair").authorId(1L)
                    .regionName("Seoul").category(PostCategory.values()[0]).build());
            postId = post.getId();
            firstId = proposals.save(proposal(post)).getId();
            secondId = proposals.save(proposal(post)).getId();
        });
    }

    private Proposal proposal(Post post) {
        return Proposal.builder().post(post).repairerId(2L).content("Repair").estimatedPrice(100).build();
    }

    private List<Boolean> race(Runnable first, Runnable second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> futures = List.of(first, second).stream().map(action -> pool.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                try { action.run(); return true; }
                catch (CustomException rejected) { return false; }
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        } finally { start.countDown(); }
    }

    @Test void competingProposalsHaveExactlyOneWinner() throws Exception {
        assertThat(race(() -> service.adoptProposal(postId, firstId, 1L),
                () -> service.adoptProposal(postId, secondId, 1L))).containsExactlyInAnyOrder(true, false);
        assertThat(List.of(proposals.findById(firstId).orElseThrow(), proposals.findById(secondId).orElseThrow())
                .stream().filter(Proposal::isAdopted).count()).isEqualTo(1);
        assertThat(deals.findByPostId(postId)).isPresent();
    }

    @Test void repeatedConcurrentAdoptionCreatesOneDeal() throws Exception {
        assertThat(race(() -> service.adoptProposal(postId, firstId, 1L),
                () -> service.adoptProposal(postId, firstId, 1L))).containsOnly(true);
        Long dealId = deals.findByProposalId(firstId).orElseThrow().getId();
        service.adoptProposal(postId, firstId, 1L);
        assertThat(deals.findByProposalId(firstId).orElseThrow().getId()).isEqualTo(dealId);
    }

    @Test void cancellationRacesWithProgressWithoutResurrectingDeal() throws Exception {
        service.adoptProposal(postId, firstId, 1L);
        Long dealId = deals.findByProposalId(firstId).orElseThrow().getId();
        assertThat(race(() -> service.cancelProposal(postId, firstId, 1L),
                () -> dealService.markProductSent(dealId, 2L))).containsExactlyInAnyOrder(true, false);
        FixDeal deal = deals.findById(dealId).orElseThrow();
        boolean canceled = deal.getStatus() == FixDealStatus.CANCELED;
        assertThat(proposals.findById(firstId).orElseThrow().isAdopted()).isEqualTo(!canceled);
        assertThat(posts.findById(postId).orElseThrow().getStatus())
                .isEqualTo(canceled ? PostStatus.WAITING : PostStatus.MATCHED);
    }

    @Test void cancellationAndReadoptionPreserveHistoryAndCurrentLookup() {
        service.adoptProposal(postId, firstId, 1L);
        Long oldId = deals.findByProposalId(firstId).orElseThrow().getId();
        service.cancelProposal(postId, firstId, 1L);
        service.cancelProposal(postId, firstId, 1L);
        service.adoptProposal(postId, firstId, 1L);
        assertThat(deals.findById(oldId).orElseThrow().getStatus()).isEqualTo(FixDealStatus.CANCELED);
        assertThat(deals.findByPostId(postId).orElseThrow().getId()).isNotEqualTo(oldId);
    }

    @Test void databaseRejectsDuplicateActivePostAndProposalEvenWithoutServiceLock() {
        service.adoptProposal(postId, firstId, 1L);
        assertThatThrownBy(() -> deals.saveAndFlush(FixDeal.builder().postId(postId).proposalId(secondId)
                .requesterId(1L).repairerId(2L).build())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> deals.saveAndFlush(FixDeal.builder().postId(postId + 100000).proposalId(firstId)
                .requesterId(1L).repairerId(2L).build())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> new TransactionTemplate(manager).executeWithoutResult(status -> {
            proposals.findById(secondId).orElseThrow().adopt();
            proposals.flush();
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void repeatedProgressRequestsAreIdempotent() throws Exception {
        service.adoptProposal(postId, firstId, 1L);
        Long dealId = deals.findByProposalId(firstId).orElseThrow().getId();
        assertThat(race(() -> dealService.markProductSent(dealId, 2L),
                () -> dealService.markProductSent(dealId, 2L))).containsOnly(true);
        assertThat(deals.findById(dealId).orElseThrow().getStatus()).isEqualTo(FixDealStatus.PRODUCT_SENT);
    }
    @Test void concurrentCompletionChecksPaymentOnceAndPreservesCompletionTime() throws Exception {
        service.adoptProposal(postId, firstId, 1L);
        Long dealId = deals.findByProposalId(firstId).orElseThrow().getId();
        dealService.markProductSent(dealId, 2L);
        dealService.markRepairing(dealId, 2L);
        dealService.requestCompletion(dealId, 2L);
        when(payments.getPaymentByPostId(postId))
                .thenReturn(new com.team2.postservice.client.dto.PaymentClientResponse(1L, postId, "COMPLETED"));
        assertThat(race(() -> dealService.acceptCompletion(dealId, 1L),
                () -> dealService.acceptCompletion(dealId, 1L))).containsOnly(true);
        java.time.LocalDateTime completedAt = deals.findById(dealId).orElseThrow().getCompletedAt();
        dealService.acceptCompletion(dealId, 1L);
        assertThat(deals.findById(dealId).orElseThrow().getCompletedAt()).isNotNull().isEqualTo(completedAt);
        verify(payments, times(1)).getPaymentByPostId(postId);
        assertThatThrownBy(() -> service.cancelProposal(postId, firstId, 1L)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> deals.saveAndFlush(FixDeal.builder().postId(postId).proposalId(secondId)
                .requesterId(1L).repairerId(2L).build())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void completedPostCannotBeMatchedAgain() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            Post post = posts.findById(postId).orElseThrow();
            post.updateStatusToMatched();
            post.changeStatus(PostStatus.COMPLETED);
            assertThatThrownBy(post::updateStatusToMatched).isInstanceOf(CustomException.class);
        });
        assertThatThrownBy(() -> service.adoptProposal(postId, firstId, 1L))
                .isInstanceOf(CustomException.class);
        assertThat(proposals.findById(firstId).orElseThrow().isAdopted()).isFalse();
        assertThat(deals.findByPostId(postId)).isEmpty();
    }

}
