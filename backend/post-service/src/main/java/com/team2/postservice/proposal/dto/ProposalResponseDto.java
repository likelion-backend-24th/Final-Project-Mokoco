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

    @JsonProperty("isAdopted")
    private final boolean isAdopted;

    private final boolean attachResume;

    private final String repairerNickname;

    public ProposalResponseDto(Proposal proposal) {
        this(proposal, null, null);
    }

    public ProposalResponseDto(Proposal proposal, Long fixDealId) {
        this(proposal, fixDealId, null);
    }

    public ProposalResponseDto(Proposal proposal, Long fixDealId, String repairerNickname) {
        this.fixDealId = fixDealId;
        this.id = proposal.getId();
        this.postId = proposal.getPost() != null ? proposal.getPost().getId() : null;
        this.repairerEmail = proposal.getRepairerEmail();
        this.estimatedPrice = proposal.getEstimatedPrice();
        this.content = proposal.getContent();
        this.isAdopted = proposal.isAdopted();
        this.attachResume = proposal.isAttachResume();
        this.repairerNickname = repairerNickname;
    }
}