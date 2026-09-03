package com.team2.postservice.proposal.repository;

import com.team2.postservice.post.entity.Post;
import com.team2.postservice.proposal.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findAllByPostId(Long postId);

    // 특정 수리 요청(Post)에 달린 모든 제안 목록 조회
    List<Proposal> findByPost(Post post);
}