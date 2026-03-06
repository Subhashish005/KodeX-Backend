package com.something.kodex_backend.project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
// TODO: think of a better name for this
public class ProjectChildrenRequestDto {

  String hash;
  String name;

}