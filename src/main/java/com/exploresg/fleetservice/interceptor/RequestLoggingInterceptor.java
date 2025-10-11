package com.exploresg.fleetservice.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, Instant.now());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            @Nullable Exception ex) {
        Instant start = (Instant) request.getAttribute(START_TIME);
        if (start == null)
            return;
        long durationMs = Duration.between(start, Instant.now()).toMillis();

        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();

        String correlationId = MDC.get("correlationId");

        log.info("{} {} completed with status {} in {}ms", method, path, status, durationMs);

        // Warn if slow
        try {
            String threshold = System.getProperty("exploresg.logging.slow-request-threshold");
            if (threshold != null) {
                long t = Long.parseLong(threshold);
                if (durationMs > t) {
                    log.warn("Slow request: {} {} took {}ms (threshold {}ms)", method, path, durationMs, t);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
