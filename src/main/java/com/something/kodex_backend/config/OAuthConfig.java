package com.something.kodex_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("spring.security.oauth2.client.registration.google")
// TODO: this class should be renamed to something else
public class OAuthConfig {

  // @Value annotation can be used as well
  private String clientId;
  private String clientSecret;
  private String applicationName;

  private String oAuth2RedirectUri = "http://localhost:8080/api/v1/oauth2/login/google/callback";

}