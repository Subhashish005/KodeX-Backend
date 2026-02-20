package com.something.kodex_backend.error;

public class EmailMismatchException extends RuntimeException {
  public EmailMismatchException(String message) {
    super(message);
  }
}