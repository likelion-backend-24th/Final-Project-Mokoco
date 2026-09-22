package com.team2.postservice.post.repository;

import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.team2.postservice.post.dto.NearbyRepairRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 아직 공개 상태로 제안을 기다리는데(WAITING), 제안이 하나도 없고 마지막으로 끌어올린 지 오래된 게시글
    @Query("""
        select p from Post p
        where p.publiclyVisible = true and p.status = com.team2.postservice.post.entity.PostStatus.WAITING
          and p.bumpedAt <= :cutoff
          and not exists (select 1 from Proposal pr where pr.post = p)
        """)
    List<Post> findWaitingPostsWithoutProposalsBumpedBefore(@Param("cutoff") LocalDateTime cutoff);

    // 제안은 도착했지만 아직 하나도 채택하지 않은 게시글
    @Query("""
        select p from Post p
        where p.publiclyVisible = true and p.status = com.team2.postservice.post.entity.PostStatus.WAITING
          and exists (select 1 from Proposal pr where pr.post = p)
          and not exists (select 1 from Proposal pr2 where pr2.post = p and pr2.isAdopted = true)
        """)
    List<Post> findWaitingPostsWithUnadoptedProposals();

    @Query(value = """
        select new com.team2.postservice.post.dto.NearbyRepairRequest(
            p.id, p.title, p.content, p.authorEmail, p.category, p.status, p.regionCode, p.regionName, p.createdAt,
            (select i.imageUrl from PostImage i where i.post = p
                and i.id = (select min(firstImage.id) from PostImage firstImage where firstImage.post = p)))
        from Post p
        where (:regionPattern is null or p.regionCode like :regionPattern) and p.publiclyVisible = true
          and p.status = com.team2.postservice.post.entity.PostStatus.WAITING
          and (:category is null or p.category = :category)
        """, countQuery = """
        select count(p) from Post p
        where (:regionPattern is null or p.regionCode like :regionPattern) and p.publiclyVisible = true
          and p.status = com.team2.postservice.post.entity.PostStatus.WAITING
          and (:category is null or p.category = :category)
        """)
    Page<NearbyRepairRequest> findNearby(
            @Param("regionPattern") String regionPattern,
            @Param("category") PostCategory category,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> lockById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = "images")
    Optional<Post> findById(Long id);

}
