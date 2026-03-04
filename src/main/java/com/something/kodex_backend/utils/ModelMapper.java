package com.something.kodex_backend.utils;

import com.something.kodex_backend.dtos.SignupResponseDto;
import com.something.kodex_backend.model.Project;
import com.something.kodex_backend.dtos.ProjectResponseDto;
import com.something.kodex_backend.model.User;
import org.springframework.stereotype.Component;

@Component
public class ModelMapper {

  public SignupResponseDto toSignupResponseDto(User user) {
    return new SignupResponseDto(user.getId(), user.getUsername());
  }

  public ProjectResponseDto toProjectResponseDto(Project project) {
    return new ProjectResponseDto(
      project.getId(),
      project.getName(),
      project.getCreatedAt(),
      project.getModifiedAt(),
      project.getLanguage()
    );
  }

}