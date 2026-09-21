package com.team2.postservice.resume.controller;

import com.team2.common.security.LoginUser;
import com.team2.postservice.resume.dto.ResumeRequestDto;
import com.team2.postservice.resume.dto.ResumeResponseDto;
import com.team2.postservice.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    public ResponseEntity<Long> createResume(@RequestBody ResumeRequestDto.Upsert request,
                                              @AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.ok(resumeService.createResume(request, user.email()));
    }

    @PatchMapping
    public ResponseEntity<Void> updateResume(@RequestBody ResumeRequestDto.Upsert request,
                                              @AuthenticationPrincipal LoginUser user) {
        resumeService.updateResume(request, user.email());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResume(@AuthenticationPrincipal LoginUser user) {
        resumeService.deleteResume(user.email());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ResumeResponseDto> getMyResume(@AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.ok(resumeService.getMyResume(user.email()));
    }

    @GetMapping("/{email}")
    public ResponseEntity<ResumeResponseDto> getResumeByEmail(@PathVariable String email) {
        return ResponseEntity.ok(resumeService.getResumeByEmail(email));
    }
}
