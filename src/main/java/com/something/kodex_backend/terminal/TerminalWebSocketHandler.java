package com.something.kodex_backend.terminal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Builder
record TerminalSession(
  PtyProcess process,
  InputStream stdin,
  OutputStream stdout,
  Thread readerThread
) {}

@Slf4j
public class TerminalWebSocketHandler extends AbstractWebSocketHandler {

  private final Map<String, TerminalSession> sessionMap = new ConcurrentHashMap<> ();

  // TODO: implement heartbeats to avoid keeping broken connection alive
  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws IOException {
    PtyProcess process = new PtyProcessBuilder()
      .setEnvironment(Map.of("TERM", "xterm"))
      .setCommand(new String[] {"/bin/bash", "-l"})
      .start();

    InputStream stdin = process.getInputStream();
    OutputStream stdout = process.getOutputStream();

    TerminalSession terminalSession = TerminalSession.builder()
      .process(process)
      .stdin(stdin)
      .stdout(stdout)
      .readerThread(new Thread(() -> {
        byte[] buffer = new byte[1024];
        int read = -1;

        try {
          while((read = stdin.read(buffer)) != -1) {
            session.sendMessage(
              new TextMessage(Arrays.copyOf(buffer, read))
            );
          }
        } catch(IOException ex) {
          Logger logger = LoggerFactory.getLogger(TerminalWebSocketHandler.class);
          logger.info(ex.getMessage());
        }
      }))
      .build();

    terminalSession.readerThread().start();

    sessionMap.put(session.getId(), terminalSession);
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
    try {
      String payload = textMessage.getPayload();
      TerminalSession terminalSession = sessionMap.get(session.getId());

      if(terminalSession == null) return;

      JsonNode node = new ObjectMapper().readTree(payload);
      String type = node.get("type").asText();

      if(type == null) return;

      PtyProcess process = terminalSession.process();

      switch(type) {
        case "resize":
          int cols = node.get("cols").asInt();
          int rows = node.get("rows").asInt();

          process.setWinSize(new WinSize(cols, rows));

          break;

        case "data":
          OutputStream stdout = terminalSession.stdout();

          stdout.write(node.get("data").asText().getBytes(StandardCharsets.UTF_8));
          stdout.flush();

          break;
      }
    } catch(IOException ex) {
      // should I keep it here or make it a class member?
      Logger logger = LoggerFactory.getLogger(TerminalWebSocketHandler.class);
      logger.info(ex.getMessage());
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
    TerminalSession terminalSession = sessionMap.remove(session.getId());

    if(terminalSession == null) return;

    terminalSession.process().destroy();
  }

}
