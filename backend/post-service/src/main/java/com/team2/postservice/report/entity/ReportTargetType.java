package com.team2.postservice.report.entity;

public enum ReportTargetType {
    POST, // targetId = post id
    USER  // targetEmail = 신고 대상 유저 이메일 (post-service는 유저 id를 직접 다루지 않음)
}
