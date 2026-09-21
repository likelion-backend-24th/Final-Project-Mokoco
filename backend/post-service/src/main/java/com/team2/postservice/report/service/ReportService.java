package com.team2.postservice.report.service;

import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
import com.team2.common.security.LoginUser;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.post.service.PostViewerService;
import com.team2.postservice.report.dto.ReportCreateRequest;
import com.team2.postservice.report.dto.ReportResponseDto;
import com.team2.postservice.report.entity.Report;
import com.team2.postservice.report.entity.ReportTargetType;
import com.team2.postservice.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final PostViewerService postViewerService;

    @Transactional
    public Long createReport(ReportCreateRequest request, String reporterEmail) {
        if (request.targetType() == null || request.reason() == null)
            throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);

        if (request.targetType() == ReportTargetType.POST) {
            if (request.targetId() == null) throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
            postRepository.findById(request.targetId())
                    .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        } else if (request.targetType() == ReportTargetType.USER) {
            if (request.targetEmail() == null || request.targetEmail().isBlank())
                throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
        }

        Report report = Report.builder()
                .targetType(request.targetType())
                .targetId(request.targetId())
                .targetEmail(request.targetEmail())
                .reporterEmail(reporterEmail)
                .reason(request.reason())
                .detail(request.detail())
                .build();

        return reportRepository.save(report).getId();
    }

    @Transactional(readOnly = true)
    public List<ReportResponseDto> listReports(LoginUser admin) {
        postViewerService.requireAdmin(admin);
        return reportRepository.findAllByOrderByCreatedAtDesc().stream().map(ReportResponseDto::from).toList();
    }

    @Transactional
    public ReportResponseDto resolve(Long id, LoginUser admin) {
        postViewerService.requireAdmin(admin);
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        report.resolve();
        return ReportResponseDto.from(report);
    }

    @Transactional
    public ReportResponseDto dismiss(Long id, LoginUser admin) {
        postViewerService.requireAdmin(admin);
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        report.dismiss();
        return ReportResponseDto.from(report);
    }
}
