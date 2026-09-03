package com.team2.postservice.proposal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team2.postservice.proposal.entity.Proposal;
import lombok.Getter;

@Getter
public class ProposalResponseDto {

    private final Long id;
    private final Long postId;
    private final String repairerEmail;
    private final String content;

    @JsonProperty("isAdopted")
    private final boolean isAdopted;

    public ProposalResponseDto(Proposal proposal) {
        this.id = proposal.getId();
        this.postId = proposal.getPost() != null ? proposal.getPost().getId() : null;
        this.repairerEmail = proposal.getRepairerEmail();
        this.content = proposal.getContent();
        this.isAdopted = proposal.isAdopted();
    }
}