package com.something.kodex_backend.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomGoogleAccessTokenResponse {

  private String azp;

  private String aud;

  private String sub;

  @JsonProperty("scope")
  private String scopes;

  private long exp;

  @JsonProperty("expires_in")
  private long expiresIn;

  private String email;

  @JsonProperty("email_verified")
  private boolean emailVerified;

}