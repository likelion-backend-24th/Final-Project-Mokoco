package com.team2.postservice.post.repository;

import com.team2.postservice.post.entity.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Override
    @EntityGraph(attributePaths = "images")
    List<Post> findAll();

    @Override
    @EntityGraph(attributePaths = "images")
    Optional<Post> findById(Long id);
}
