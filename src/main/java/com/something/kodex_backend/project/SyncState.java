package com.something.kodex_backend.project;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SyncState {

  // (projectId, (relativePath, DriveFile))
  private final ConcurrentHashMap<Integer , ConcurrentHashMap<String, DriveFile>> state = new ConcurrentHashMap<> ();

  public Optional<DriveFile> get(Integer projectId, String relativePath) {
    ConcurrentHashMap<String, DriveFile> projectState = state.get(projectId);

    if(projectState == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(projectState.get(relativePath));
  }

  public void put(Integer projectId, DriveFile driveFile) {
    state.computeIfAbsent(projectId, k -> new ConcurrentHashMap<> ())
      .put(driveFile.relativePath(), driveFile);
  }

  public void remove(Integer projectId, String relativePath) {
    ConcurrentHashMap<String, DriveFile> projectState = state.get(projectId);

    if(projectState != null) projectState.remove(relativePath);
  }

  public ConcurrentHashMap<String, DriveFile> getProjectSnapshot(Integer projectId) {
    ConcurrentHashMap<String, DriveFile> projectState = state.get(projectId);

    if(projectState == null) return new ConcurrentHashMap<> ();

    // send a copy to avoid concurrent modification
    return new ConcurrentHashMap<> (projectState);
  }

  public void clearProject(Integer projectId) {
    state.remove(projectId);
  }

}