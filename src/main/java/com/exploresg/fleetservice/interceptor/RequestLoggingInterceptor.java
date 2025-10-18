package com.exploresg.fleetservice.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;

/**
 * HTTP Request Logging Interceptor
 * 
 * Logs HTTP request/response details including:
 * - HTTP method and path
 * - Response status code
 * - Request duration
 * - Client IP address
 * - User agent
 * 
 * Also adds request metadata to MDC for structured logging.
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    private static final String START_TIME = "startTime";
    private static final String MDC_REQUEST_METHOD = "requestMethod";
    private static final String MDC_REQUEST_PATH = "requestPath";
    private static final String MDC_CLIENT_IP = "clientIp";
    private static final long SLOW_REQUEST_THRESHOLD_MS = 2000; // 2 seconds

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        request.setAttribute(START_TIME, Instant.now());

        // Add request context to MDC for all logs during this request
        MDC.put(MDC_REQUEST_METHOD, request.getMethod());
        MDC.put(MDC_REQUEST_PATH, request.getRequestURI());
        MDC.put(MDC_CLIENT_IP, getClientIpAddress(request));

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler,
            @Nullable Exception ex) {
        try {
            Instant start = (Instant) request.getAttribute(START_TIME);
            if (start == null)
                return;

            long durationMs = Duration.between(start, Instant.now()).toMillis();
            String method = request.getMethod();
            String path = request.getRequestURI();
            int status = response.getStatus();
            String clientIp = getClientIpAddress(request);

            // Log successful requests at INFO level
            if (status < 400) {
                log.info("HTTP {} {} completed with status {} in {}ms from {}",
                        method, path, status, durationMs, clientIp);
            }
            // Log client errors (4xx) at WARN level
            else if (status < 500) {
                log.warn("HTTP {} {} returned client error {} in {}ms from {}",
                        method, path, status, durationMs, clientIp);
            }
            // Log server errors (5xx) at ERROR level
            else {
                log.error("HTTP {} {} returned server error {} in {}ms from {}",
                        method, path, status, durationMs, clientIp);
            }

            // Warn if request is slow
            if (durationMs > SLOW_REQUEST_THRESHOLD_MS) {
                log.warn("Slow request detected: {} {} took {}ms (threshold: {}ms)",
                        method, path, durationMs, SLOW_REQUEST_THRESHOLD_MS);
            }

            // Log exception if present
            if (ex != null) {
                log.error("Exception occurred during request {} {}: {}",
                        method, path, ex.getMessage(), ex);
            }
        } finally {
            // Clean up MDC
            MDC.remove(MDC_REQUEST_METHOD);
            MDC.remove(MDC_REQUEST_PATH);
            MDC.remove(MDC_CLIENT_IP);
        }
    }

    /**
     * Extract client IP address from request, checking common proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}
