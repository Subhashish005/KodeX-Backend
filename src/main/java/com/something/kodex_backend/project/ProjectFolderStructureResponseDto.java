package com.something.kodex_backend.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProjectFolderStructureResponseDto {

  private FileType type;
  private String name;
  @JsonProperty("id")
  private String id;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<ProjectFolderStructureResponseDto> children;

}