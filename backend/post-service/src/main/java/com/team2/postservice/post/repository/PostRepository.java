package com.team2.postservice.post.repository;

import com.team2.postservice.post.dto.NearbyRepairRequest;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(
            value = """
                    select new com.team2.postservice.post.dto.NearbyRepairRequest(
                        p.id,
                        p.title,
                        p.content,
                        p.contentFormat,
                        p.authorEmail,
                        p.category,
                        p.status,
                        p.regionCode,
                        p.regionName,
                        p.createdAt,
                        (
                            select i.imageUrl
                            from PostImage i
                            where i.post = p
                              and i.id = (
                                  select min(firstImage.id)
                                  from PostImage firstImage
                                  where firstImage.post = p
                              )
                        ),
                        null
                    )
                    from Post p
                    where (:regionPattern is null or p.regionCode like :regionPattern)
                      and p.publiclyVisible = true
                      and p.status = com.team2.postservice.post.entity.PostStatus.WAITING
                      and (:category is null or p.category = :category)
                    """,
            countQuery = """
                    select count(p)
                    from Post p
                    where (:regionPattern is null or p.regionCode like :regionPattern)
                      and p.publiclyVisible = true
                      and p.status = com.team2.postservice.post.entity.PostStatus.WAITING
                      and (:category is null or p.category = :category)
                    """
    )
    Page<NearbyRepairRequest> findNearby(
            @Param("regionPattern") String regionPattern,
            @Param("category") PostCategory category,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = "images")
    Optional<Post> findById(Long id);

    // 제안 채택 시 동시 요청을 막기 위한 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> lockById(@Param("id") Long id);
}