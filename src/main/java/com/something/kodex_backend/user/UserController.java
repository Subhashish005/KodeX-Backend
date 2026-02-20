package com.something.kodex_backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  // TODO: there should be a 2-factor
  // authentication for deleting the user account
  // userId in the argument is not redundant
  // it is used by PreAuthorize, don't remove or it won't work
  @PreAuthorize("#userId == authentication.principal.username")
  @DeleteMapping("/{userId}")
  public ResponseEntity<?> deleteUser(
    @CookieValue("refresh_token") String refreshToken,
    @PathVariable String userId
  ) throws MissingRequestCookieException, NoSuchMethodException {
    return userService.deleteUser(refreshToken);
  }

}