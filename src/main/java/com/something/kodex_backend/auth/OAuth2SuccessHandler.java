package com.something.kodex_backend.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

  @Override
  public void onAuthenticationSuccess(
    HttpServletRequest request,
    HttpServletResponse response,
    Authentication authentication
  ) throws IOException, ServletException {
    OAuth2AuthenticationToken oAuth2AuthenticationToken =
      (OAuth2AuthenticationToken) authentication;

    final String principalName = oAuth2AuthenticationToken.getName();
    final String clientRegistrationId = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();

    OAuth2AuthorizedClient oAuth2AuthorizedClient =
      oAuth2AuthorizedClientService.loadAuthorizedClient(clientRegistrationId, principalName);

//    oAuth2AuthenticationToken.

  }
}
