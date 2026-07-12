package com.yue.jobcomparer.exception;

import com.yue.jobcomparer.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Analysis not found
    @ExceptionHandler(AnalysisNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAnalysisNotFound(
            AnalysisNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // 404 - CV not found
    @ExceptionHandler(CvNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCvNotFound(
            CvNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // 409 - limit exceeded
    @ExceptionHandler(CvLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleCvLimitExceeded(
            CvLimitExceededException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // 404 - job not found
    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobNotFound(
            JobNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // 409 - job limit exceeded
    @ExceptionHandler(JobLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleJobLimitExceeded(
            JobLimitExceededException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // 409 - duplicate name
    @ExceptionHandler(DuplicateCvNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCvName(
            DuplicateCvNameException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // 400 - Bean Validation failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 400 - malformed JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Malformed JSON request", request);
    }

    // 400 - illegal argument (e.g. login failure from authService)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // 400 - invalid enum value or type mismatch in query/path parameter
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return buildError(HttpStatus.BAD_REQUEST, message, request);
    }

    // 400 - invalid avatar exception
    @ExceptionHandler(InvalidAvatarException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAvatar(
            InvalidAvatarException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // 413 - upload exceeds the configured multipart size limit
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildError(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the maximum allowed size", request);
    }

    // 429 - rate limit exceeded
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException ex, HttpServletRequest request) {
        return buildError(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }

    // 502 - AI response parse error
    @ExceptionHandler(AiResponseParseException.class)
    public ResponseEntity<ErrorResponse> handleAiResponseParseException(
            AiResponseParseException ex, HttpServletRequest request) {
        log.error("AI response parse error", ex);
        return buildError(
                HttpStatus.BAD_GATEWAY,
                "AI service returned an invalid response. Please try again.",
                request
        );
    }

    // 422 - PDF parsing failed
    @ExceptionHandler(PdfParsingException.class)
    public ResponseEntity<ErrorResponse> handlePdfParsing(
            PdfParsingException ex, HttpServletRequest request) {
        log.error("PDF paring failed", ex);
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    // 413 - file exceed size limit
    @ExceptionHandler(FileSizeLimitException.class)
    public ResponseEntity<ErrorResponse> handleFieldSizeLimit(
            FileSizeLimitException ex, HttpServletRequest request) {
        return buildError(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(), request);
    }

    // 500 - catch-all for unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
