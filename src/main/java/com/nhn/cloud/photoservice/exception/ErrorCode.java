package com.nhn.cloud.photoservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "Invalid input value"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "Invalid file type"),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "File size exceeded"),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "Expired token"),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    ALBUM_NOT_FOUND(HttpStatus.NOT_FOUND, "Album not found"),
    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "Photo not found"),

    // 409 Conflict
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "Email already exists"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed"),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "File delete failed");

    private final HttpStatus status;
    private final String message;
}