package com.team2.postservice.admin.controller;

import com.team2.postservice.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 관리자가 작성자와 무관하게 아무 글이나 삭제할 수 있는 경로.
// 인증은 X-User-Email이 아니라 Authorization: Bearer로 받아서, post-service가 매 요청마다
// user-service에 role을 다시 확인한다(관리자 권한 위조/우회 여지를 줄이기 위함).
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostService postService;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        postService.deletePostAsAdmin(id, authorization);
        return ResponseEntity.noContent().build();
    }
}
