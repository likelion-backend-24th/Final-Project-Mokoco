package com.team2.userservice;

import com.team2.userservice.config.JwtTokenProvider;
import com.team2.userservice.region.entity.Region;
import com.team2.userservice.region.repository.RegionRepository;
import com.team2.userservice.user.dto.UserSignUpRequest;
import com.team2.userservice.user.repository.UserRepository;
import com.team2.userservice.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Import(UserService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserServiceDatabaseTest extends FlywaySchemaTest {
    @Autowired UserService service;
    @Autowired UserRepository users;
    @Autowired RegionRepository regions;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean PasswordEncoder passwords;
    @MockitoBean JwtTokenProvider tokens;
    String regionCode;

    @BeforeEach void seedRegion() {
        regionCode = "1168010100";
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            users.deleteAll();
            regions.deleteAll();
            regions.save(Region.builder().regionCode(regionCode).sido("서울특별시")
                    .sigungu("강남구").dong("역삼동").build());
        });
    }

    @Test void concurrentSignupCreatesOneUserAndKeepsRegionRelation() throws Exception {
        CyclicBarrier encoded = new CyclicBarrier(2);
        when(passwords.encode(anyString())).thenAnswer(call -> {
            encoded.await(10, TimeUnit.SECONDS);
            return "encoded-password";
        });
        UserSignUpRequest request = signup("same@example.com", "same-nickname");

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> attempts = List.of(
                    pool.submit(() -> signup(request)),
                    pool.submit(() -> signup(request)));
            assertThat(attempts.stream().map(this::result).toList())
                    .containsExactlyInAnyOrder(true, false);
        }

        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findByEmail("same@example.com").orElseThrow().getRegionCode()).isEqualTo(regionCode);
    }

    private boolean signup(UserSignUpRequest request) {
        try {
            service.signUp(request);
            return true;
        } catch (DataIntegrityViolationException rejected) {
            return false;
        }
    }

    private boolean result(Future<Boolean> result) {
        try {
            return result.get(20, TimeUnit.SECONDS);
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }

    private UserSignUpRequest signup(String email, String nickname) {
        UserSignUpRequest request = new UserSignUpRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", "password1");
        ReflectionTestUtils.setField(request, "name", "테스터");
        ReflectionTestUtils.setField(request, "nickname", nickname);
        ReflectionTestUtils.setField(request, "regionCode", regionCode);
        return request;
    }
}
