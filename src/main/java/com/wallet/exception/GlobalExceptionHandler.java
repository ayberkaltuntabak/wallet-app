package com.wallet.exception;

import com.wallet.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    FieldError fieldError = ex.getBindingResult().getFieldError();
    String message =
        fieldError != null
            ? fieldError.getField() + " " + fieldError.getDefaultMessage()
            : "Validation error";
    return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
  }

  @ExceptionHandler({WalletNotFoundException.class, CustomerNotFoundException.class})
  public ResponseEntity<ErrorResponse> handleNotFound(
      RuntimeException ex, HttpServletRequest request) {
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler({InsufficientBalanceException.class, InvalidTransactionStatusException.class})
  public ResponseEntity<ErrorResponse> handleBusiness(
      RuntimeException ex, HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler({UnauthorizedOperationException.class, BadCredentialsException.class})
  public ResponseEntity<ErrorResponse> handleUnauthorized(
      RuntimeException ex, HttpServletRequest request) {
    return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
  }

  private ResponseEntity<ErrorResponse> buildResponse(
      HttpStatus status, String message, String path) {
    ErrorResponse body =
        new ErrorResponse(
            LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path);
    return ResponseEntity.status(status).body(body);
  }
}
