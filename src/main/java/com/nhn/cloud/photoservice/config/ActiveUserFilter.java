package com.nhn.cloud.photoservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 인증된 요청의 동시 처리 수를 추적하는 필터.
 * 요청 시작 시 activeUsers를 증가시키고, 요청 완료 시 감소시킨다.
 */
@Component
@RequiredArgsConstructor
public class ActiveUserFilter extends OncePerRequestFilter {

    private final AtomicInteger activeUsers;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());

        if (isAuthenticated) {
            activeUsers.incrementAndGet();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (isAuthenticated) {
                activeUsers.decrementAndGet();
            }
        }
    }
}
