package com.example.studio.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    FILE_EMPTY(HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST),
    IMAGE_COUNT_MISMATCH(HttpStatus.BAD_REQUEST),
    INVALID_UPLOAD(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    EMAIL_EXISTS(HttpStatus.CONFLICT),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
