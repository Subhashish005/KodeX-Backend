package com.something.kodex_backend.error;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UsernameNotFoundException.class)
  public ResponseEntity<ApiError> handleUsernameNotFoundException(
    UsernameNotFoundException ex
  ) {
    ApiError apiError = new ApiError(
      "USER_NOT_FOUND",
      "Username not Found with username: " + ex.getMessage(),
      HttpStatus.NOT_FOUND
    );

    return new ResponseEntity<>(apiError, apiError.getHttpStatus());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiError> handleAuthenticationException(
    AuthenticationException ex
  ) {
    ApiError apiError = new ApiError(
      "CANNOT_AUTHENTICATE_USER",
      "User cannot be authenticated! try signup. error: " + ex.getMessage(),
      HttpStatus.UNAUTHORIZED
    );

    return new ResponseEntity<>(apiError, apiError.getHttpStatus());
  }

  @ExceptionHandler(JwtException.class)
  public ResponseEntity<ApiError> handleJwtException(JwtException ex) {
    ApiError apiError = new ApiError(
      "INVALID_TOKEN",
      "Invalid JWT token: " + ex.getMessage(),
      HttpStatus.UNAUTHORIZED
    );

    return new ResponseEntity<>(apiError, apiError.getHttpStatus());
  }

  // TODO: fix this exception (as user not found exception exists)
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ApiError> handleUserNotFoundException(
    NoSuchElementException ex
  ) {
    ApiError apiError = new ApiError(
      "USER_NOT_FOUND",
      "User Not Found in Database! error: " + ex.getMessage(),
      HttpStatus.NOT_FOUND
    );

    return new ResponseEntity<>(apiError, apiError.getHttpStatus());
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<?> handleUserAlreadyExistsException(
    UserAlreadyExistsException ex
  ) {
    ApiError apiError = new ApiError(
      "USER_ALREADY_EXISTS",
      ex.getMessage(),
      HttpStatus.CONFLICT
    );

    return new ResponseEntity<>(apiError, apiError.getHttpStatus());
  }

  @ExceptionHandler(MissingRequestCookieException.class)
  public ResponseEntity<ApiError> handleMissingRequestCookieException(
    MissingRequestCookieException ex
  ) {
    ApiError apiError = new ApiError(
      "MISSING_COOKIE",
      "Required cookie not found! error: " + ex.getMessage(),
      HttpStatus.NOT_FOUND
    );

    return new ResponseEntity<>(apiError, apiError.getHttpStatus());
  }

  @ExceptionHandler(MissingTokenException.class)
  public ResponseEntity<ApiError> handleMissingTokenException(
    MissingTokenException ex
  ) {
    ApiError apiError = new ApiError(
      "TOKEN_NOT_FOUND",
      "error: " + ex.getMessage(),
      HttpStatus.NOT_FOUND
    );

    return new ResponseEntity<> (apiError, apiError.getHttpStatus());
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ApiError> handleUserNotFoundException(
    UserNotFoundException ex
  ) {
    ApiError apiError = new ApiError(
      "USER_NOT_FOUND",
      "User not found in database! cause: " + ex.getMessage(),
      HttpStatus.NOT_FOUND
    );

    return new ResponseEntity<> (apiError, apiError.getHttpStatus());
  }

  @ExceptionHandler(EmailMismatchException.class)
  public ResponseEntity<ApiError> handleEmailMismatchException(EmailMismatchException ex) {
    ApiError apiError = new ApiError(
      "EMAIL_MISMATCH",
      "error: " + ex.getMessage(),
      HttpStatus.NOT_ACCEPTABLE
    );

    return new ResponseEntity<>(apiError, apiError.getHttpStatus());
  }

  @ExceptionHandler(DuplicateProjectException.class)
  public ResponseEntity<ApiError> handleDuplicateProjectException(DuplicateProjectException ex) {
    ApiError apiError = new ApiError(
      "DUPLICATE_PROJECT_NAME",
      "error: " + ex.getMessage(),
      HttpStatus.CONFLICT
    );

    return new ResponseEntity<>(apiError, apiError.getHttpStatus());
  }

  // if this method gets invoked something horribly has gone wrong
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleSomeException(Exception ex) {
    ApiError apiError = new ApiError(
      "EXCEPTION_OCCURRED",
      "Something went wrong, error: " + ex.getMessage(),
      HttpStatus.FORBIDDEN
    );

    ex.printStackTrace(System.err);

    return new ResponseEntity<>(apiError, apiError.getHttpStatus());
  }

}