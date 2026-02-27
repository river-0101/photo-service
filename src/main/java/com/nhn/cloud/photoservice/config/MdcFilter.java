package com.nhn.cloud.photoservice.config;

import com.nhn.cloud.photoservice.util.ClientIpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = request.getHeader("X-Request-ID");
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            }
            MDC.put("requestId", requestId);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = "-";
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                userId = auth.getName();
            }
            MDC.put("userId", userId);
            MDC.put("UserID", userId);

            MDC.put("method", request.getMethod());
            MDC.put("uri", request.getRequestURI());

            String clientIp = ClientIpUtil.getClientIp();
            MDC.put("clientIp", clientIp);
            MDC.put("ClientIp", clientIp);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
