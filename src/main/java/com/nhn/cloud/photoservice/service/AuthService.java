package com.nhn.cloud.photoservice.service;

import com.nhn.cloud.photoservice.config.security.JwtTokenProvider;
import com.nhn.cloud.photoservice.domain.audit.AuditAction;
import com.nhn.cloud.photoservice.domain.user.User;
import com.nhn.cloud.photoservice.domain.user.UserRole;
import com.nhn.cloud.photoservice.dto.request.LoginRequest;
import com.nhn.cloud.photoservice.dto.request.SignupRequest;
import com.nhn.cloud.photoservice.dto.response.AuthResponse;
import com.nhn.cloud.photoservice.exception.CustomException;
import com.nhn.cloud.photoservice.exception.ErrorCode;
import com.nhn.cloud.photoservice.repository.UserRepository;
import com.nhn.cloud.photoservice.util.ClientIpUtil;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter signupCounter;

    /**
     * 회원가입
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .role(UserRole.USER)
                .build();

        User savedUser = userRepository.save(user);
        signupCounter.increment();
        log.info("New user registered: {}", savedUser.getEmail());

        auditLogService.log(savedUser.getId(), savedUser.getEmail(),
                AuditAction.SIGNUP, "user", savedUser.getId(),
                "New user registered", ClientIpUtil.getClientIp());

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        String refreshToken = jwtTokenProvider.createRefreshToken(savedUser.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName()
        );
    }

    /**
     * 로그인
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String clientIp = ClientIpUtil.getClientIp();

        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    loginFailureCounter.increment();
                    auditLogService.log(null, request.getEmail(),
                            AuditAction.LOGIN_FAILURE, "user", null,
                            "User not found", clientIp);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginFailureCounter.increment();
            auditLogService.log(user.getId(), user.getEmail(),
                    AuditAction.LOGIN_FAILURE, "user", user.getId(),
                    "Invalid password", clientIp);
            throw new CustomException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }

        // 계정 활성화 확인
        if (!user.getIsActive()) {
            loginFailureCounter.increment();
            auditLogService.log(user.getId(), user.getEmail(),
                    AuditAction.LOGIN_FAILURE, "user", user.getId(),
                    "Account deactivated", clientIp);
            throw new CustomException(ErrorCode.FORBIDDEN, "Account is deactivated");
        }

        loginSuccessCounter.increment();
        auditLogService.log(user.getId(), user.getEmail(),
                AuditAction.LOGIN_SUCCESS, "user", user.getId(),
                "Login successful", clientIp);
        log.info("User logged in: {}", user.getEmail());

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }
}