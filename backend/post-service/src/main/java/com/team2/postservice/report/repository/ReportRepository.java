package com.team2.postservice.report.repository;

import com.team2.postservice.report.entity.Report;
import com.team2.postservice.report.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findAllByOrderByCreatedAtDesc();

    // 관리자 대시보드 개요용
    long countByStatus(ReportStatus status);
}
