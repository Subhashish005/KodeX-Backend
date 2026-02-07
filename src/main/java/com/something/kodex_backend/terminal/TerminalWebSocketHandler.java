package com.something.kodex_backend.terminal;

import com.something.kodex_backend.auth.JwtAuthenticationUtil;
import com.something.kodex_backend.user.User;
import com.something.kodex_backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@Component
public class TerminalWebSocketHandler extends AbstractWebSocketHandler {

  private final TerminalSessionService terminalSessionService;
  private final ObjectMapper objectMapper;
  private final UserRepository userRepository;
  private final JwtAuthenticationUtil jwtAuthenticationUtil;

  // TODO: implement heartbeats to avoid keeping broken connection alive
  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    session.getAttributes().put("authenticated", false);
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
    // TODO: make custom error handlers

    // check if user is authenticated or not
    // if not try to authenticate using provided access token
    // and create a terminal for this session
    // if user can't authenticate close connection
    // if already authenticated skip
    Object authenticated = session.getAttributes().get("authenticated");

    if(!((boolean) authenticated)) {
      try {
        String payload = textMessage.getPayload();
        JsonNode node = objectMapper.readTree(payload);

        if(!"auth".equals(node.get("type").asString())) {
          session.close(CloseStatus.NOT_ACCEPTABLE);
        }

        String accessToken = node.get("access_token").asString();
        String username = jwtAuthenticationUtil.getUsernameFromToken(accessToken);
        User user = userRepository.findByUsername(username).orElseThrow();

        if(!jwtAuthenticationUtil.isAccessTokenValid(accessToken, user)) {
          session.close(CloseStatus.NOT_ACCEPTABLE);
        }

        session.getAttributes().put("userId", username);
        session.getAttributes().put("authenticated", true);

        terminalSessionService.createTerminal(username, session);
      } catch(Exception ex) {
        throw new RuntimeException(ex);
      }

      return;
    }

    try {
      String payload = textMessage.getPayload();

      JsonNode node = objectMapper.readTree(payload);
      String type = node.get("type").asString();

      if(type == null) return;

      switch(type) {
        case "data":
          terminalSessionService.sendInput(
            session.getId(),
            node.get("data").asString()
          );

          break;

        case "resize":
          terminalSessionService.handleResize(
            session.getId(),
            node.get("cols").asInt(),
            node.get("rows").asInt()
          );

          break;
      }

    } catch(Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
    terminalSessionService.closeSession(session.getId());
  }

}