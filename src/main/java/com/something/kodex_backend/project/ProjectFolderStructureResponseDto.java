package com.something.kodex_backend.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProjectFolderStructureResponseDto {

  private FileType type;
  private String name;
  @JsonProperty("relative_path")
  private String relativePath;
  private List<ProjectFolderStructureResponseDto> content;

}