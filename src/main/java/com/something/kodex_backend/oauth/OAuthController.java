package com.something.kodex_backend.oauth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OAuthController {

  private final OAuthService oAuthService;

  @GetMapping("/oauth2/login/google")
  public void redirectToGoogle(
    @RequestParam("user_id") Integer userId,
    HttpServletResponse response) {
    // This situation is pretty weird.
    // Since my backend uses JWT for auth, I don't need to rely on Google oauth2.
    // The only reason to make this maneuver is to use user's Google Drive for storage.
    // The complication comes into play when I have to make sure that user's login Gmail
    // (the one in database) matches with the oauth one.
    // For this purpose the userId is passed as a query parameter to verify it later on

    oAuthService.redirectToGoogle(userId, response);
  }

  @GetMapping("/oauth2/login/google/callback")
  public void handleCallback(
    @RequestParam String code,
    @RequestParam Integer state,
    HttpServletResponse response
  ) {
    oAuthService.handleCallback(code, state, response);
  }

  @GetMapping("/refresh-token/google")
  public ResponseEntity<Map<String, String>> refreshToken(
    @CookieValue("oauth_refresh_token") String refreshToken
  ) {
    return oAuthService.refreshToken(refreshToken);
  }

}