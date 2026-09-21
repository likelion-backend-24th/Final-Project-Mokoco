package com.team2.postservice.contract;

import com.team2.common.chat.ChatRoomInfo;
import com.team2.postservice.client.ChatClient;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.proposal.entity.Proposal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.*;
import com.team2.postservice.contract.dto.ContractTerms;
import com.team2.postservice.contract.dto.Version;
import com.team2.postservice.contract.entity.RepairContract;
import com.team2.postservice.contract.service.ContractService;
import com.team2.postservice.fixDeal.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
@Import({ContractService.class, ContractServiceTest.JsonConfig.class})
class ContractServiceTest {
    @TestConfiguration static class JsonConfig {
        @Bean ObjectMapper mapper() { return new ObjectMapper().findAndRegisterModules(); }
    }
    @Autowired TestEntityManager em;
    @Autowired ContractService service;
    Long roomId;
    @MockitoBean ChatClient chat;
    @MockitoBean com.team2.postservice.client.PaymentClient payments;

    @BeforeEach void setup() {
        Post post = em.persist(Post.builder().title("Repair").content("Repair").authorId(10L)
                .regionName("Seoul").category(PostCategory.values()[0]).build());
        Proposal proposal = em.persist(Proposal.builder().post(post).repairerId(20L).estimatedPrice(50000).content("Repair").build());
        proposal.adopt();
        FixDeal deal = em.persist(FixDeal.builder().postId(post.getId()).proposalId(proposal.getId()).requesterId(10L).repairerId(20L).build());
        roomId = 100L;
        when(payments.getPaymentByPostId(post.getId())).thenReturn(new com.team2.postservice.client.dto.PaymentClientResponse(1L, post.getId(), "COMPLETED"));
        when(chat.getRoom(roomId)).thenReturn(new ChatRoomInfo(roomId, deal.getId(), proposal.getId(), 10L, 20L, null));
    }
    @Test void consultationRoomRejectsContractsUntilAdoption() {
        Long consultation = 999L;
        when(chat.getRoom(consultation)).thenReturn(new ChatRoomInfo(consultation, null, null, 10L, 20L, null));
        assertThatThrownBy(() -> service.get(consultation, 10L)).isInstanceOfSatisfying(ResponseStatusException.class,
                failure -> assertThat(failure.getStatusCode().value()).isEqualTo(409));
        assertThatThrownBy(() -> service.draft(consultation, 20L, null, terms("수리"))).isInstanceOfSatisfying(ResponseStatusException.class,
                failure -> assertThat(failure.getStatusCode().value()).isEqualTo(409));
        assertThatThrownBy(() -> service.get(consultation, 99L)).isInstanceOfSatisfying(ResponseStatusException.class,
                failure -> assertThat(failure.getStatusCode().value()).isEqualTo(403));
    }    ContractTerms terms(String scope) {
        return new ContractTerms("가구 수리", scope, "도색 제외", "부품비 포함", new BigDecimal("50000"), "검수 후 지급",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), "방문 작업", "흔들림 없음", "30일 재수리", "착수 전 취소 가능", "추가 비용 사전 승인");
    }
    Version signing() {
        Version draft = service.draft(roomId, 10L, null, terms("의자 다리 수리"));
        return service.request(roomId, 10L, draft.id());
    }
    @Test void bothSignaturesRequiredBeforeRepairerCanStartAndRequesterCanAccept() {
        Version contract = signing();
        assertThatThrownBy(() -> service.advance(roomId, 20L, contract.id(), "start")).isInstanceOf(ResponseStatusException.class);
        service.sign(roomId, 10L, contract.id(), contract.documentHash(), "의뢰인", true);
        assertThat(service.get(roomId, 10L).versions().getFirst().status()).isEqualTo("SIGNING");
        Version signed = service.sign(roomId, 20L, contract.id(), contract.documentHash(), "수리자", true);
        assertThat(signed.status()).isEqualTo("SIGNED");
        assertThat(signed.signatures()).hasSize(2);
        assertThatThrownBy(() -> service.advance(roomId, 10L, contract.id(), "start")).isInstanceOf(ResponseStatusException.class);
        service.advance(roomId, 20L, contract.id(), "start");
        assertThat(service.get(roomId, 10L).dealStatus()).isEqualTo(FixDealStatus.REPAIRING);
        assertThatThrownBy(() -> service.advance(roomId, 10L, contract.id(), "accept")).isInstanceOf(ResponseStatusException.class);
        service.advance(roomId, 20L, contract.id(), "finish");
        when(payments.getPaymentByPostId(anyLong())).thenReturn(new com.team2.postservice.client.dto.PaymentClientResponse(1L, 1L, "FAILED"));
        assertThatThrownBy(() -> service.advance(roomId, 10L, contract.id(), "accept")).isInstanceOf(ResponseStatusException.class);
        when(payments.getPaymentByPostId(anyLong())).thenReturn(new com.team2.postservice.client.dto.PaymentClientResponse(1L, 1L, "COMPLETED"));
        service.advance(roomId, 10L, contract.id(), "accept");
        assertThat(service.get(roomId, 10L).dealStatus()).isEqualTo(FixDealStatus.COMPLETED);
    }
    @Test void revisionPreservesOldContentAndDoesNotReuseSignatures() {
        Version first = signing();
        service.sign(roomId, 10L, first.id(), first.documentHash(), "의뢰인", true);
        Version second = service.draft(roomId, 20L, first.id(), terms("의자 다리와 등받이 수리"));
        assertThat(second.revision()).isEqualTo(2);
        assertThat(second.documentHash()).isNotEqualTo(first.documentHash());
        assertThat(second.signatures()).isEmpty();
        Version old = service.get(roomId, 10L).versions().get(1);
        assertThat(old.status()).isEqualTo("SUPERSEDED");
        assertThat(old.signatures()).hasSize(1);
        assertThat(old.terms().scope()).isEqualTo("의자 다리 수리");
        assertThatThrownBy(() -> service.sign(roomId, 20L, first.id(), first.documentHash(), "수리자", true)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.draft(roomId, 10L, first.id(), terms("stale"))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsOutsiderHashMismatchAndMissingConsent() {
        Version contract = signing();
        assertThatThrownBy(() -> service.get(roomId, 99L)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.sign(roomId, 99L, contract.id(), contract.documentHash(), "외부인", true)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.sign(roomId, 10L, contract.id(), "changed", "의뢰인", true)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.sign(roomId, 10L, contract.id(), contract.documentHash(), "의뢰인", false)).isInstanceOf(ResponseStatusException.class);
        assertThat(service.get(roomId, 10L).versions().getFirst().signatures()).isEmpty();
    }
    @Test void duplicateSigningIsIdempotentAndSignedTermsAreImmutable() {
        Version contract = signing();
        service.sign(roomId, 10L, contract.id(), contract.documentHash(), "의뢰인", true);
        service.sign(roomId, 10L, contract.id(), contract.documentHash(), "다른 이름", true);
        Version signed = service.sign(roomId, 20L, contract.id(), contract.documentHash(), "수리자", true);
        assertThat(signed.signatures()).hasSize(2);
        assertThat(signed.signatures().getFirst().getSignerName()).isEqualTo("의뢰인");
        assertThatThrownBy(() -> service.draft(roomId, 10L, contract.id(), terms("변경"))).isInstanceOf(ResponseStatusException.class);
    }
}
