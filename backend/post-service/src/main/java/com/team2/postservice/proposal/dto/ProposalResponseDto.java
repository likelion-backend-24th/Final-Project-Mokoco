package com.team2.postservice.proposal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team2.postservice.proposal.entity.Proposal;
import lombok.Getter;

@Getter
public class ProposalResponseDto {

    private final Long id;
    private final Long postId;
    private final String repairerEmail;
    private final Integer estimatedPrice;
    private final String content;
    private final Long fixDealId;
    private final String repairerRegion; // 수리공 활동 지역 (시도+시군구 정도만, 동까지는 노출 안 함)
    private final long repairerCompletedCount; // 그 수리공이 완료까지 마친 거래 건수

    @JsonProperty("isAdopted")
    private final boolean isAdopted;

    public ProposalResponseDto(Proposal proposal) {
        this(proposal, null, null, 0);
    }

    public ProposalResponseDto(Proposal proposal, Long fixDealId, String repairerRegion, long repairerCompletedCount) {
        this.fixDealId = fixDealId;
        this.id = proposal.getId();
        this.postId = proposal.getPost() != null ? proposal.getPost().getId() : null;
        this.repairerEmail = proposal.getRepairerEmail();
        this.estimatedPrice = proposal.getEstimatedPrice();
        this.content = proposal.getContent();
        this.isAdopted = proposal.isAdopted();
        this.repairerRegion = repairerRegion;
        this.repairerCompletedCount = repairerCompletedCount;
    }
}