package com.team2.postservice.notification.repository;

import com.team2.postservice.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByUserEmail(String userEmail);
    Optional<NotificationSetting> findByUserId(Long userId);

}
