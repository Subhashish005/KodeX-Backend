package com.something.kodex_backend.project;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProjectController {

  private final ProjectService projectService;

  @GetMapping("/projects")
  public ResponseEntity<List<ProjectResponseDto>> getAllUserProjects(
    HttpServletRequest request
  ) {
    return projectService.getAllUserProjects(request);
  }

  @PostMapping("/projects")
  public ResponseEntity<ProjectResponseDto> createProject(
    @RequestBody ProjectRequestDto projectRequestDto,
    HttpServletRequest request
  ) {
    return projectService.createProject(projectRequestDto, request);
  }

  @DeleteMapping("/projects/{project-id}")
  public ResponseEntity<?> deleteProject(
    @PathVariable("project-id") Integer projectId,
    HttpServletRequest request
  ) {
    return projectService.deleteProject(projectId, request);
  }

  @GetMapping("/projects/{project-id}")
  public ResponseEntity<ProjectFolderStructureResponseDto> getProject(
    @PathVariable("project-id") Integer projectId,
    HttpServletRequest request
  ) {
    return projectService.getProject(projectId, request);
  }

}