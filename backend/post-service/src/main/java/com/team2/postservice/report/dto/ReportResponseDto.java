package com.team2.postservice.report.dto;

import com.team2.postservice.report.entity.Report;
import com.team2.postservice.report.entity.ReportReason;
import com.team2.postservice.report.entity.ReportStatus;
import com.team2.postservice.report.entity.ReportTargetType;

import java.time.format.DateTimeFormatter;

public record ReportResponseDto(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        String targetEmail,
        String reporterEmail,
        ReportReason reason,
        String detail,
        ReportStatus status,
        String createdAt
) {
    public static ReportResponseDto from(Report report) {
        return new ReportResponseDto(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getTargetEmail(),
                report.getReporterEmail(),
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getCreatedAt() != null ? report.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null
        );
    }
}
