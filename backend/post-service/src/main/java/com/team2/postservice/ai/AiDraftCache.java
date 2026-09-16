package com.team2.postservice.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.team2.postservice.common.exception.AiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Process-local, bounded cache. Keys are user-scoped hashes, never raw photos or conversations. */
@Component
public class AiDraftCache {

    private static final long TTL = 600_000, RETRY_DELAY = 30_000;
    private final Map<String, Entry> entries = new HashMap<>();
    private final Clock clock;

    public AiDraftCache() {
        this(Clock.systemUTC());
    }

    AiDraftCache(Clock clock) {
        this.clock = clock;
    }

    private static class Entry {
        long expires, retryAt;
        int attempts;
        boolean running;
        JsonNode result;
        Entry(long expires) { this.expires = expires; }
    }

    public JsonNode get(String key, Runnable acquireQuota, Supplier<JsonNode> generate) {
        Entry entry;

        synchronized (this) {
            long now = clock.millis();

            entries.values().removeIf(e -> !e.running && e.expires <= now);
            entry = entries.get(key);

            if (entry == null) {
                if (entries.size() >= 500) throw limited("AI_BUSY", "AI 요청이 많습니다. 잠시 후 다시 시도해주세요.");
                entry = new Entry(now + TTL);
                entries.put(key, entry);
            }

            if (entry.result != null) {
                return entry.result.deepCopy();
            }
            if (entry.running) {
                throw limited("AI_IN_PROGRESS", "동일한 입력을 처리 중입니다. 잠시 기다려주세요.");
            }
            if (entry.attempts >= 2) {
                throw limited("AI_RETRY_LIMIT", "같은 입력의 재시도 한도에 도달했습니다. 10분 후 다시 시도하거나 직접 작성해주세요.");
            }
            if (entry.retryAt > now) {
                throw limited("AI_RETRY_WAIT", "실패한 요청은 30초 후 한 번만 다시 시도할 수 있습니다.");
            }

            acquireQuota.run();
            entry.attempts++;
            entry.running = true;
        }

        try {
            JsonNode result = generate.get();
            synchronized (this) {
                entry.result = result.deepCopy(); entry.expires = clock.millis() + TTL;
            }
            return result.deepCopy();
        } finally {
            synchronized (this) {
                entry.running = false; entry.retryAt = clock.millis() + RETRY_DELAY;
            }
        }
    }

    private AiException limited(String code, String message) {
        return new AiException(HttpStatus.TOO_MANY_REQUESTS, code, message);
    }
}
