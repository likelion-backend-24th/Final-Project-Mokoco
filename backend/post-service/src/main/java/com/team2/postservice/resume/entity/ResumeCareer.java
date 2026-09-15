package com.team2.postservice.resume.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "resume_careers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeCareer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(nullable = false, length = 50)
    private String period;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false)
    private Integer sortOrder;

    @Builder
    public ResumeCareer(Resume resume, String period, String description, Integer sortOrder) {
        this.resume = resume;
        this.period = period;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public record CareerInput(String period, String description) {}
}
