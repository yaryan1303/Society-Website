package com.esicsociety.ams.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Application exceptions, each mapped to an HTTP status by
 * {@link GlobalExceptionHandler}.
 */
public final class ApiExceptions {

    private ApiExceptions() {}

    /** 404 — entity not found. */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    /** 400 — invalid input or a violated business rule (e.g. CASH without receipt). */
    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }

    /** 403 — authenticated but not allowed (e.g. member requesting another member's data). */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    /** 409 — conflicting state (e.g. editing a closed year, duplicate account number). */
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    public static HttpStatus statusOf(RuntimeException ex) {
        if (ex instanceof NotFoundException) return HttpStatus.NOT_FOUND;
        if (ex instanceof BadRequestException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof ForbiddenException) return HttpStatus.FORBIDDEN;
        if (ex instanceof ConflictException) return HttpStatus.CONFLICT;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
