package com.nhn.cloud.photoservice.service;

import com.nhn.cloud.photoservice.exception.CustomException;
import com.nhn.cloud.photoservice.exception.ErrorCode;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final Timer objectStorageUploadTimer;
    private final Timer presignedUrlTimer;

    @Value("${cloud.nhn.object-storage.bucket-name}")
    private String bucketName;

    @Value("${cloud.nhn.object-storage.presigned-url-expiration}")
    private Long presignedUrlExpiration;

    /**
     * 파일 업로드
     */
    public String uploadFile(MultipartFile file, Long userId) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String storageKey = generateStorageKey(userId, extension);

        return objectStorageUploadTimer.record(() -> {
            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(storageKey)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

                log.info("File uploaded successfully: {} (size: {} bytes)", storageKey, file.getSize());
                return storageKey;

            } catch (IOException e) {
                log.error("Failed to upload file", e);
                throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        });
    }

    /**
     * Pre-signed URL 생성 (다운로드용)
     */
    public String generatePresignedUrl(String storageKey) {
        return presignedUrlTimer.record(() -> {
            try {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(storageKey)
                        .build();

                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(presignedUrlExpiration))
                        .getObjectRequest(getObjectRequest)
                        .build();

                PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

                return presignedRequest.url().toString();

            } catch (Exception e) {
                log.error("Failed to generate presigned URL for key: {}", storageKey, e);
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate download URL");
            }
        });
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String storageKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storageKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully: {}", storageKey);

        } catch (Exception e) {
            log.error("Failed to delete file: {}", storageKey, e);
            throw new CustomException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE, "Only image files are allowed");
        }

        // 50MB 제한
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED, "File size must not exceed 50MB");
        }
    }

    /**
     * Storage Key 생성
     */
    private String generateStorageKey(Long userId, String extension) {
        String uuid = UUID.randomUUID().toString();
        return String.format("users/%d/photos/%s%s", userId, uuid, extension);
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}