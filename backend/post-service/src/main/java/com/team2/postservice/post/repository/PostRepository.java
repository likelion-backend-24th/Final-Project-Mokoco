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

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(value = """
        select new com.team2.postservice.post.dto.NearbyRepairRequest(
            p.id, p.title, p.content, p.authorEmail, p.category, p.status, p.regionCode, p.regionName, p.createdAt,
            (select i.imageUrl from PostImage i where i.post = p
                and i.id = (select min(firstImage.id) from PostImage firstImage where firstImage.post = p)))
        from Post p
        where (:regionPattern is null or p.regionCode like :regionPattern) and p.publiclyVisible = true
          and (:category is null or p.category = :category)
        """, countQuery = """
        select count(p) from Post p
        where (:regionPattern is null or p.regionCode like :regionPattern) and p.publiclyVisible = true
          and (:category is null or p.category = :category)
        """)
    Page<NearbyRepairRequest> findNearby(
            @Param("regionPattern") String regionPattern,
            @Param("category") PostCategory category,
            Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "images")
    Optional<Post> findById(Long id);

}
