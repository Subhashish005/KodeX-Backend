package com.something.kodex_backend.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UploadTask {

  private Path localFile;
  private String relativePath;
  private String existingFileId;
  private String parentDriveId;

}