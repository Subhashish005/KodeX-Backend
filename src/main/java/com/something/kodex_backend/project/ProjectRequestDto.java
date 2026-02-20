package com.something.kodex_backend.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectRequestDto {

  @JsonProperty("project_name")
  private String projectName;

  @JsonProperty("project_language")
  private String projectLanguage;

  @JsonProperty("user_id")
  private Integer userId;

}