package com.something.kodex_backend.project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileSyncEngine {
  private static final Path LOCAL_ROOT = Path.of("/tmp/kodex/projects");
  private static final int THREAD_POOL_SIZE = 8;

  private final DriveService driveService;
  private final SyncState syncState;
  private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

  // this function will pull the files inside the project to local folder
  public void pull(
    String accessToken,
    Integer projectId,
    String projectDriveId
  ) throws IOException, InterruptedException, ExecutionException {
    log.info("Pulling for project {}", projectId);

    // fetch all file metadata from drive
    // metadata is fetched first to allow for multithreaded download later on
    List<DriveFile> driveFiles = driveService.listFilesRecursively(accessToken, projectDriveId);

    // create all required directories first
    Path projectRoot = LOCAL_ROOT.resolve(projectId.toString());

    Files.createDirectories(projectRoot);

    for(DriveFile driveFile : driveFiles) {
      Path localPath = projectRoot.resolve(driveFile.relativePath());
      Files.createDirectories(localPath.getParent());
    }

    // download files parallelly
    List<Callable<DriveFile>> tasks = driveFiles.stream()
      .map(driveFile -> (Callable<DriveFile>) () -> {
        Path localPath = projectRoot.resolve(driveFile.relativePath());

        // drive doesn't allow download for empty files
        // so, just create them locally
        if(driveFile.size() == 0L) {
          // use this if FileAlreadyExistsException is thrown
//          Files
//            .newOutputStream(localPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
//            .close();

          Files.createFile(localPath);
        } else {
          driveService.downloadFile(accessToken, driveFile.googleDriveId(), localPath);
        }

        return driveFile;
      })
      .toList();

    List<Future<DriveFile>> futures = executorService.invokeAll(tasks);
    List<String> errors = collectErrors(futures);

    if(!errors.isEmpty()) {
      throw new RuntimeException("Pull failed for some files:\n" + String.join("\n", errors));
    }

    for(Future<DriveFile> future : futures) {
      DriveFile driveFile = future.get();
      syncState.put(projectId, driveFile);
    }

    log.info("Pull finished for project {}: {} files", projectId, driveFiles.size());
  }

  public void push(
    String accessToken,
    Integer projectId,
    String projectDriveId
  ) throws IOException, InterruptedException {
    log.info("Pushing for project {}", projectId);

    Path projectRoot = LOCAL_ROOT.resolve(projectId.toString());

    if(!Files.exists(projectRoot)) {
      log.warn("Local project directory not found, skipping push for {}", projectId);

      return;
    }

    List<Path> localFiles;
    try(Stream<Path> walk = Files.walk(projectRoot)) {
      localFiles = walk.filter(Files::isRegularFile).toList();
    }

    Map<String, DriveFile> snapshot = syncState.getProjectSnapshot(projectId);
    Set<String> localRelativePaths = new HashSet<> ();
    List<UploadTask> toUpload = new ArrayList<> ();

    // find out which files to upload by last modified time and file hash(md5)
    for(Path localFile : localFiles) {
      String relativePath = projectRoot.relativize(localFile).toString();
      localRelativePaths.add(relativePath);

      Optional<DriveFile> existing = Optional.ofNullable(snapshot.get(relativePath));
      if(existing.isEmpty()) {
        // new file upload, don't care about parentDriveId it'll be handled later
        toUpload.add(new UploadTask(localFile, relativePath, null, null));
      } else {
        long localModifiedAt = Files.getLastModifiedTime(localFile).toMillis();
        // if last modified time is less we skip
        if(localModifiedAt <= existing.get().modifiedAt()) {
          continue;
        }

        // if file is modified check hash to decide whether to upload or not
        String localHash = computeMd5(localFile);
        if(localHash.equals(existing.get().md5Checksum())) {
          // hashes matched, just update the last modified time to avoid recompute next cycle
          syncState.put(
            projectId, new DriveFile(
              existing.get().googleDriveId(),
              relativePath,
              existing.get().md5Checksum(),
              localModifiedAt,
              existing.get().size()
            )
          );

          continue;
        }

        toUpload.add(new UploadTask(localFile, relativePath, existing.get().googleDriveId(), null));
      }
    }

    // all the files which were in the map previously but
    // are absent now should be deleted
    List<String> toDelete = snapshot.keySet().stream()
      .filter(path -> !localRelativePaths.contains(path))
      .toList();

    // making new list to avoid messing with toUpload list
    List<UploadTask> resolvedTasks = new ArrayList<> ();
    // create parent folder for newly made files if necessary
    for(UploadTask task : toUpload) {
      if(task.getExistingFileId() == null) {
        String parentDriveId =
          driveService.ensureFolderPath(
            accessToken,
            projectDriveId,
            task.getRelativePath()
          );

        resolvedTasks.add(
          new UploadTask(task.getLocalFile(), task.getRelativePath(), null, parentDriveId)
        );
      } else {
        resolvedTasks.add(
          new UploadTask(task.getLocalFile(), task.getRelativePath(), task.getExistingFileId(), null)
        );
      }
    }

    // parallelly upload and delete files
    List<Callable<Void>> tasks = new ArrayList<> ();
    for(UploadTask task : resolvedTasks) {
      tasks.add(() -> {
        DriveFile result = driveService.uploadFile(
          accessToken, task.getLocalFile(), task.getRelativePath(),
          task.getParentDriveId(), task.getExistingFileId()
        );
        syncState.put(projectId, result);

        return null;
      });
    }

    for(String relativePath : toDelete) {
      String driveFileId = snapshot.get(relativePath).googleDriveId();

      tasks.add(() -> {
        driveService.deleteFile(accessToken, driveFileId);
        syncState.remove(projectId, relativePath);

        return null;
      });
    }

    List<Future<Void>> futures = executorService.invokeAll(tasks);
    List<String> errors = collectErrors(futures);
    if(!errors.isEmpty()) {
      throw new RuntimeException("Push failed for some files:\n" + String.join("\n", errors));
    }

    log.info(
      "Push completed for project {}: {} uploaded, {} deleted",
      projectId, toUpload.size(), toDelete.size()
    );
  }

  public void cleanup(
    String accessToken,
    Integer projectId,
    String projectDriveId
  ) throws IOException, InterruptedException {
    log.info("Cleaning up project {}", projectId);

    push(accessToken, projectId, projectDriveId);
    deleteLocalDirectory(LOCAL_ROOT.resolve(projectId.toString()));
    syncState.clearProject(projectId);

    log.info("Cleanup complete for project {}", projectId);
  }

  private <T> List<String> collectErrors(List<Future<T>> futures) {
    List<String> errors = new ArrayList<> ();
    for(Future<T> future : futures) {
      try {
        future.get();
      } catch(ExecutionException ex) {
        errors.add(ex.getCause().getMessage());
      } catch(InterruptedException ex) {
        Thread.currentThread().interrupt();
        errors.add("Interrupted");
      }
    }

    return errors;
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

  private String computeMd5(Path filePath) throws IOException {
    try(InputStream inputStream = Files.newInputStream(filePath)) {
      return DigestUtils.md5DigestAsHex(inputStream);
    }
  }

}