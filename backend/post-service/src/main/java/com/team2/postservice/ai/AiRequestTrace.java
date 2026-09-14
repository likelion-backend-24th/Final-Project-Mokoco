package com.team2.postservice.ai;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
public class AiRequestTrace extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AiRequestTrace.class);
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals("/api/ai/post-draft") && !path.matches("/api/chat-rooms/\\d+/contract/ai-draft");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String id = UUID.randomUUID().toString(); long started = System.nanoTime();
        MDC.put("aiRequestId",id); response.setHeader("X-Request-Id",id);
        try { chain.doFilter(request,response); }
        finally {
            log.info("AI request={} feature={} status={} durationMs={}", id,
                    request.getRequestURI().equals("/api/ai/post-draft") ? "post" : "contract", response.getStatus(), (System.nanoTime()-started)/1_000_000);
            MDC.remove("aiRequestId");
        }
    }
    public static String requestId() { return MDC.get("aiRequestId") == null ? UUID.randomUUID().toString() : MDC.get("aiRequestId"); }
}
