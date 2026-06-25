package com.jonathan.ecommerce.exception;

import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.ratelimiting.exception.RateLimitExceededException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @InjectMocks private GlobalExceptionHandler globalExceptionHandler;

  @Test
  void handleResourceNotFound() {
    ResourceNotFoundException ex = new ResourceNotFoundException("Product not found");

    ResponseEntity<?> response = globalExceptionHandler.handleNotFoundEntities(ex);

    org.assertj.core.api.Assertions.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void handleRateLimitExceeded() {
    RateLimitExceededException ex = new RateLimitExceededException("Too many requests");

    ResponseEntity<Object> response = globalExceptionHandler.handleRateLimitExceeded(ex);

    org.assertj.core.api.Assertions.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    org.assertj.core.api.Assertions.assertThat(response.getHeaders().get("Retry-After"))
        .contains("60");
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    org.assertj.core.api.Assertions.assertThat(body.get("error")).isEqualTo("Too Many Requests");
  }

  @Test
  void handleResourceAlreadyExists() {
    ResourceAlreadyExistsException ex = new ResourceAlreadyExistsException("Category exists");

    ResponseEntity<?> response = globalExceptionHandler.handleDuplicateEntities(ex);

    org.assertj.core.api.Assertions.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void handleInsufficientStock() {
    InsufficientStockException ex = new InsufficientStockException("Not enough stock");

    ResponseEntity<?> response = globalExceptionHandler.handleInsufficientStock(ex);

    org.assertj.core.api.Assertions.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void handleGenericException() {
    Exception ex = new RuntimeException("Unexpected error");

    ResponseEntity<?> response = globalExceptionHandler.handleException(ex);

    org.assertj.core.api.Assertions.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  void handleCartAccessDenied() {
    CartAccessDeniedException ex = new CartAccessDeniedException("Not your cart");

    ResponseEntity<?> response = globalExceptionHandler.handleCartAccessDenied(ex);

    org.assertj.core.api.Assertions.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void handleBadCredentials() {
    BadCredentialsException ex = new BadCredentialsException("Invalid credentials");

    ResponseEntity<?> response = globalExceptionHandler.handleBadCredentials(ex);

    org.assertj.core.api.Assertions.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void handleEmailAlreadyExists() {
    EmailAlreadyExistsException ex = new EmailAlreadyExistsException("Email taken");

    ResponseEntity<?> response = globalExceptionHandler.handleEmailAlreadyExists(ex);

    org.assertj.core.api.Assertions.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void handleDataIntegrityViolation() {
    DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate key");

    ResponseEntity<?> response = globalExceptionHandler.handleDataIntegrityViolation(ex);

    org.assertj.core.api.Assertions.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
  }
}
