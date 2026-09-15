package com.team2.postservice.ai;

import com.team2.postservice.common.exception.AiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

/** Single-instance guard. Deploy shared quotas before scaling post-service horizontally. */
@Component
public class AiRateLimit {
    private final int minuteLimit, dayLimit, globalLimit;
    private final Map<Long, ArrayDeque<Instant>> calls = new HashMap<>();
    private LocalDate day = LocalDate.now(ZoneOffset.UTC);
    private int total;
    public AiRateLimit(@Value("${ai.limit.per-minute:3}") int minuteLimit,
                      @Value("${ai.limit.per-day:20}") int dayLimit,
                      @Value("${ai.limit.global-per-day:500}") int globalLimit) {
        this.minuteLimit = minuteLimit; this.dayLimit = dayLimit; this.globalLimit = globalLimit;
    }
    public synchronized void acquire(Long userId) {
        Instant now = Instant.now();
        LocalDate today = now.atOffset(ZoneOffset.UTC).toLocalDate();
        if (!day.equals(today)) { calls.clear(); total = 0; day = today; }
        if (total >= globalLimit) throw limited();
        var history = calls.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        if (history.size() >= dayLimit || history.stream().filter(t -> t.isAfter(now.minusSeconds(60))).count() >= minuteLimit) throw limited();
        history.add(now); total++;
    }
    private AiException limited() { return new AiException(HttpStatus.TOO_MANY_REQUESTS, "AI_LIMIT", "AI 작성 도움의 사용 한도에 도달했습니다. 직접 작성하거나 나중에 다시 시도해주세요."); }
}
