package com.something.kodex_backend.config;


import com.something.kodex_backend.terminal.TerminalWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

  private final TerminalWebSocketHandler terminalWebSocketHandler;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
      .addHandler(terminalWebSocketHandler, "/terminal")
      .setAllowedOrigins("http://localhost:8090");
  }

}