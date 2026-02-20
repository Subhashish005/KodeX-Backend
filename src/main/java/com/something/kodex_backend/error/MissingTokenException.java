package com.something.kodex_backend.error;

public class MissingTokenException extends RuntimeException {
  public MissingTokenException(String message) {
    super(message);
  }
}