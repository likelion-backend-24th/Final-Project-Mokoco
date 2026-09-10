package com.team2.postservice.contract;

import com.team2.postservice.chatRoom.entity.ChatRoom;
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

@DataJpaTest
@Import({ContractService.class, ContractServiceTest.JsonConfig.class})
class ContractServiceTest {
    @TestConfiguration static class JsonConfig {
        @Bean ObjectMapper mapper() { return new ObjectMapper().findAndRegisterModules(); }
    }
    @Autowired TestEntityManager em;
    @Autowired ContractService service;
    Long roomId;
    @BeforeEach void setup() {
        var deal = em.persist(FixDeal.builder().postId(1L).proposalId(1L).requesterId(10L).repairerId(20L).build());
        roomId = em.persist(ChatRoom.builder().fixDeal(deal).build()).getId();
    }
    ContractTerms terms(String scope) {
        return new ContractTerms("가구 수리", scope, "도색 제외", "부품비 포함", new BigDecimal("50000"), "검수 후 지급",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), "방문 작업", "흔들림 없음", "30일 재수리", "착수 전 취소 가능", "추가 비용 사전 승인");
    }
    ContractService.Version signing() {
        var draft = service.draft(roomId, 10L, null, terms("의자 다리 수리"));
        return service.request(roomId, 10L, draft.id());
    }
    @Test void bothSignaturesRequiredBeforeRepairerCanStartAndRequesterCanAccept() {
        var contract = signing();
        assertThatThrownBy(() -> service.advance(roomId, 20L, contract.id(), "start")).isInstanceOf(ResponseStatusException.class);
        service.sign(roomId, 10L, contract.id(), contract.documentHash(), "의뢰인", true);
        assertThat(service.get(roomId, 10L).versions().getFirst().status()).isEqualTo("SIGNING");
        var signed = service.sign(roomId, 20L, contract.id(), contract.documentHash(), "수리자", true);
        assertThat(signed.status()).isEqualTo("SIGNED");
        assertThat(signed.signatures()).hasSize(2);
        assertThatThrownBy(() -> service.advance(roomId, 10L, contract.id(), "start")).isInstanceOf(ResponseStatusException.class);
        service.advance(roomId, 20L, contract.id(), "start");
        assertThat(service.get(roomId, 10L).dealStatus()).isEqualTo(FixDealStatus.REPAIRING);
        assertThatThrownBy(() -> service.advance(roomId, 10L, contract.id(), "accept")).isInstanceOf(ResponseStatusException.class);
        service.advance(roomId, 20L, contract.id(), "finish");
        service.advance(roomId, 10L, contract.id(), "accept");
        assertThat(service.get(roomId, 10L).dealStatus()).isEqualTo(FixDealStatus.COMPLETED);
    }
    @Test void revisionPreservesOldContentAndDoesNotReuseSignatures() {
        var first = signing();
        service.sign(roomId, 10L, first.id(), first.documentHash(), "의뢰인", true);
        var second = service.draft(roomId, 20L, first.id(), terms("의자 다리와 등받이 수리"));
        assertThat(second.revision()).isEqualTo(2);
        assertThat(second.documentHash()).isNotEqualTo(first.documentHash());
        assertThat(second.signatures()).isEmpty();
        var old = service.get(roomId, 10L).versions().get(1);
        assertThat(old.status()).isEqualTo("SUPERSEDED");
        assertThat(old.signatures()).hasSize(1);
        assertThat(old.terms().scope()).isEqualTo("의자 다리 수리");
        assertThatThrownBy(() -> service.sign(roomId, 20L, first.id(), first.documentHash(), "수리자", true)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.draft(roomId, 10L, first.id(), terms("stale"))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsOutsiderHashMismatchAndMissingConsent() {
        var contract = signing();
        assertThatThrownBy(() -> service.get(roomId, 99L)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.sign(roomId, 99L, contract.id(), contract.documentHash(), "외부인", true)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.sign(roomId, 10L, contract.id(), "changed", "의뢰인", true)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.sign(roomId, 10L, contract.id(), contract.documentHash(), "의뢰인", false)).isInstanceOf(ResponseStatusException.class);
        assertThat(service.get(roomId, 10L).versions().getFirst().signatures()).isEmpty();
    }
    @Test void duplicateSigningIsIdempotentAndSignedTermsAreImmutable() {
        var contract = signing();
        service.sign(roomId, 10L, contract.id(), contract.documentHash(), "의뢰인", true);
        service.sign(roomId, 10L, contract.id(), contract.documentHash(), "다른 이름", true);
        var signed = service.sign(roomId, 20L, contract.id(), contract.documentHash(), "수리자", true);
        assertThat(signed.signatures()).hasSize(2);
        assertThat(signed.signatures().getFirst().getSignerName()).isEqualTo("의뢰인");
        assertThatThrownBy(() -> service.draft(roomId, 10L, contract.id(), terms("변경"))).isInstanceOf(ResponseStatusException.class);
    }
}
