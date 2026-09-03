package com.team2.postservice.post.controller;

import com.team2.postservice.post.dto.PostRequestDto;
import com.team2.postservice.post.dto.PostResponseDto;
import com.team2.postservice.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createPost(@RequestPart("request") PostRequestDto.Create request,
                                           @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                           @RequestHeader("X-User-Email") String userEmail) {
        Long postId = postService.createPost(request, images, userEmail);
        return ResponseEntity.ok(postId);
    }

    @GetMapping
    public ResponseEntity<List<PostResponseDto.Detail>> getPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto.Detail> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable Long id,
                                           @RequestBody PostRequestDto.Update request,
                                           @RequestHeader("X-User-Email") String userEmail) {
        postService.updatePost(id, request, userEmail);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id,
                                           @RequestHeader("X-User-Email") String userEmail) {
        postService.deletePost(id, userEmail);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponseDto.Detail> addImages(@PathVariable Long id,
                                                             @RequestPart("images") List<MultipartFile> images,
                                                             @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(postService.addImages(id, images, userEmail));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id,
                                            @PathVariable Long imageId,
                                            @RequestHeader("X-User-Email") String userEmail) {
        postService.deleteImage(id, imageId, userEmail);
        return ResponseEntity.ok().build();
    }
}
