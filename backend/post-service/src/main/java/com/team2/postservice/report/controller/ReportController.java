package com.team2.postservice.report.controller;

import com.team2.postservice.report.dto.ReportCreateRequest;
import com.team2.postservice.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 로그인한 유저 누구나 신고를 접수할 수 있다 (BFF가 쿠키 검증 후 X-User-Email 주입).
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<Long> createReport(
            @RequestBody ReportCreateRequest request,
            @RequestHeader("X-User-Email") String reporterEmail
    ) {
        Long id = reportService.createReport(request, reporterEmail);
        return ResponseEntity.ok(id);
    }
}
