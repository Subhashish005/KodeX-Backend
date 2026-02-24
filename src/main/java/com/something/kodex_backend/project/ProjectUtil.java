package com.something.kodex_backend.project;

import jakarta.servlet.http.HttpServletRequest;
import kotlin.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class ProjectUtil {

  public String getAccessTokenFromRequestHeader(HttpServletRequest request) {
    String requestHeader = request.getHeader("OAuthAccessToken");

    if(requestHeader == null || !requestHeader.startsWith("Bearer ")) {
      throw new RuntimeException("Missing Expected header or malformed request header!");
    }

    // "Bearer " is 7 char long
    return requestHeader.substring(7);
  }

  public void buildProjectStructureJson(
    Path dir,
    FileType type,
    ProjectFolderStructureResponseDto responseDto
  ) throws IOException {
    responseDto.setType(type);
    responseDto.setName(dir.getFileName().toString());
    responseDto.setRelativePath(dir.toString());
    responseDto.setContent(new ArrayList<> ());

    // if this file is a regular file end recursion here
    if(type.equals(FileType.FILE)) return;

    try(Stream<Path> paths = Files.walk(dir, 1)) {
      List<Pair<Path, FileType>> contentList = paths
        .filter(path -> !path.equals(dir))
        .map(path ->
          new Pair<>(path.getFileName(), Files.isDirectory(path) ? FileType.FOLDER : FileType.FILE)
        )
        .sorted((a, b) -> {
          if(a.getSecond().equals(b.getSecond())) {
            return a.getFirst().compareTo(b.getFirst());
          }

          return a.getSecond().equals(FileType.FOLDER) ? -1 : 1;
        })
        .toList();

      ProjectFolderStructureResponseDto temp;

      for(Pair<Path, FileType> content : contentList) {
        temp = new ProjectFolderStructureResponseDto();

        buildProjectStructureJson(dir.resolve(content.getFirst()), content.getSecond(), temp);

        responseDto.getContent().add(temp);
      }
    } catch(IOException ex) {
      log.error("Failed to traverse folder for json response: '{}'", dir, ex);

      throw ex;
    }

  }
}