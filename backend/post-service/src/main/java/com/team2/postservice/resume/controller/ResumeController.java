package com.team2.postservice.resume.controller;

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
                                              @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(resumeService.createResume(request, userEmail));
    }

    @PatchMapping
    public ResponseEntity<Void> updateResume(@RequestBody ResumeRequestDto.Upsert request,
                                              @RequestHeader("X-User-Email") String userEmail) {
        resumeService.updateResume(request, userEmail);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResume(@RequestHeader("X-User-Email") String userEmail) {
        resumeService.deleteResume(userEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ResumeResponseDto> getMyResume(@RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(resumeService.getMyResume(userEmail));
    }

    @GetMapping("/{email}")
    public ResponseEntity<ResumeResponseDto> getResumeByEmail(@PathVariable String email) {
        return ResponseEntity.ok(resumeService.getResumeByEmail(email));
    }
}
