package com.team2.postservice.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ProfileServiceTest는 ContractRepository를 Mockito로 흉내 내서, findDistinctChatRoomIdByChatRoomIdIn의
// 실제 JPQL이 Hibernate에서 진짜로 실행되는지는 검증하지 못했다 — 그래서 메서드 이름만으로 만든 쿼리가
// chatRoomId 하나만 뽑는 대신 엔티티 전체를 반환해버리는 버그(QueryTypeMismatchException)가 그대로
// 배포됐다. 이 테스트는 목(mock) 없이 실제로 쿼리를 실행해서 그 클래스의 버그를 잡는다.
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class ContractRepositoryTest {
    @Autowired TestEntityManager em;
    @Autowired ContractRepository repository;

    @Test void returnsOnlyDistinctChatRoomIdsThatHaveAContract() {
        em.persistAndFlush(new RepairContract(100L, 1, 1L, "{}", "hash-a"));
        em.persistAndFlush(new RepairContract(100L, 2, 1L, "{}", "hash-a2")); // 같은 방의 개정판 — 중복 제거돼야 함
        em.persistAndFlush(new RepairContract(200L, 1, 2L, "{}", "hash-b"));
        // 300L은 계약서가 아예 없는 채팅방 — 결과에 나오면 안 됨.

        List<Long> result = repository.findDistinctChatRoomIdByChatRoomIdIn(List.of(100L, 200L, 300L));

        assertThat(result).containsExactlyInAnyOrder(100L, 200L);
    }

    @Test void returnsEmptyWhenNoneOfTheGivenRoomsHaveAContract() {
        em.persistAndFlush(new RepairContract(100L, 1, 1L, "{}", "hash-a"));

        List<Long> result = repository.findDistinctChatRoomIdByChatRoomIdIn(List.of(999L));

        assertThat(result).isEmpty();
    }
}
