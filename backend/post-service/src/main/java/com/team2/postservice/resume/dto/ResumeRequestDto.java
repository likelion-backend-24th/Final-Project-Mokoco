package com.team2.postservice.resume.dto;

import com.team2.postservice.resume.entity.ResumeCareer;

import java.util.List;

public class ResumeRequestDto {
    public record Upsert(
            String headline,
            String introduction,
            List<String> skills,
            List<ResumeCareer.CareerInput> careers
    ) {}
}
