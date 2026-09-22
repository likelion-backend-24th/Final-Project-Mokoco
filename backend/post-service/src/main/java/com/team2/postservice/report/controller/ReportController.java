package com.team2.postservice.report.controller;

import com.team2.common.security.LoginUser;
import com.team2.postservice.report.dto.ReportCreateRequest;
import com.team2.postservice.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 로그인한 유저 누구나 신고를 접수할 수 있다.
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<Long> createReport(
            @RequestBody ReportCreateRequest request,
            @AuthenticationPrincipal LoginUser user
    ) {
        Long id = reportService.createReport(request, user.userId());
        return ResponseEntity.ok(id);
    }
}
