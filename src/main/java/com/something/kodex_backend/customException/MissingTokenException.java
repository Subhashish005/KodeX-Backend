package com.something.kodex_backend.customException;

public class MissingTokenException extends RuntimeException {
  public MissingTokenException(String message) {
    super(message);
  }
}