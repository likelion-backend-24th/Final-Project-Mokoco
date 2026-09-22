package com.team2.postservice.resume.repository;

import com.team2.postservice.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    Optional<Resume> findByUserEmail(String userEmail);
    boolean existsByUserEmail(String userEmail);
}
