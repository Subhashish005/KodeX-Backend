package com.something.kodex_backend.project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectMountService {

  private final ProjectRepository projectRepository;
  private final FileSyncEngine fileSyncEngine;
  private final SyncScheduler syncScheduler;
  private final ConcurrentHashMap<Integer, String> activeSessions = new ConcurrentHashMap<> ();

  private final static Path LOCAL_ROOT = Path.of("/tmp/kodex/projects");

  public Path openProject(
    String accessToken,
    Integer projectId
  ) throws IOException, ExecutionException, InterruptedException {
    log.info("Opening project {}", projectId);

    Project project = projectRepository.findById(projectId).orElseThrow(
      () -> new RuntimeException("Project with " + projectId + " not Found!")
    );
    String projectDriveId = project.getGoogleDriveId();

    Path localPath = LOCAL_ROOT.resolve(projectId.toString());

    try {
      fileSyncEngine.pull(accessToken, projectId, projectDriveId);
    } catch(Exception ex) {
      log.error("Pull failed for project '{}', cleaning up remaining local files", projectId, ex);
      deleteLocalDirectory(localPath);

      throw ex;
    }

    activeSessions.put(projectId, projectDriveId);
    syncScheduler.startScheduling(accessToken, projectId, projectDriveId);

    log.info("Project '{}' opened at {}", projectId, localPath);

    return localPath;
  }

  public void saveProject(
    String accessToken,
    Integer projectId
  ) throws IOException, InterruptedException {
    log.info("manual save triggered for project '{}'", projectId);

    fileSyncEngine.push(accessToken, projectId, getActiveFolderDriveId(projectId));
  }

  public void closeProject(
    String accessToken,
    Integer projectId
  ) throws IOException, InterruptedException {
    log.info("Closing project '{}'", projectId);

    String projectDriveId = getActiveFolderDriveId(projectId);

    syncScheduler.stopScheduling(projectId);

    try {
      fileSyncEngine.cleanup(accessToken, projectId, projectDriveId);
    } finally {
      // always remove project from active session
      activeSessions.remove(projectId);
    }

    log.info("Project '{}' closed", projectId);
  }

  private String getActiveFolderDriveId(Integer projectId) {
    String projectDriveId = activeSessions.get(projectId);

    if(projectDriveId == null) {
      throw new IllegalStateException("Project is not currently open: " + projectId);
    }

    return projectDriveId;
  }

  private void deleteLocalDirectory(Path path) throws IOException {
    if(!Files.exists(path)) return;

    try {
      FileSystemUtils.deleteRecursively(path);
    } catch(IOException ex) {
      log.error("project deletion failed for path: {}", path, ex);

      throw ex;
    }
  }

}