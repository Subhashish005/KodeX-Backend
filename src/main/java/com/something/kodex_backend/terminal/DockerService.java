package com.something.kodex_backend.terminal;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import com.something.kodex_backend.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerService {

  private final DockerClient dockerClient;
  private final ProjectRepository projectRepository;

  // userId to ContainerId
  private final Map<String, String> userContainerMap = new ConcurrentHashMap<> ();

  public String getOrCreateForUser(String userId, Path localProjectDir, Integer projectId) {
    String containerName = "term_" + userId;
    String containerId = userContainerMap.get(userId);

    // if there exists a container for the
    // user give it back
    if(containerId != null){
      return containerId;
    }

    HostConfig config = HostConfig
      .newHostConfig()
      .withReadonlyRootfs(true)
      .withBinds(
        new Bind(
          localProjectDir.toString(),
          new Volume("/workspace"),
          AccessMode.rw
        )
      )
      .withMemory(256 * 1024 * 1024L)
      .withCpuCount(1L)
      .withReadonlyRootfs(true)
      .withCapDrop(Capability.ALL)
      .withPidsLimit(128L)
      .withNetworkMode("none");

    String projectLanguage = projectRepository.findById(projectId).orElseThrow(
      () -> new IllegalArgumentException("No project with project id " + projectId + " found!")
    ).getLanguage();

    String imageName = "image_";

    switch(projectLanguage) {
      case "Go":
        imageName += "go:v1";
        break;
      case "Java":
        imageName += "java:v1";
        break;
      case "C++":
        imageName += "cpp:v1";
        break;
      default:
        throw new IllegalStateException("Unknown project language: " + projectLanguage);
    }

    // or make a new one if not
    CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
      .withName(containerName)
      .withHostConfig(config)
      .withWorkingDir("/workspace")
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
    log.info("removing container with id: {} made for user {}", containerId, userId);

    // delete the local directory for the project folder as well
//    projectMountService.


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