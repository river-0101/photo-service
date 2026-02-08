package com.nhn.cloud.photoservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MetricsConfig {

    /**
     * 동시 활성 사용자 수 (Gauge)
     * - 현재 인증된 요청을 처리 중인 수
     * - 요청 시작 시 증가, 종료 시 감소
     */
    @Bean
    public AtomicInteger activeUsers() {
        return new AtomicInteger(0);
    }

    @Bean
    public Gauge activeUsersGauge(MeterRegistry registry, AtomicInteger activeUsers) {
        return Gauge.builder("photo_service.active_users", activeUsers, AtomicInteger::get)
                .description("Number of currently active authenticated users")
                .register(registry);
    }

    // --- Auth Metrics ---

    @Bean
    public Counter loginSuccessCounter(MeterRegistry registry) {
        return Counter.builder("photo_service.auth.login")
                .tag("result", "success")
                .description("Number of successful logins")
                .register(registry);
    }

    @Bean
    public Counter loginFailureCounter(MeterRegistry registry) {
        return Counter.builder("photo_service.auth.login")
                .tag("result", "failure")
                .description("Number of failed logins")
                .register(registry);
    }

    @Bean
    public Counter signupCounter(MeterRegistry registry) {
        return Counter.builder("photo_service.auth.signup")
                .description("Number of user signups")
                .register(registry);
    }

    // --- Photo Metrics ---

    @Bean
    public Counter photoUploadSuccessCounter(MeterRegistry registry) {
        return Counter.builder("photo_service.photo.upload")
                .tag("result", "success")
                .description("Number of successful photo uploads")
                .register(registry);
    }

    @Bean
    public Counter photoUploadFailureCounter(MeterRegistry registry) {
        return Counter.builder("photo_service.photo.upload")
                .tag("result", "failure")
                .description("Number of failed photo uploads")
                .register(registry);
    }

    @Bean
    public Timer photoUploadTimer(MeterRegistry registry) {
        return Timer.builder("photo_service.photo.upload.duration")
                .description("Time taken to upload a photo")
                .register(registry);
    }

    // --- Object Storage Metrics ---

    @Bean
    public Timer objectStorageUploadTimer(MeterRegistry registry) {
        return Timer.builder("photo_service.storage.upload.duration")
                .description("Time taken to upload file to Object Storage")
                .register(registry);
    }

    @Bean
    public Timer presignedUrlTimer(MeterRegistry registry) {
        return Timer.builder("photo_service.storage.presigned_url.duration")
                .description("Time taken to generate presigned URL")
                .register(registry);
    }

    // --- Album Metrics ---

    @Bean
    public Counter albumShareSuccessCounter(MeterRegistry registry) {
        return Counter.builder("photo_service.album.share")
                .tag("result", "success")
                .description("Number of successful album share link generations")
                .register(registry);
    }

    @Bean
    public Counter albumShareFailureCounter(MeterRegistry registry) {
        return Counter.builder("photo_service.album.share")
                .tag("result", "failure")
                .description("Number of failed album share link generations")
                .register(registry);
    }

    @Bean
    public Timer albumListTimer(MeterRegistry registry) {
        return Timer.builder("photo_service.album.list.duration")
                .description("Time taken to list albums")
                .register(registry);
    }
}
