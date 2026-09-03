package com.team2.postservice.post.service;

import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.post.dto.PostRequestDto;
import com.team2.postservice.post.dto.PostResponseDto;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    @Transactional
    public Long createPost(PostRequestDto.Create request, String authorEmail) {
        Post post = Post.builder()
                .title(request.title())
                .content(request.content())
                .authorEmail(authorEmail)
                .build();
        return postRepository.save(post).getId();
    }

    public List<PostResponseDto.Detail> getAllPosts() {
        return postRepository.findAll().stream()
                .map(PostResponseDto.Detail::from)
                .toList();
    }

    public PostResponseDto.Detail getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        return PostResponseDto.Detail.from(post);
    }

    @Transactional
    public void updatePost(Long id, PostRequestDto.Update request, String userEmail) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getAuthorEmail().equals(userEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_POST_UPDATE);
        }

        post.update(request.title(), request.content());
    }

    @Transactional
    public void deletePost(Long id, String userEmail) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getAuthorEmail().equals(userEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_POST_DELETE);
        }

        postRepository.delete(post);
    }
}