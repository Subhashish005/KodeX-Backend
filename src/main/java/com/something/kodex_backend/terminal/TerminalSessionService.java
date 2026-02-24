package com.something.kodex_backend.terminal;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TerminalSessionService {

  private final DockerService dockerService;
  private final DockerClient dockerClient;

  private final Map<String, TerminalSession> sessionMap = new ConcurrentHashMap<> ();

  // used to clean up container if no session is associated with it
  // TODO: in future make container persist a little bit
  // after all sessions associated with are removed to give
  // a chance for reconnect and save on cold start of container
  private final Map<String, Integer> userSessionCountMap = new ConcurrentHashMap<> ();
  private final static Path LOCAL_ROOT = Path.of("/tmp/kodex/projects");

  public void createTerminal(String userId, Integer projectId, WebSocketSession ws) throws IOException {
    if(userSessionCountMap.getOrDefault(userId, 0) >= 4) {
      System.err.printf(
        "User with userId: %s has reached max allowed terminal limit! (which is 4)",
        userId
      );

      return;
    }

    // assume the local directory for project already exists
    String containerId = dockerService.getOrCreateForUser(userId, LOCAL_ROOT.resolve(projectId.toString()));

    dockerService.startContainer(containerId);

    // make a new terminal process inside container
    ExecCreateCmdResponse exec = dockerService.createExec(containerId);

    String sessionId = ws.getId();

    PipedOutputStream pipedOutputStream = new PipedOutputStream();
    PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream);

    dockerClient.execStartCmd(exec.getId())
      .withTty(true)
      // TODO: why exactly detaching gives a funny error?
      .withDetach(false)
      .withStdIn(pipedInputStream)
      .exec(
        new ResultCallback.Adapter<Frame> () {
          // this is for stdout and stderr both
          @Override
          public void onNext(Frame frame) {
            try {
              ws.sendMessage(
                new BinaryMessage(frame.getPayload())
              );
            } catch(IOException ex) {
              throw new RuntimeException(ex);
            }
          }
        }
      );

    TerminalSession ts = new TerminalSession.TerminalSessionBuilder()
      .userId(userId)
      .containerId(containerId)
      .execId(exec.getId())
      .outputStream(pipedOutputStream)
      .build();

    // BiFunction<Integer, Integer, Integer> sum = (a, b) -> a + b;

    userSessionCountMap.merge(
      userId,
      1,
      Integer::sum
    );

    sessionMap.put(sessionId, ts);
  }

  public void sendInput(String sessionId, String data) throws IOException {
    TerminalSession ts = sessionMap.get(sessionId);

    if(ts == null) return;

    ts.outputStream.write(data.getBytes(StandardCharsets.UTF_8));
    ts.outputStream.flush();
  }

  public void handleResize(String sessionId, int cols, int rows) {
    TerminalSession ts = sessionMap.get(sessionId);

    if(ts == null) return;

    try {
      dockerClient.resizeExecCmd(ts.execId)
        .withSize(rows, cols)
        .exec();
    } catch(Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void closeSession(String sessionId) {
    TerminalSession ts = sessionMap.get(sessionId);
    if(ts == null) return;

    try {
      ts.outputStream.close();
    } catch(Exception ex) {
      throw new RuntimeException(ex);
    }

    int count = userSessionCountMap.merge(
      ts.userId,
      -1,
      Integer::sum
    );

    if(count <= 0) dockerService.stopAndRemoveContainer(ts.userId, ts.containerId);

    sessionMap.remove(sessionId);
  }
}