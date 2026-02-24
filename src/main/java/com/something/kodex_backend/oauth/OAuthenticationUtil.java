package com.something.kodex_backend.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.something.kodex_backend.config.OAuthConfig;
import com.something.kodex_backend.project.ProjectRequestDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.net.URI;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class OAuthenticationUtil {

  private final OAuthConfig oAuthConfig;

  // this method can be used as a backup method if for some reason id method fails
  public CustomGoogleUserInfo fetchUserInfoUsingAccessToken(String accessToken) {
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    HttpEntity<String> entity = new HttpEntity<> (headers);

    try {
      return restTemplate.exchange(
        "https://www.googleapis.com/oauth2/v3/userinfo",
        HttpMethod.GET,
        entity,
        CustomGoogleUserInfo.class
      ).getBody();
    } catch(RestClientException ex) {
      throw new RuntimeException(ex);
    }
  }

  public CustomGoogleUserInfo fetchUserInfoUsingIdToken(String idToken) {
    String[] parts = idToken.split("\\.");

    if(parts.length != 3) throw new JwtException("Invalid JWT provided!");

    String header    = parts[0];
    String payload   = parts[1];

    PublicKey googlePublicKey;

    try {
      googlePublicKey = getMatchingGooglePublicKey(header);
    } catch(NoSuchAlgorithmException | InvalidKeySpecException ex) {
      throw new RuntimeException(ex);
    }

    if(!isIdTokenValid(idToken, googlePublicKey, payload))
      throw new JwtException("Provided JWT is not valid!");

    ObjectMapper mapper = new ObjectMapper();

    return mapper.readValue(Base64.getUrlDecoder().decode(payload), CustomGoogleUserInfo.class);
  }

  private boolean isIdTokenValid(
    String idToken,
    PublicKey googlePublicKey,
    String payload
  ) {
    if(!verifySignature(idToken, googlePublicKey))
      throw new JwtException("Signature verification failed for the provided token!");

    Claims claims = extractAllClaims(payload);

    String issuer = claims.getIssuer();
    Set<String> audience = claims.getAudience();
    Date expiresAt = claims.getExpiration();

    return issuer.equals("https://accounts.google.com") &&
      audience.contains(oAuthConfig.getClientId()) &&
      expiresAt.after(new Date(System.currentTimeMillis()));
  }

  private PublicKey getMatchingGooglePublicKey(
    String header
  ) throws NoSuchAlgorithmException, InvalidKeySpecException {
    // decode the header before anything
    header = new String(Base64.getUrlDecoder().decode(header));

    // first find the kid in the header
    Pattern kidPattern = Pattern.compile("\"kid\"\\s*:\\s*\"([^\"]+)\"");
    Matcher kidMatcher = kidPattern.matcher(header);

    if(!kidMatcher.find())
      throw new JsonParseException(new Throwable("No kid found in provided header!"));

    String headerKid = kidMatcher.group(1);

    // now extract keys from JSON response (usually 2, hopefully it remains two, or we crash)
    String keysJson = getGooglePublicKeysJson();

    Pattern keyPattern = Pattern.compile("\"keys\"\\s*:\\s*\\[([^]]+)]");
    Matcher keyMatcher = keyPattern.matcher(keysJson);

    // if keys are found then look which kid's mod and exp we need
    if(!keyMatcher.find())
      throw new JsonParseException(new Throwable("No keys found in provided JSON!"));

    Pattern modPattern = Pattern.compile("\"n\"\\s*:\\s*\"([^\"]+)\"");
    Pattern expPattern = Pattern.compile("\"e\"\\s*:\\s*\"([^\"]+)\"");
    Matcher modMatcher = modPattern.matcher(keysJson);
    Matcher expMatcher = expPattern.matcher(keysJson);

    // check the first kid
    kidMatcher = kidPattern.matcher(keysJson);

    if(!kidMatcher.find() || !modMatcher.find() || !expMatcher.find())
      throw new JsonParseException(new Throwable("No kid or mod or exp found in provided JSON!"));

    String jsonKid = kidMatcher.group(1);

    if(headerKid.equals(jsonKid)) {
      return constructPublicKey(modMatcher.group(1), expMatcher.group(1));
    }

    // if first kid check failed try second kid
    if(!kidMatcher.find(kidMatcher.start(1)))
      throw new JsonParseException(
        new Throwable("Expected 2 keys in provided JSON but found only 1!")
      );

    jsonKid = kidMatcher.group(1);

    if(!expMatcher.find(expMatcher.start(1)) || !modMatcher.find(modMatcher.start(1)))
      throw new JsonParseException(new Throwable("No kid or mod found in provided JSON!"));

    if(!headerKid.equals(jsonKid))
      throw new JsonParseException(
        new Throwable("no matching kid found for provided header!")
      );

    return constructPublicKey(modMatcher.group(1), expMatcher.group(1));
  }

  private PublicKey constructPublicKey(
    String modStr,
    String expStr
  ) throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] modBytes = Base64.getUrlDecoder().decode(modStr);
    byte[] expBytes = Base64.getUrlDecoder().decode(expStr);

    BigInteger modulus = new BigInteger(1, modBytes);
    BigInteger exponent = new BigInteger(1, expBytes);

    RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
    KeyFactory factory = KeyFactory.getInstance("RSA");

    return factory.generatePublic(spec);
  }

  // google uses something called rolling keys
  // usually 2 keys at a time
  private String getGooglePublicKeysJson() {
    URI jwtKeySetUrl = URI.create("https://www.googleapis.com/oauth2/v3/certs");

    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> entity = new HttpEntity<> (headers);

    ResponseEntity<String> response = restTemplate.exchange(
      jwtKeySetUrl,
      HttpMethod.GET,
      entity,
      String.class
    );

    return response.getBody();
  }

  private boolean verifySignature(String idToken, PublicKey publicKey) {
    String[] parts = idToken.split("\\.");

    if(parts.length != 3) throw new JwtException("Invalid JWT provided!");

    String data = parts[0] + "." + parts[1];
    byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);


    try {
      Signature sig = Signature.getInstance("SHA256withRSA");
      sig.initVerify(publicKey);
      sig.update(data.getBytes());

      return sig.verify(signatureBytes);
    } catch(InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  // if key or value has quote character(") in it this method will fail
  // horribly that is
  private Claims extractAllClaims(String payload) {
    payload = new String(Base64.getUrlDecoder().decode(payload));
    payload = payload.substring(1, payload.length() - 1);

    String[] claimsArray = payload.split(",");

    Map<String, Object> claims = new HashMap<> ();

    Pattern keyPattern   = Pattern.compile("\"([^\"]+)\"");
    Pattern valuePattern = Pattern.compile(":\\s*\"?([^\"]+)\"?");
    Matcher keyMatcher = null;
    Matcher valueMatcher = null;

    for(String claim : claimsArray) {
      // extract key
      keyMatcher = keyPattern.matcher(claim);

      if(!keyMatcher.find()) continue;

      String key = keyMatcher.group(1);

      // extract value
      valueMatcher = valuePattern.matcher(claim);

      if(!valueMatcher.find()) continue;

      Object value = valueMatcher.group(1);

      claims.put(key, value);
    }

    return new DefaultClaims(claims);
  }

}