package com.jonathan.ecommerce.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFoundEntities(ResourceNotFoundException ex) {
    log.warn("Resource not found: {}", ex.getMessage());
    return createErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(ResourceAlreadyExistsException.class)
  public ResponseEntity<Map<String, String>> handleDuplicateEntities(
      ResourceAlreadyExistsException ex) {
    return createErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(InsufficientStockException.class)
  public ResponseEntity<Map<String, String>> manageInsufficientStock(
      InsufficientStockException ex) {
    return createErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleException(Exception ex) {
    log.error("Unexpected error: ", ex);
    return createErrorResponse(
        "An unexpected server error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // Método privado para evitar repetir la creación del Map
  private ResponseEntity<Map<String, String>> createErrorResponse(
      String message, HttpStatus status) {
    Map<String, String> error = new HashMap<>();
    error.put("error", message);
    return ResponseEntity.status(status).body(error);
  }
}
