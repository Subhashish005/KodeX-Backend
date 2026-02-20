package com.something.kodex_backend.oauth;

import com.something.kodex_backend.config.OAuthConfig;
import com.something.kodex_backend.error.EmailMismatchException;
import com.something.kodex_backend.error.MissingTokenException;
import com.something.kodex_backend.error.UserNotFoundException;
import com.something.kodex_backend.token.Token;
import com.something.kodex_backend.token.TokenRepository;
import com.something.kodex_backend.token.TokenType;
import com.something.kodex_backend.user.User;
import com.something.kodex_backend.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuthService {

  private final OAuthConfig oAuthConfig;
  private final OAuthenticationUtil oAuthenticationUtil;
  private final UserRepository userRepository;
  private final TokenRepository tokenRepository;

  public void redirectToGoogle(
    Integer userId,
    HttpServletResponse response) {
    // make sure a user with provided userId exists in db
    boolean isUserPresent = userRepository.checkIfUserExistsById(userId)
      .orElseThrow(
        () -> new RuntimeException("Expected a not null value got a null value instead!")
      );

    if(!isUserPresent)
      throw new UserNotFoundException("User with given id of " + userId + " not found in database!");

    String googleUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
      "client_id=" + oAuthConfig.getClientId() +
      "&redirect_uri=" + oAuthConfig.getOAuth2RedirectUri() +
      "&response_type=code" +
      "&scope=openid%20email%20profile%20https://www.googleapis.com/auth/drive.file" +
      "&access_type=offline" +
      "&prompt=consent" +
      // pass any value you want back as a query parameter named 'state'
      // otherwise Google will probably remove it in callback
      "&state=" + userId;

    try {
      response.sendRedirect(googleUrl);
    } catch(IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void handleCallback(
    String code,
    Integer userId,
    HttpServletResponse response
  ) {
    CustomGoogleTokenResponse tokenResponse = exchangeCodeForToken(code);

    List<String> scopes = Arrays.asList(tokenResponse.getScopes().split(" "));

    if(!scopes.contains("https://www.googleapis.com/auth/drive.file")) {
      throw new RuntimeException("User did not accepted the drive scope!");
    }

    CustomGoogleUserInfo userInfo =
      oAuthenticationUtil.fetchUserInfoUsingIdToken(tokenResponse.getIdToken());

    if(!userInfo.isEmailVerified())
      throw new RuntimeException("User's Email is not verified!");

    User user = userRepository.findById(userId).orElseThrow(
      () -> new UserNotFoundException("User with given id of " + userId + " not found in database!")
    );

    removeAllUserOAuthRefreshTokens(userId);

    String refreshToken = tokenResponse.getRefreshToken();

    // this can be used later on if access token expires while syncing
    // backend will get an access token to complete syncing
    saveUserOAuthRefreshToken(refreshToken, user);

    Cookie refreshTokenCookie = getRefreshCookie(user, userInfo, refreshToken);

    // This access token is going to be wasted for the sake of simplicity.
    // otherwise, I have to pass a public key from frontend while redirecting to backend
    // pass it using state parameter while redirecting to google
    // extract it after Google's redirect then encrypt access token using it
    // finally appending access token to frontend redirect url
    // and somehow frontend has to store the private key in session or something
    // final String oAuthAccessToken = tokenResponse.getAccessToken();

    response.addCookie(refreshTokenCookie);

    try {
      response.sendRedirect("http://localhost:8090/auth-success/");
    } catch(IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public ResponseEntity<Map<String, String>> refreshToken(
    String refreshToken
  ) {
    String oAuthAccessToken = getAccessToken(refreshToken);

    return ResponseEntity.ok(Map.of("oauth_access_token", oAuthAccessToken));
  }

  private CustomGoogleTokenResponse exchangeCodeForToken(String code) {
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<MultiValueMap<String, String>> request =
      requestURLBuilderForLogin(code, headers);

    try {
      return restTemplate.postForObject(
        "https://oauth2.googleapis.com/token",
        request,
        CustomGoogleTokenResponse.class
      );

    } catch(RestClientException ex) {
      throw new RuntimeException(ex);
    }
  }

  private Cookie getRefreshCookie(
    User user,
    CustomGoogleUserInfo userInfo,
    String oAuthRefreshToken
  ) {
    if(!user.getEmail().equals(userInfo.getGmail()))
      throw new EmailMismatchException("OAuth2 email doesn't match with the sign up email!");

    if(oAuthRefreshToken == null) throw new MissingTokenException("Refresh token not present!");

    Cookie cookie = new Cookie("oauth_refresh_token", oAuthRefreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(false);
    cookie.setPath("/api/v1");
    cookie.setMaxAge(604800000); // 7 days, the app is in testing and R.T. expires after 7 days
    cookie.setAttribute("SameSite", "Strict");

    return cookie;
  }

  private HttpEntity<MultiValueMap<String, String>> requestURLBuilderForLogin(
    String code, HttpHeaders headers
  ) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<> ();

    params.add("code", code);
    params.add("client_id", oAuthConfig.getClientId());
    params.add("client_secret", oAuthConfig.getClientSecret());
    params.add("redirect_uri", oAuthConfig.getOAuth2RedirectUri());
    params.add("grant_type", "authorization_code");

    return new HttpEntity<> (params, headers);
  }

  private HttpEntity<MultiValueMap<String, String>> requestURLBuilderForRefresh(
    String refreshToken, HttpHeaders headers
  ) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<> ();

    params.add("client_id", oAuthConfig.getClientId());
    params.add("client_secret", oAuthConfig.getClientSecret());
    params.add("refresh_token", refreshToken);
    params.add("grant_type", "refresh_token");

    return new HttpEntity<> (params, headers);
  }

  private String getAccessToken(String refreshToken) {
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<MultiValueMap<String, String>> request =
      requestURLBuilderForRefresh(refreshToken, headers);

    try {
      CustomGoogleTokenResponse tokenResponse = restTemplate.postForObject(
        "https://oauth2.googleapis.com/token",
        request,
        CustomGoogleTokenResponse.class
      );

      if(tokenResponse == null) throw new RuntimeException("Failed to receive a token response from Google!");

      String accessToken = tokenResponse.getAccessToken();

      if(accessToken == null) throw new MissingTokenException("Error while obtaining oauth access token!");

      return accessToken;

    } catch(RestClientException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void removeAllUserOAuthRefreshTokens(Integer userId) {
    // remove all previous refresh token, we only keep the latest one
    List<Token> refreshTokens = tokenRepository.findAllOAuthRefreshTokensByUserId(userId).orElse(null);

    if(refreshTokens == null) return;

    for(Token refreshToken : refreshTokens) {
      tokenRepository.delete(refreshToken);
    }
  }

  private void saveUserOAuthRefreshToken(String refreshToken, User user) {
    tokenRepository.save(
      Token.builder()
        .value(refreshToken)
        .type(TokenType.OAUTH2_REFRESH)
        .revoked(false)
        .user(user)
        .build()
    );
  }

}