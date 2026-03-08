package com.something.kodex_backend.project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectMountService {

  private final ProjectRepository projectRepository;
  private final FileSyncEngine fileSyncEngine;
  private final SyncScheduler syncScheduler;
  private final ConcurrentHashMap<Integer, String> activeSessions = new ConcurrentHashMap<> ();
  private final PathIndex pathIndex;
  private final ProjectUtil projectUtil;

  private final static Path LOCAL_ROOT = Path.of("/tmp/kodex/projects");

  public void openProject(
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
    syncScheduler.startScheduling(projectId, projectDriveId);

    log.info("Project '{}' opened at {}", projectId, localPath);
  }

  public void saveProject(
    Integer projectId
  ) throws IOException, InterruptedException {
    log.info("manual save triggered for project '{}'", projectId);

    fileSyncEngine.push(projectId, getActiveFolderDriveId(projectId));
  }

  public void closeProject(
    Integer projectId
  ) throws IOException, InterruptedException {
    log.info("Closing project '{}'", projectId);

    String projectDriveId = getActiveFolderDriveId(projectId);

    syncScheduler.stopScheduling(projectId);

    try {
      fileSyncEngine.cleanup(projectId, projectDriveId);
    } finally {
      // always remove project from active session
      activeSessions.remove(projectId);
    }

    log.info("Project '{}' closed", projectId);
  }

  public String createFolder(
    Integer projectId,
    String parentHash,
    String folderName
  ) throws IOException {
    // first get parent path using project id and parent's hash
    Path parentPath = resolvePath(projectId, parentHash);
    // now get the absolute path for this folder
    Path newFolder = parentPath.resolve(folderName);

    Files.createDirectories(newFolder);

    // change permissions for newly created folders too
    Set<PosixFilePermission> permissions =
      PosixFilePermissions.fromString("rwxrwxrwx");

    Files.setPosixFilePermissions(newFolder, permissions);

    // update pathIndex
    String relativePath = LOCAL_ROOT.resolve(projectId.toString())
      .relativize(newFolder).toString();

    pathIndex.put(projectId, relativePath);

    // return hash for this folder
    return projectUtil.hash(relativePath);
  }

  // this method can possibly be refactored to be combined with createFolder
  public String createFile(
    Integer projectId,
    String parentHash,
    String fileName
  ) throws IOException {
    Path parentPath = resolvePath(projectId, parentHash);
    Path newFile = parentPath.resolve(fileName);

    String relativePath = LOCAL_ROOT.resolve(projectId.toString())
        .relativize(newFile).toString();

    Files.createFile(newFile);

    Set<PosixFilePermission> permissions =
      PosixFilePermissions.fromString("rwxrwxrwx");

    Files.setPosixFilePermissions(newFile, permissions);

    return projectUtil.hash(relativePath);
  }

  public void deleteFolder(
    Integer projectId,
    String parentHash,
    String folderName
  ) throws IOException {
    Path parentPath = resolvePath(projectId, parentHash);
    Path target = parentPath.resolve(folderName);

    FileSystemUtils.deleteRecursively(target);

    pathIndex.remove(projectId, parentHash);
  }

  public void deleteFile(
    Integer projectId,
    String parentHash,
    String fileName
  ) throws IOException {
    Path parentPath = resolvePath(projectId, parentHash);

    Files.delete(parentPath.resolve(fileName));
  }

  public Path resolvePath(Integer projectId, String parentHash) {
    if(parentHash == null || parentHash.equals("null")) return LOCAL_ROOT.resolve(projectId.toString());

    String relativePath = pathIndex.getPath(projectId, parentHash).orElseThrow(
      () -> new IllegalArgumentException("Unknown path hash : " + parentHash)
    );

    return LOCAL_ROOT.resolve(projectId.toString()).resolve(relativePath);
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