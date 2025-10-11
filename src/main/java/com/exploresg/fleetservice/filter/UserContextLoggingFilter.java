package com.exploresg.fleetservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class UserContextLoggingFilter extends HttpFilter {

    public static final String MDC_USER_ID = "userId";
    public static final String MDC_USER_EMAIL = "userEmail";

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object principal = auth.getPrincipal();
                // Many resource servers use a JwtAuthenticationToken where principal is a Jwt
                try {
                    // Attempt to extract common fields if present
                    String name = auth.getName();
                    if (name != null)
                        MDC.put(MDC_USER_ID, name);
                } catch (Exception ignored) {
                }
            }

            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_USER_EMAIL);
        }
    }
}
