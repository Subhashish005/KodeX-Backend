package com.something.kodex_backend.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomGoogleUserInfo {

  @JsonProperty("sub")
  private String id;

  @JsonProperty("name")
  private String username;

  @JsonProperty("given_name")
  private String firstname;

  @JsonProperty("family_name")
  private String lastname;

  @JsonProperty("email")
  private String gmail;

  @JsonProperty("picture")
  private String picture;

  @JsonProperty("email_verified")
  private boolean emailVerified;

}