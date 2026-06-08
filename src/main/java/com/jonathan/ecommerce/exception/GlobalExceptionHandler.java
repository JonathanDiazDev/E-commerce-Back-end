package com.jonathan.ecommerce.exception;

import com.jonathan.ecommerce.dto.enums.ErrorCode;
import com.jonathan.ecommerce.dto.response.ErrorResponse;
import com.jonathan.ecommerce.dto.response.ValidationErrorResponse;
import com.jonathan.ecommerce.ratelimiting.exception.RateLimitExceededException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFoundEntities(ResourceNotFoundException ex) {
    log.warn("event=RESOURCE_NOT_FOUND traceId={} msg={}", MDC.get("traceId"), ex.getMessage());
    return createErrorResponse(
        ErrorCode.RESOURCE_NOT_FOUND, "Resource not found", HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<Object> handleRateLimitExceeded(RateLimitExceededException ex) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", "60") // Indica que puede reintentar en 60 segundos
        .body(Map.of("error", "Too Many Requests", "message", ex.getMessage()));
  }

  @ExceptionHandler(ResourceAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateEntities(ResourceAlreadyExistsException ex) {
    log.warn(
        "event=RESOURCE_ALREADY_EXISTS traceId={} msg={}", MDC.get("traceId"), ex.getMessage());
    return createErrorResponse(
        ErrorCode.RESOURCE_ALREADY_EXISTS, "Resource already exists", HttpStatus.CONFLICT);
  }

  @ExceptionHandler(InsufficientStockException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
    log.warn("event=INSUFFICIENT_STOCK traceId={} msg={}", MDC.get("traceId"), ex.getMessage());
    return createErrorResponse(
        ErrorCode.INSUFFICIENT_STOCK, "Insufficient stock", HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    log.error("event=UNEXPECTED_ERROR traceId={} msg={}", MDC.get("traceId"), ex.getMessage());
    return createErrorResponse(
        ErrorCode.INTERNAL_ERROR,
        "An unexpected server error occurred",
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(CartAccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleCartAccessDenied(CartAccessDeniedException ex) {
    log.warn("event=CartAccessDenied traceId={} msg={}", MDC.get("traceId"), ex.getMessage());
    return createErrorResponse(
        ErrorCode.AUTH_FORBIDDEN, "Cart access denied", HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {

    log.warn("event=LOGIN_FAILED traceId={} msg={}", MDC.get("traceId"), ex.getMessage());

    return createErrorResponse(
        ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid credentials", HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
    log.warn("event=EMAIL_EXISTS traceId={} msg={}", MDC.get("traceId"), ex.getMessage());

    return createErrorResponse(
        ErrorCode.RESOURCE_ALREADY_EXISTS, "Email already exists", HttpStatus.CONFLICT);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    log.error(
        "event=DATA_INTEGRITY_VIOLATION traceId={} msg={}", MDC.get("traceId"), ex.getMessage());

    return createErrorResponse(
        ErrorCode.RESOURCE_ALREADY_EXISTS, "Data integrity violation", HttpStatus.CONFLICT);
  }

  private ResponseEntity<ErrorResponse> createErrorResponse(
      ErrorCode code, String message, HttpStatus status) {
    return ResponseEntity.status(status)
        .body(
            new ErrorResponse(
                code.getCode(), MDC.get("traceId"), message, System.currentTimeMillis()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex) {

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
    log.warn("event=VALIDATION_ERROR errors={}", errors);

    return ResponseEntity.badRequest()
        .body(new ValidationErrorResponse(errors, System.currentTimeMillis()));
  }
}
