package com.something.kodex_backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  // TODO: there should be a 2-factor
  // authentication for deleting the user account
  @DeleteMapping("/me")
  public ResponseEntity<?> deleteUser(
    @CookieValue("refresh_token") String refreshToken
  ) throws MissingRequestCookieException, NoSuchMethodException {
    return userService.deleteUser(refreshToken);
  }

}
