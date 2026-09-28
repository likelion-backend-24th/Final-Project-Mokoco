package com.team2.postservice.contract.repository;

import com.team2.postservice.contract.entity.RepairContract;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ContractRepository extends JpaRepository<RepairContract, Long> {
    Optional<RepairContract> findFirstByChatRoomIdOrderByRevisionDesc(Long roomId);
    List<RepairContract> findByChatRoomIdOrderByRevisionDesc(Long roomId);
}
