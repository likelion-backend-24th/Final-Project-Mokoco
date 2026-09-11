package com.team2.postservice.post.controller;

import com.team2.postservice.post.dto.PostRequestDto;
import com.team2.postservice.post.dto.NearbyRepairRequest;
import com.team2.postservice.post.service.PostViewerService;
import jakarta.validation.constraints.NotNull;
import com.team2.postservice.post.dto.PostResponseDto;
import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.post.entity.RegionScope;
import com.team2.postservice.post.service.PostService;
import jakarta.validation.Valid;
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
    private final PostViewerService postViewerService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createPost(@RequestPart("post") @Valid PostRequestDto.Create request,
                                           @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long postId = postService.createPost(request, images, postViewerService.requireEmail(authorization));
        return ResponseEntity.ok(postId);
    }

    @GetMapping
    public ResponseEntity<NearbyRepairRequest.Result> getPosts(
            @RequestParam(required = false) PostCategory category,
            @RequestParam(defaultValue = "ALL") RegionScope regionScope,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getNearbyPosts(authorization, category, page, size, regionScope));
    }

    public record VisibilityRequest(@NotNull Boolean publiclyVisible) {}

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Void> changeVisibility(@PathVariable Long id,
            @RequestBody @Valid VisibilityRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        postService.changeVisibility(id, request.publiclyVisible(), authorization);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto.Detail> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable Long id,
                                           @RequestBody @Valid PostRequestDto.Update request,
                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        postService.updatePost(id, request, postViewerService.requireEmail(authorization));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id,
                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        postService.deletePost(id, postViewerService.requireEmail(authorization));
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponseDto.Detail> addImages(@PathVariable Long id,
                                                            @RequestPart("images") List<MultipartFile> images,
                                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(postService.addImages(id, images, postViewerService.requireEmail(authorization)));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id,
                                            @PathVariable Long imageId,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        postService.deleteImage(id, imageId, postViewerService.requireEmail(authorization));
        return ResponseEntity.ok().build();
    }
}
