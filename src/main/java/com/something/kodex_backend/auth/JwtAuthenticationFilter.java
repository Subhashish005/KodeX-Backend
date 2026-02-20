package com.something.kodex_backend.auth;

import com.something.kodex_backend.user.User;
import com.something.kodex_backend.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final UserRepository userRepository;
  private final JwtAuthenticationUtil jwtAuthenticationUtil;
  private final HandlerExceptionResolver handlerExceptionResolver;

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) {

    try {
      final String requestTokenHeader = request.getHeader("Authorization");

      if(requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);

        return;
      }

      // TODO: check if the given token length is under a specific limit
      // 7 is the length of "Bearer "
      String token = requestTokenHeader.substring(7);

      String username = jwtAuthenticationUtil.getUsernameFromToken(token);

      // we don't want already authenticated user to be authenticated again
      // update the security context holder to holder this new authenticated user
      // SecurityContextHolder already knows if user is authenticated or not
      if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        User user = userRepository.findByUsername(username).orElseThrow();

        if(!jwtAuthenticationUtil.isAccessTokenValid(token, user)) {
          // TODO: for some reason this exception is not getting caught
          // by GlobalExceptionHandler(JwtException) even tho a specialized handler exists
          throw new JwtException("Provided Access token is not valid!");
        }

        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
          new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        // Question: what extra details are we trying to provide here
        // timestamp: 1:21:48
        // video link: https://youtu.be/BVdQ3iuovg0?si=L17kZSDqVqKVRbvl
        usernamePasswordAuthenticationToken.setDetails(
          new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
      }

      filterChain.doFilter(request, response);
    } catch(Exception ex) {
      // give this exception to handlerExceptionResolver
      // since we are in filters layer and our exception handler
      // is in MVC layer
      // spring will automatically detect that our GlobalExceptionHandler class can
      // handle this exception or not

      handlerExceptionResolver.resolveException(request, response, null, ex);
    }
  }
}