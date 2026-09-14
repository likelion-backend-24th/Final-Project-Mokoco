package com.team2.postservice.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.postservice.common.exception.AiException;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiDraftCacheTest {
    final Clock clock = mock(Clock.class);
    final AiDraftCache cache = new AiDraftCache(clock);
    final AtomicInteger calls = new AtomicInteger();
    final Runnable quota = mock(Runnable.class);
    com.fasterxml.jackson.databind.JsonNode generate() {
        calls.incrementAndGet();
        return new ObjectMapper().createObjectNode().put("value", "original");
    }
    @Test void reusesResultWithoutQuotaAndReturnsIndependentCopiesUntilExpiry() {
        var first = cache.get("1:post:a", quota, this::generate);
        ((com.fasterxml.jackson.databind.node.ObjectNode) first).put("requestId", "old");
        assertThat(cache.get("1:post:a", quota, this::generate).has("requestId")).isFalse();
        assertThat(calls.get()).isEqualTo(1); verify(quota).run();
        cache.get("2:post:a", quota, this::generate);
        cache.get("1:post:b", quota, this::generate);
        when(clock.millis()).thenReturn(600_001L);
        cache.get("1:post:a", quota, this::generate);
        assertThat(calls.get()).isEqualTo(4);
    }
    @Test void failedGenerationAllowsOnlyOneRetryAfterCooldown() {
        java.util.function.Supplier<com.fasterxml.jackson.databind.JsonNode> failure = () -> { calls.incrementAndGet(); throw AiException.output(); };
        assertThatThrownBy(() -> cache.get("key", quota, failure)).isInstanceOf(AiException.class);
        assertThatThrownBy(() -> cache.get("key", quota, failure)).hasMessageContaining("30초");
        assertThat(calls.get()).isEqualTo(1);
        when(clock.millis()).thenReturn(30_000L);
        assertThatThrownBy(() -> cache.get("key", quota, failure)).isInstanceOf(AiException.class);
        when(clock.millis()).thenReturn(60_000L);
        assertThatThrownBy(() -> cache.get("key", quota, failure)).hasMessageContaining("재시도 한도");
        assertThat(calls.get()).isEqualTo(2);
        when(clock.millis()).thenReturn(600_001L);
        cache.get("key", quota, this::generate);
        assertThat(calls.get()).isEqualTo(3);
    }
    @Test void concurrentDuplicateDoesNotCallProviderTwice() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var running = executor.submit(() -> cache.get("key", quota, () -> {
                entered.countDown();
                try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                return generate();
            }));
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> cache.get("key", quota, this::generate)).hasMessageContaining("처리 중");
            } finally { release.countDown(); }
            running.get(5, TimeUnit.SECONDS);
            assertThat(calls.get()).isEqualTo(1); verify(quota).run();
        }
    }
}
