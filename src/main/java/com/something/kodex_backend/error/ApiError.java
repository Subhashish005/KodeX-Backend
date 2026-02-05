package com.something.kodex_backend.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public class ApiError {

  @JsonProperty("local_date_time")
  private final LocalDateTime localDateTime;

  @JsonProperty("error_msg")
  private final String errorMsg;

  @JsonProperty("error_code")
  private final String errorCode;

  @JsonProperty("http_status")
  private final HttpStatus httpStatus;

  public ApiError(
    String errorCode,
    String errorMsg,
    HttpStatus httpStatus
  ) {
    this.localDateTime = LocalDateTime.now();
    this.errorMsg = errorMsg;
    this.errorCode = errorCode;
    this.httpStatus = httpStatus;
  }

}
