package com.team2.postservice.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ContractRepository extends JpaRepository<RepairContract, Long> {
    Optional<RepairContract> findFirstByChatRoomIdOrderByRevisionDesc(Long roomId);
    List<RepairContract> findByChatRoomIdOrderByRevisionDesc(Long roomId);

    // 프로필의 거래 목록이 "계약서 보기" 버튼을 실제로 계약서가 있는 항목에만 보여주려고,
    // 채팅방 id 여러 개를 한 번에 넘겨 그중 계약서가 하나라도 있는 채팅방 id만 돌려받는다.
    List<Long> findDistinctChatRoomIdByChatRoomIdIn(Collection<Long> chatRoomIds);
}
