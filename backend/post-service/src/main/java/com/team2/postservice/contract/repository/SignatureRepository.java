package com.team2.postservice.contract.repository;

import com.team2.postservice.contract.entity.ContractSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SignatureRepository extends JpaRepository<ContractSignature, Long> {
    List<ContractSignature> findByContractIdOrderBySignedAtAsc(Long contractId);
}
