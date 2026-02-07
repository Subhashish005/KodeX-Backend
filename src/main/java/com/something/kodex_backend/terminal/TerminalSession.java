package com.something.kodex_backend.terminal;

import lombok.Builder;

import java.io.OutputStream;

@Builder
public class TerminalSession {

  // userId is username since it is unique thanks to db
  public String userId;

  // each user is supposed to get only one container
  // which is ensured by the userId, since each container is named
  // using it
  public String containerId;

  // the id of current terminal associated with this websocket session
  public String execId;

  // the output stream of the current terminal
  public OutputStream outputStream;

}