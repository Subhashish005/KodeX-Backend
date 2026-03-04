package com.something.kodex_backend.customException;

public class DuplicateProjectException extends RuntimeException {
  public DuplicateProjectException(String message) {
    super(message);
  }
}