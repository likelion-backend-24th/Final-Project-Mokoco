package com.team2.postservice.resume.dto;

import com.team2.postservice.resume.entity.Resume;
import com.team2.postservice.resume.entity.ResumeCareer;

import java.time.LocalDateTime;
import java.util.List;

public record ResumeResponseDto(
        Long id,
        String userEmail,
        String headline,
        String introduction,
        List<String> skills,
        List<CareerItem> careers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record CareerItem(String period, String description) {
        public static CareerItem from(ResumeCareer career) {
            return new CareerItem(career.getPeriod(), career.getDescription());
        }
    }

    public static ResumeResponseDto from(Resume resume) {
        return new ResumeResponseDto(
                resume.getId(),
                resume.getUserEmail(),
                resume.getHeadline(),
                resume.getIntroduction(),
                resume.getSkills(),
                resume.getCareers().stream().map(CareerItem::from).toList(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }
}
