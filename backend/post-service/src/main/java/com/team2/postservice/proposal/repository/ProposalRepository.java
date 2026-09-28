package com.team2.postservice.proposal.repository;

import com.team2.postservice.post.entity.Post;
import com.team2.postservice.proposal.entity.Proposal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Proposal p where p.id = :id")
    Optional<Proposal> lockById(@Param("id") Long id);

    List<Proposal> findAllByPostId(Long postId);

    // 특정 수리 요청(Post)에 달린 모든 제안 목록 조회
    List<Proposal> findByPost(Post post);
}
