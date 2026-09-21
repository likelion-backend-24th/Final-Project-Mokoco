package com.team2.postservice.admin.controller;

import com.team2.common.security.LoginUser;
import com.team2.postservice.report.dto.ReportResponseDto;
import com.team2.postservice.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<List<ReportResponseDto>> listReports(
            @AuthenticationPrincipal LoginUser user
    ) {
        return ResponseEntity.ok(reportService.listReports(user));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ReportResponseDto> resolve(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser user
    ) {
        return ResponseEntity.ok(reportService.resolve(id, user));
    }

    @PatchMapping("/{id}/dismiss")
    public ResponseEntity<ReportResponseDto> dismiss(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser user
    ) {
        return ResponseEntity.ok(reportService.dismiss(id, user));
    }
}
