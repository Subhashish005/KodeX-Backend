package com.something.kodex_backend.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
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
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

  private final UserRepository userRepository;
  private final DockerClient dockerClient;

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

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  // TODO: remove this after you're done experimenting
  @Bean
  public CommandLineRunner insertDummyUser() {
    return args -> {
      userRepository.save(
        User.builder()
          .username("subhashish005")
          .email("subhashish005@gmail.com")
          .password(passwordEncoder().encode("Subh#2003"))
          .role(Role.STUDENT)
          .build()
      );
    };

  }

  // is there a better way to run some method after initialization
  // without creating an object?
  // idk, and maybe this object is going to be garbage collected anyway
  // !!DO NOT PUT THIS IN DockerConfig OR A CIRCULAR DEPENDENCY WILL OCCUR!!
  @Bean
  public CommandLineRunner removeAllContainers() {
    return args -> {
      List<Container> containers = dockerClient.listContainersCmd()
        .withShowAll(true).exec();

      for(Container c: containers) {
        List<String> names = Arrays.asList(c.getNames());

        names.forEach(name -> {
          if(name.startsWith("/term_")) {
            if(!"exited".equals(c.getStatus()))
              dockerClient.stopContainerCmd(c.getId()).exec();

            dockerClient.removeContainerCmd(c.getId()).exec();
          }
        });
      }
    };
  }

}