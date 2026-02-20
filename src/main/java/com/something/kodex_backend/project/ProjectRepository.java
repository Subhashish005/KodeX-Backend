package com.something.kodex_backend.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

  @Query( value = """
    SELECT EXISTS(SELECT 1 FROM t_project WHERE name=:name) AS project_exists
""", nativeQuery = true
  )
  Optional<Boolean> checkIfProjectExistsByName(String name);

  @Query( value = """
    SELECT EXISTS(SELECT 1 FROM t_project WHERE id=:id) AS project_exists
""", nativeQuery = true
  )
  Optional<Boolean> checkIfProjectExistsById(Integer id);

  void deleteByName(String name);

  Optional<Project> findByName(String projectName);
}