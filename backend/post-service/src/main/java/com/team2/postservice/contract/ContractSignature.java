package com.team2.postservice.contract;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "contract_signatures", uniqueConstraints = @UniqueConstraint(columnNames = {"contract_id", "signer_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContractSignature {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "contract_id", nullable = false) private Long contractId;
    @Column(name = "signer_id", nullable = false) private Long signerId;
    @Column(nullable = false, length = 80) private String signerName;
    @Column(nullable = false, length = 64) private String documentHash;
    @Column(nullable = false, length = 500) private String consentText;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    @Column(nullable = false) private Instant signedAt;
    public ContractSignature(Long contractId, Long signerId, String name, String hash, String consent) {
        this.contractId = contractId; this.signerId = signerId; this.signerName = name;
        this.documentHash = hash; this.consentText = consent; this.signedAt = Instant.now();
    }
}
