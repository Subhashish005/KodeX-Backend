package com.something.kodex_backend.config;


import com.something.kodex_backend.terminal.TerminalWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class webSocketConfig implements WebSocketConfigurer {

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
      .addHandler(new TerminalWebSocketHandler(), "/terminal")
      .setAllowedOrigins("http://localhost:8090");
  }

}
