package com.something.kodex_backend.project;

public record DriveFile(
  String googleDriveId,
  String relativePath,
  String md5Checksum,
  long modifiedAt,
  long size
) {
}