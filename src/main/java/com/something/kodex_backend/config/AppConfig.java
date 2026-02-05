package com.something.kodex_backend.config;

import com.something.kodex_backend.user.Role;
import com.something.kodex_backend.user.User;
import com.something.kodex_backend.user.UserRepository;
import com.something.kodex_backend.utils.ModelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

  private final UserRepository userRepository;

  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(
    AuthenticationConfiguration authenticationConfiguration
  ) {
    return authenticationConfiguration.getAuthenticationManager();
  }

  // TODO: remove this after you're done experimenting
//  @Bean
  public CommandLineRunner commandLineRunner() {
    return args -> userRepository.save(
      User.builder()
        .username("subhashish005")
        .email("subhashish005@gmail.com")
        .password(passwordEncoder().encode("Subh#2003"))
        .role(Role.STUDENT)
        .build()
    );
  }

}
