package com.team2.postservice.report.dto;

import com.team2.postservice.report.entity.ReportReason;
import com.team2.postservice.report.entity.ReportTargetType;

public record ReportCreateRequest(
        ReportTargetType targetType,
        Long targetId,      // POST 신고 시 post id
        String targetEmail, // USER 신고 시(또는 POST 신고에서 작성자 참조용) 이메일
        ReportReason reason,
        String detail
) {
}
