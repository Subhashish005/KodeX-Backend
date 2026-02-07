package com.something.kodex_backend.config;

import com.something.kodex_backend.auth.JwtAuthenticationFilter;
import com.something.kodex_backend.auth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private static final String[] WHITE_LIST_URL = {
    "/api/v1/auth/**",
    "/api/v1/public/**",
    // TODO: setup a proper authorization for websocket connection
    "/terminal",
  };

  private final OAuth2SuccessHandler oAuth2SuccessHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
    httpSecurity
      .cors(c -> c.configurationSource(corsConfigurationSource()))
      // TODO: enable this in future
      .csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(sessionConfig ->
        sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .authorizeHttpRequests(authorizeHttpRequests ->
        authorizeHttpRequests
          .requestMatchers(WHITE_LIST_URL).permitAll()
          .anyRequest().authenticated()
      )
      .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//      .oauth2Login(oauth2 -> oauth2
//        // unnamed class instance pretty shrimple
//        .failureHandler(((
//          request,
//          response,
//          exception) -> {
//          log.error("OAuth2 error: {}", exception.getMessage());
//        }))
//        .successHandler(oAuth2SuccessHandler)
//      );

    return httpSecurity.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    var config = new CorsConfiguration();

    config.setAllowedOrigins(List.of("http://localhost:8090/"));
    config.setAllowedMethods(List.of("*"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    var source = new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration("/**", config);

    return source;
  }

}