package com.something.kodex_backend.project;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
// TODO: create an interceptor or filter for this controller
// to check if the project id belongs to the that user or
// a different authenticated user can delete/create for other users
public class ProjectController {

  private final ProjectService projectService;

  @GetMapping("")
  public ResponseEntity<List<ProjectResponseDto>> getAllUserProjects(
    HttpServletRequest request
  ) {
    return projectService.getAllUserProjects(request);
  }

  @PostMapping("")
  public ResponseEntity<ProjectResponseDto> createProject(
    @RequestBody ProjectRequestDto projectRequestDto,
    HttpServletRequest request
  ) {
    return projectService.createProject(projectRequestDto, request);
  }

  @DeleteMapping("/{project-id}")
  public ResponseEntity<?> deleteProject(
    @PathVariable("project-id") Integer projectId,
    HttpServletRequest request
  ) {
    return projectService.deleteProject(projectId, request);
  }

  @GetMapping("/{project-id}")
  public ResponseEntity<ProjectFolderStructureResponseDto> getProject(
    @PathVariable("project-id") Integer projectId,
    HttpServletRequest request
  ) {
    return projectService.getProject(projectId, request);
  }

  @PostMapping("/{project-id}/folders")
  public ResponseEntity<Map<String, String>> createFolder(
    @PathVariable("project-id") Integer projectId,
    @RequestBody ProjectChildrenRequestDto requestDto
  ) {
    return projectService.createFolder(projectId, requestDto);
  }

  @PostMapping("/{project-id}/files")
  public ResponseEntity<Map<String, String>> createFile(
    @PathVariable("project-id") Integer projectId,
    @RequestBody ProjectChildrenRequestDto requestDto
  ) {
    return projectService.createFile(projectId, requestDto);
  }

  @DeleteMapping("/{project-id}/folders/{folder-name}")
  public ResponseEntity<Void> deleteFolder(
    @PathVariable("project-id") Integer projectId,
    @PathVariable("folder-name") String folderName,
    @RequestParam("parent_id") String parentHash
  ) {
    return projectService.deleteFolder(projectId, parentHash, folderName);
  }

  @DeleteMapping("/{project-id}/files/{file-name}")
  public ResponseEntity<Void> deleteFile(
    @PathVariable("project-id") Integer projectId,
    @PathVariable("file-name") String fileName,
    @RequestParam("parent_id") String parentHash
  ) {
    return projectService.deleteFile(projectId, parentHash, fileName);
  }

  @GetMapping("/{project-id}/files/{file-name}")
  public ResponseEntity<String> getFileData(
    @PathVariable("project-id") Integer projectId,
    @PathVariable("file-name") String fileName,
    @RequestParam("parent_id") String parentHash
  ) {
    return projectService.getFileData(projectId, parentHash, fileName);
  }

  @PutMapping("/{project-id}/files/{file-name}")
  public ResponseEntity<Void> putFileData(
    @PathVariable("project-id") Integer projectId,
    @PathVariable("file-name") String fileName,
    @RequestParam("parent_id") String parentHash,
    @RequestBody String content
  ) {
    return projectService.putFileData(projectId, parentHash, fileName, content);
  }

}