package com.team2.postservice.report.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    // POST 신고일 때만 값이 있음 (post-service가 직접 다루는 id)
    private Long targetId;

    // USER 신고이거나, POST 신고에서 작성자를 같이 참조해두고 싶을 때 사용 (관리자가 화면에서 바로 찾을 수 있도록)
    @Column(length = 100)
    private String targetEmail;

    @Column(nullable = false, length = 100)
    private String reporterEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Report(ReportTargetType targetType, Long targetId, String targetEmail,
            String reporterEmail, ReportReason reason, String detail) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetEmail = targetEmail;
        this.reporterEmail = reporterEmail;
        this.reason = reason;
        this.detail = detail;
        this.status = ReportStatus.PENDING;
    }

    public void resolve() {
        this.status = ReportStatus.RESOLVED;
    }

    public void dismiss() {
        this.status = ReportStatus.DISMISSED;
    }
}
