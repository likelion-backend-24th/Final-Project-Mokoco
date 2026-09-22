package com.team2.postservice.resume.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resumes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String headline;

    @Column(length = 2000)
    private String introduction;

    @ElementCollection
    @CollectionTable(name = "resume_skills", joinColumns = @JoinColumn(name = "resume_id"))
    @Column(name = "skill_name", length = 30)
    @OrderColumn(name = "sort_order")
    private List<String> skills = new ArrayList<>();

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ResumeCareer> careers = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Resume(Long userId, String headline, String introduction, List<String> skills) {
        this.userId = userId;
        this.headline = headline;
        this.introduction = introduction;
        this.skills = skills != null ? new ArrayList<>(skills) : new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateContent(String headline, String introduction, List<String> skills,
                               List<ResumeCareer.CareerInput> careerInputs) {
        this.headline = headline;
        this.introduction = introduction;
        this.updatedAt = LocalDateTime.now();

        this.skills.clear();
        if (skills != null) this.skills.addAll(skills);

        this.careers.clear();
        if (careerInputs != null) {
            int order = 0;
            for (ResumeCareer.CareerInput input : careerInputs) {
                this.careers.add(ResumeCareer.builder()
                        .resume(this)
                        .period(input.period())
                        .description(input.description())
                        .sortOrder(order++)
                        .build());
            }
        }
    }
}
