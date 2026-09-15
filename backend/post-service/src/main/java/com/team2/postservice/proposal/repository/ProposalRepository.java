package com.team2.postservice.proposal.repository;

import com.team2.postservice.post.entity.Post;
import com.team2.postservice.proposal.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Proposal p where p.id = :id")
    java.util.Optional<Proposal> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    List<Proposal> findAllByPostId(Long postId);

    // 특정 수리 요청(Post)에 달린 모든 제안 목록 조회
    List<Proposal> findByPost(Post post);
}