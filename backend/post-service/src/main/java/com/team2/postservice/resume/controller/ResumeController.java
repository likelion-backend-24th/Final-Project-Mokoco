package com.team2.postservice.resume.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.team2.common.security.LoginUser;
import com.team2.postservice.resume.dto.ResumeRequestDto;
import com.team2.postservice.resume.dto.ResumeResponseDto;
import com.team2.postservice.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    public ResponseEntity<Long> createResume(@RequestBody ResumeRequestDto.Upsert request,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(resumeService.createResume(request, loginUser.userId()));
    }

    @PatchMapping
    public ResponseEntity<Void> updateResume(@RequestBody ResumeRequestDto.Upsert request,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        resumeService.updateResume(request, loginUser.userId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResume(@AuthenticationPrincipal LoginUser loginUser) {
        resumeService.deleteResume(loginUser.userId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ResumeResponseDto> getMyResume(@AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(resumeService.getMyResume(loginUser.userId()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ResumeResponseDto> getResumeByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(resumeService.getResumeByUserId(userId));
    }
}
