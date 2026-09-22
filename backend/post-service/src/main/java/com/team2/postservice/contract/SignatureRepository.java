package com.team2.postservice.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SignatureRepository extends JpaRepository<ContractSignature, Long> {
    List<ContractSignature> findByContractIdOrderBySignedAtAsc(Long contractId);
}
