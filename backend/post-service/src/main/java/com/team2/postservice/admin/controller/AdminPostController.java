package com.team2.postservice.admin.controller;

import com.team2.common.security.LoginUser;
import com.team2.postservice.post.dto.AdminPostResponse;
import com.team2.postservice.post.entity.PostStatus;
import com.team2.postservice.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 관리자 글 관리 — 목록 조회(비공개 글 포함) + 작성자와 무관한 삭제.
// post-service가 매 요청마다 user-service에 role을 다시 확인한다(관리자 권한 위조/우회 여지를 줄이기 위함).
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private static final int MAX_PAGE_SIZE = 50;

    private final PostService postService;

    @GetMapping
    public ResponseEntity<Page<AdminPostResponse>> listPosts(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return ResponseEntity.ok(postService.listPostsForAdmin(user, keyword, status, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser user
    ) {
        postService.deletePostAsAdmin(id, user);
        return ResponseEntity.noContent().build();
    }
}
