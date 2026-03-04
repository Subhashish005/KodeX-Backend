package com.something.kodex_backend.customException;

public class EmailMismatchException extends RuntimeException {
  public EmailMismatchException(String message) {
    super(message);
  }
}