package com.team2.postservice.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team2.postservice.contract.entity.ContractSignature;
import com.team2.postservice.contract.dto.ContractTerms;
import java.time.Instant;
import java.util.List;

public record Version(Long id, int revision, String status, Long authorId, ContractTerms terms,
                      String documentHash,
                      @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
                      @JsonFormat(shape = JsonFormat.Shape.STRING) Instant requestedAt,
                      @JsonFormat(shape = JsonFormat.Shape.STRING) Instant signedAt,
                      List<ContractSignature> signatures) {}