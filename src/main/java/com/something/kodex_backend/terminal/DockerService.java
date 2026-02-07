package com.something.kodex_backend.terminal;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DockerService {

  private final DockerClient dockerClient;

  // userId to ContainerId
  private final Map<String, String> userContainerMap = new ConcurrentHashMap<> ();

  public String getOrCreateForUser(String userId) {
    String containerName = "term_" + userId;
    String containerId = userContainerMap.get(userId);

    // if there exists a container for the
    // user give it back
    if(containerId != null){
      return containerId;
    }

    // or make a new one if not
    CreateContainerResponse container = dockerClient.createContainerCmd("alpine:latest")
      .withName(containerName)
      .withTty(true)
      .withAttachStdin(true)
      .withAttachStdout(true)
      .withAttachStderr(true)
      .exec();

    userContainerMap.put(userId, container.getId());

    return container.getId();
  }

  public void startContainer(String containerId) {
    InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();

    String status = inspectContainerResponse.getState().getStatus();

    if(status != null && status.equals("running")) return;

    try {
      dockerClient.startContainerCmd(containerId).exec();
    } catch(Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public ExecCreateCmdResponse createExec(String containerId) {
    return dockerClient.execCreateCmd(containerId)
      .withCmd("/bin/sh")
      .withAttachStdin(true)
      .withAttachStderr(true)
      .withAttachStdout(true)
      .withTty(true)
      .exec();
  }

  public void stopAndRemoveContainer(String userId, String containerId) {
    System.err.println("removing container with id:" + containerId);

    try {
      dockerClient.stopContainerCmd(containerId)
        .exec();

      dockerClient.removeContainerCmd(containerId)
        .withForce(true)
        .exec();
    } catch(Exception ex) {
      throw new RuntimeException(ex);
    }

    userContainerMap.remove(userId);
  }
}