package com.team2.userservice.user.repository;

import com.team2.userservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);

    // 관리자 대시보드 개요용
    long countByCreatedAtAfter(LocalDateTime dateTime);
}