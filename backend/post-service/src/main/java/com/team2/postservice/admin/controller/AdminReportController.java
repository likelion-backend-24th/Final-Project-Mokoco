package com.team2.postservice.admin.controller;

import com.team2.postservice.report.dto.ReportResponseDto;
import com.team2.postservice.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<List<ReportResponseDto>> listReports(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ResponseEntity.ok(reportService.listReports(authorization));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ReportResponseDto> resolve(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ResponseEntity.ok(reportService.resolve(id, authorization));
    }

    @PatchMapping("/{id}/dismiss")
    public ResponseEntity<ReportResponseDto> dismiss(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ResponseEntity.ok(reportService.dismiss(id, authorization));
    }
}
