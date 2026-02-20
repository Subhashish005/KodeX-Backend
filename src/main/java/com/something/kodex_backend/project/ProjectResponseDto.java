package com.something.kodex_backend.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ProjectResponseDto {

  private Integer id;
  private String name;
  @JsonProperty("created_at")
  private Instant createdAt;
  @JsonProperty("modified_at")
  private Instant modifiedAt;
  private String language;

}