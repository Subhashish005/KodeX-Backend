package com.something.kodex_backend.utils;

import com.something.kodex_backend.auth.SignupResponseDto;
import com.something.kodex_backend.project.Project;
import com.something.kodex_backend.project.ProjectResponseDto;
import com.something.kodex_backend.user.User;
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