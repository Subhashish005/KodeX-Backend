package com.something.kodex_backend.error;

public class DuplicateProjectException extends RuntimeException {
  public DuplicateProjectException(String message) {
    super(message);
  }
}