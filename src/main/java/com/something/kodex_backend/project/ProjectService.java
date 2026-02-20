package com.something.kodex_backend.project;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.something.kodex_backend.error.DuplicateProjectException;
import com.something.kodex_backend.error.UserNotFoundException;
import com.something.kodex_backend.user.User;
import com.something.kodex_backend.user.UserRepository;
import com.something.kodex_backend.utils.ModelMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectUtil projectUtil;
  private final ModelMapper modelMapper;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;

  private static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
  private static final String APP_NAME = "KodeX";

  public ResponseEntity<List<ProjectResponseDto>> getAllUserProjects(HttpServletRequest request) {
    // assume the token is valid
    // if not the frontend is supposed to refresh it and ask again

    String accessToken = projectUtil.getAccessTokenFromRequestHeader(request);

    String rootId = projectUtil.findAppRootFolderOrCreate(accessToken);

    Drive drive = projectUtil.buildDrive(accessToken);

    String query = String.format(
      " mimeType='%s'" +
      " and '%s' in parents" +
      " and appProperties has { key='type' and value='project' }" +
      " and appProperties has { key='createdBy' and value='%s' }" +
      " and trashed=false", MIME_TYPE_FOLDER ,rootId, APP_NAME);

    List<ProjectResponseDto> projectResponseDtoList = new ArrayList<> ();

    try {
      String pageToken = null;
      FileList driveProjects;
      boolean isProjectPresent;

      do {
        // google only return first 100 results by default
        // use page token to get all results
        driveProjects = drive.files()
          .list()
          .setQ(query)
          .setFields("nextPageToken, files(name, modifiedTime)")
          .setPageToken(pageToken)
          .execute();

        for(File driveProject : driveProjects.getFiles()) {
          isProjectPresent = projectRepository.checkIfProjectExistsByName(driveProject.getName())
            .orElseThrow(
              () -> new RuntimeException("Expected a not null value got a null value instead!")
            );

          String projectName = driveProject.getName();

          // if the driveProject is not present on drive then remove it from db
          if(!isProjectPresent) {
            projectRepository.deleteByName(projectName);

          } else {
            Project project = projectRepository.findByName(projectName).orElseThrow();

            projectResponseDtoList.add(modelMapper.toProjectResponseDto(project));
          }
        }

        // update the current page token
        pageToken = driveProjects.getNextPageToken();
      } while(pageToken != null);

      return ResponseEntity.ok().body(projectResponseDtoList);
    } catch(IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public ResponseEntity<ProjectResponseDto> createProject(
    ProjectRequestDto projectRequestDto,
    HttpServletRequest request
  ) {
    // TODO: fix - multiple projects with same name can be made due to concurrency

    User user = userRepository.findById(projectRequestDto.getUserId()).orElseThrow(
      () -> new UserNotFoundException("User with user id: " + projectRequestDto.getUserId() + " not found!")
    );

    // check if project with same name exists, if yes don't process the request
    boolean isProjectPresent = projectRepository.checkIfProjectExistsByName(projectRequestDto.getProjectName())
      .orElseThrow(
        () -> new RuntimeException("Expected a not null value got a null value instead!")
      );

    if(isProjectPresent) {
      throw new DuplicateProjectException("A project exists with same name!");
    }

    String accessToken = projectUtil.getAccessTokenFromRequestHeader(request);

    // making api call to google each time to do anything related to root folder
    // this is bad, gonna fix sometime
    String rootId = projectUtil.findAppRootFolderOrCreate(accessToken);

    Drive drive = projectUtil.buildDrive(accessToken);

    File metadata = new File();
    metadata.setMimeType(MIME_TYPE_FOLDER);
    metadata.setName(projectRequestDto.getProjectName());
    metadata.setParents(List.of(rootId));
    metadata.setAppProperties(Map.of("type", "project", "createdBy", APP_NAME));

    try {
      File driveProject = drive.files()
        .create(metadata)
        .setFields("id, name, createdTime, modifiedTime")
        .execute();

      Project project = Project.builder()
        .name(driveProject.getName())
        .googleDriveId(driveProject.getId())
        .createdAt(Instant.ofEpochMilli(driveProject.getCreatedTime().getValue()))
        .modifiedAt(Instant.ofEpochMilli(driveProject.getModifiedTime().getValue()))
        .language(projectRequestDto.getProjectLanguage())
        .user(user)
        .build();

      projectRepository.save(project);

      return ResponseEntity.ok().body(modelMapper.toProjectResponseDto(project));
    } catch(IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public ResponseEntity<?> deleteProject(Integer projectId, HttpServletRequest request) {
    boolean isProjectPresent = projectRepository.checkIfProjectExistsById(projectId)
      .orElseThrow(
        () -> new RuntimeException("Expected a not null value got a null value instead!")
      );

    if(!isProjectPresent) {
      throw new RuntimeException("Project with project id " + projectId + " does not exists!");
    }

    Project project = projectRepository.findById(projectId).orElseThrow();

    String accessToken = projectUtil.getAccessTokenFromRequestHeader(request);

    Drive drive = projectUtil.buildDrive(accessToken);

    try {
      File metadata = new File();
      metadata.setTrashed(true);

      drive.files()
        .update(project.getGoogleDriveId(), metadata)
        .execute();

      projectRepository.deleteById(projectId);

      return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
    } catch(IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}