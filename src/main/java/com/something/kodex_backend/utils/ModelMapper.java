package com.something.kodex_backend.utils;

import com.something.kodex_backend.auth.SignupResponseDto;
import com.something.kodex_backend.user.User;
import org.springframework.stereotype.Component;

@Component
public class ModelMapper {

  public SignupResponseDto toSignupResponseDto(User user) {
    return new SignupResponseDto(user.getId(), user.getUsername());
  }

}
