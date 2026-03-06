package com.something.kodex_backend.project;

import com.something.kodex_backend.oauth.OAuthenticationUtil;
import com.something.kodex_backend.token.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import kotlin.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectUtil {

  private final OAuthenticationUtil oAuthenticationUtil;
  private final TokenRepository tokenRepository;
  private final ProjectRepository projectRepository;

  public String validateAccessTokenAndGetNewToken(HttpServletRequest request, Integer projectId) {
    validateAccessToken(request);

    // if the token is valid then generate a new token for use
    // this is done so that the access token doesn't expire in middle
    // of processing

    return getAccessTokenForUser(projectId);
  }

  public String getAccessTokenForUser(Integer projectId) {

    Integer userId = projectRepository.findById(projectId).orElseThrow(
      () -> {
        log.error("Project not found with project id {}", projectId);

        return new IllegalArgumentException("Invalid project with id " + projectId);
      }
    ).getUser().getId();

    String refreshToken = tokenRepository
      .findAllOAuthRefreshTokensByUserId(userId)
      .orElseThrow()
      .getFirst()
      .getValue();

    return oAuthenticationUtil.getAccessToken(refreshToken);
  }

  public void validateAccessToken(HttpServletRequest request) {
    String accessToken = extractAccessTokenFromHeader(request);

    if(!oAuthenticationUtil.isAccessTokenValid(accessToken)) {
      log.error("Provided access token '{}' is not valid", accessToken);

      throw new IllegalStateException("Access token is not valid");
    }
  }

  public String extractAccessTokenFromHeader(HttpServletRequest request) {
    String oAuthHeader = request.getHeader("OAuthAccessToken");

    if(oAuthHeader == null || !oAuthHeader.startsWith("Bearer ")) {
      throw new RuntimeException("Missing Expected header or malformed request header!");
    }

    return oAuthHeader.substring(7);
  }

  public void buildProjectStructureJson(
    Path projectRoot,
    Path dir,
    FileType type,
    ProjectFolderStructureResponseDto responseDto
  ) throws IOException {
    responseDto.setType(type);
    responseDto.setName(dir.getFileName().toString());
    responseDto.setId(hash(projectRoot.relativize(dir).toString()));
    responseDto.setChildren(new ArrayList<> ());

    // if this file is a regular file end recursion here
    if(type.equals(FileType.FILE)) return;

    try(Stream<Path> paths = Files.walk(dir, 1)) {
      List<Pair<Path, FileType>> children = paths
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

      for(Pair<Path, FileType> child : children) {
        temp = new ProjectFolderStructureResponseDto();

        buildProjectStructureJson(projectRoot, dir.resolve(child.getFirst()), child.getSecond(), temp);

        responseDto.getChildren().add(temp);
      }
    } catch(IOException ex) {
      log.error("Failed to traverse folder for json response: '{}'", dir, ex);

      throw ex;
    }
  }

  public String hash(String path) {
    return DigestUtils.md5DigestAsHex(path.getBytes(StandardCharsets.UTF_8));
  }

}