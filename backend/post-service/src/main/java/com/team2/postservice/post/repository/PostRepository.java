package com.team2.postservice.post.repository;

import com.team2.postservice.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}