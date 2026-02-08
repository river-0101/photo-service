package com.nhn.cloud.photoservice.service;

import com.nhn.cloud.photoservice.domain.audit.AuditAction;
import com.nhn.cloud.photoservice.domain.audit.AuditLog;
import com.nhn.cloud.photoservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 감사 로그를 비동기로 기록한다.
     * 별도 트랜잭션으로 실행되어, 메인 비즈니스 로직의 트랜잭션에 영향을 주지 않는다.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String userEmail, AuditAction action,
                    String targetType, Long targetId, String detail, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .detail(detail)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: action={}, userId={}, detail={}",
                    action, userId, detail, e);
        }
    }

    /**
     * IP가 없는 경우 (서비스 간 내부 호출 등)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String userEmail, AuditAction action,
                    String targetType, Long targetId, String detail) {
        log(userId, userEmail, action, targetType, targetId, detail, null);
    }
}
