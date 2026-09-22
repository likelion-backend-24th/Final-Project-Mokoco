package com.team2.postservice.post.controller;

import com.team2.common.security.LoginUser;
import com.team2.postservice.post.dto.PostRequestDto;
import com.team2.postservice.post.dto.NearbyRepairRequest;
import jakarta.validation.constraints.NotNull;
import com.team2.postservice.post.dto.PostResponseDto;
import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.post.entity.RegionScope;
import com.team2.postservice.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createPost(@RequestPart("post") @Valid PostRequestDto.Create request,
                                           @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                           @AuthenticationPrincipal LoginUser user) {
        Long postId = postService.createPost(request, images, user);
        return ResponseEntity.ok(postId);
    }

    @GetMapping
    public ResponseEntity<NearbyRepairRequest.Result> getPosts(
            @RequestParam(required = false) PostCategory category,
            @RequestParam(defaultValue = "ALL") RegionScope regionScope,
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getNearbyPosts(user == null ? null : user.id(), category, page, size, regionScope));
    }

    public record VisibilityRequest(@NotNull Boolean publiclyVisible) {}

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Void> changeVisibility(@PathVariable Long id,
            @RequestBody @Valid VisibilityRequest request,
            @AuthenticationPrincipal LoginUser user) {
        postService.changeVisibility(id, request.publiclyVisible(), user.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto.Detail> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable Long id,
                                           @RequestBody @Valid PostRequestDto.Update request,
                                           @AuthenticationPrincipal LoginUser user) {
        postService.updatePost(id, request, user.id());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id,
                                           @AuthenticationPrincipal LoginUser user) {
        postService.deletePost(id, user.id());
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponseDto.Detail> addImages(@PathVariable Long id,
                                                            @RequestPart("images") List<MultipartFile> images,
                                                            @AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.ok(postService.addImages(id, images, user.id()));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id,
                                            @PathVariable Long imageId,
                                            @AuthenticationPrincipal LoginUser user) {
        postService.deleteImage(id, imageId, user.id());
        return ResponseEntity.ok().build();
    }
}
