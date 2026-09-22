package com.team2.postservice.report.entity;

public enum ReportStatus {
    PENDING,   // 접수됨, 처리 대기
    RESOLVED,  // 관리자가 조치함 (글 삭제, 유저 정지 등 — 조치 자체는 별도 관리자 기능으로 수행)
    DISMISSED  // 검토 결과 조치 불필요
}
