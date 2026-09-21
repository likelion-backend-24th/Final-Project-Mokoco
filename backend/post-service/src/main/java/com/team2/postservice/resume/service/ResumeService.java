package com.team2.postservice.resume.service;

import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
import com.team2.postservice.resume.dto.ResumeRequestDto;
import com.team2.postservice.resume.dto.ResumeResponseDto;
import com.team2.postservice.resume.entity.Resume;
import com.team2.postservice.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

    private static final int MAX_SKILLS = 10;
    private static final int MAX_CAREERS = 10;

    private final ResumeRepository resumeRepository;

    @Transactional
    public Long createResume(ResumeRequestDto.Upsert request, String userEmail) {
        if (resumeRepository.existsByUserEmail(userEmail)) {
            throw new CustomException(ErrorCode.DUPLICATE_RESUME);
        }
        validate(request);

        Resume resume = Resume.builder()
                .userEmail(userEmail)
                .build();
        resume.updateContent(request.headline(), request.introduction(), request.skills(), request.careers());

        resumeRepository.save(resume);
        return resume.getId();
    }

    @Transactional
    public void updateResume(ResumeRequestDto.Upsert request, String userEmail) {
        validate(request);
        Resume resume = resumeRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.RESUME_NOT_FOUND));
        resume.updateContent(request.headline(), request.introduction(), request.skills(), request.careers());
    }

    @Transactional
    public void deleteResume(String userEmail) {
        Resume resume = resumeRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.RESUME_NOT_FOUND));
        resumeRepository.delete(resume);
    }

    public ResumeResponseDto getMyResume(String userEmail) {
        Resume resume = resumeRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.RESUME_NOT_FOUND));
        return ResumeResponseDto.from(resume);
    }

    public ResumeResponseDto getResumeByEmail(String userEmail) {
        Resume resume = resumeRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.RESUME_NOT_FOUND));
        return ResumeResponseDto.from(resume);
    }

    private void validate(ResumeRequestDto.Upsert request) {
        if (request.headline() == null || request.headline().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (request.skills() != null && request.skills().size() > MAX_SKILLS) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (request.careers() != null && request.careers().size() > MAX_CAREERS) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
