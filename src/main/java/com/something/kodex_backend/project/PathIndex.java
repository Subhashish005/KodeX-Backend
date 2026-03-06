package com.something.kodex_backend.project;

import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PathIndex {

  // (projectId, (folderHash, relativePath))
  private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, String>> index =
    new ConcurrentHashMap<> ();

  public void put(Integer projectId, String relativePath) {
    index.computeIfAbsent(projectId, k -> new ConcurrentHashMap<> ())
      .put(hash(relativePath), relativePath);
  }

  public Optional<String> getPath(Integer projectId, String folderHash) {
    ConcurrentHashMap<String, String> projectIndex = index.get(projectId);

    if(projectIndex == null) return Optional.empty();

    return Optional.ofNullable(projectIndex.get(folderHash));
  }

  // this function needs to delete the folder and all of its subfolders too
  public void remove(Integer projectId, String folderHash) {
    ConcurrentHashMap<String, String> projectIndex = index.get(projectId);
    if(projectIndex == null) return;

    String relativePath = projectIndex.get(folderHash);
    if(relativePath == null) return;

    String prefix = relativePath + "/";
    projectIndex.values()
      .removeIf(path -> path.equals(relativePath) || path.startsWith(prefix));
  }

  public void clearProject(Integer projectId) {
    index.remove(projectId);
  }

  private String hash(String path) {
    return DigestUtils.md5DigestAsHex(path.getBytes(StandardCharsets.UTF_8));
  }

}