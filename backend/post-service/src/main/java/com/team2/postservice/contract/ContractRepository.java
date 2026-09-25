package com.team2.postservice.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ContractRepository extends JpaRepository<RepairContract, Long> {
    Optional<RepairContract> findFirstByChatRoomIdOrderByRevisionDesc(Long roomId);
    List<RepairContract> findByChatRoomIdOrderByRevisionDesc(Long roomId);

    // 프로필의 거래 목록이 "계약서 보기" 버튼을 실제로 계약서가 있는 항목에만 보여주려고,
    // 채팅방 id 여러 개를 한 번에 넘겨 그중 계약서가 하나라도 있는 채팅방 id만 돌려받는다.
    // 메서드 이름만으로는(findDistinctChatRoomIdByChatRoomIdIn) Spring Data가 chatRoomId
    // 필드 하나만 뽑는 프로젝션으로 해석하지 않고 엔티티 전체를 반환하는 쿼리를 만들어서,
    // 반환 타입 List<Long>과 실제 결과(RepairContract)가 실행 시점에 어긋나는 버그가 있었다
    // (앱 기동 시점엔 검증이 안 돼서 조용히 넘어갔다가, 실제로 호출될 때만 터졌다) — 그래서
    // 명시적 JPQL로 어떤 필드를 뽑을지 확실히 적는다.
    @Query("select distinct c.chatRoomId from RepairContract c where c.chatRoomId in :chatRoomIds")
    List<Long> findDistinctChatRoomIdByChatRoomIdIn(@Param("chatRoomIds") Collection<Long> chatRoomIds);
}
