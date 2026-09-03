package com.team2.postservice.proposal.repository;

import com.team2.postservice.proposal.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findAllByPostId(Long postId);
}