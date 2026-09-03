package com.team2.userservice.user.repository;

import com.team2.userservice.user.entity.SocialAccount;
import com.team2.userservice.user.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderId(SocialProvider provider, String providerId);
}