package com.something.kodex_backend.project;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.something.kodex_backend.config.OAuthConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DriveService {

  private final OAuthConfig oAuthConfig;

  private static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
  private final static String APP_NAME = "KodeX";

  public Drive buildDrive(String accessToken) {
    GoogleCredentials googleCredentials = GoogleCredentials.create(new AccessToken(accessToken, null));

    try {
      return new Drive.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        new HttpCredentialsAdapter(googleCredentials)
      )
      .setApplicationName(oAuthConfig.getApplicationName())
      .build();
    } catch(GeneralSecurityException | IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public String findAppRootFolderOrCreate(String accessToken) throws IOException {
    Drive drive = buildDrive(accessToken);

    // look for a particular app property that root folder is supposed have
    String query = "mimeType='application/vnd.google-apps.folder' " +
      "and appProperties has { key='type' and value='kodex_root' } " +
      "and appProperties has { key='createdBy' and value='KodeX' } " +
      "and trashed=false";


    FileList result = drive.files()
      .list()
      .setQ(query)
      .setFields("files(id)")
      .execute();

    // assume the first match is our root folder
    // if multiple exists then its user's fault :)
    if(!result.getFiles().isEmpty()) {
      return result.getFiles().getFirst().getId();
    }

    // if no root folder is found then make a new one
    File metadata = new File();
    metadata.setName("KodeX_cloud_IDE_root");
    metadata.setMimeType("application/vnd.google-apps.folder");

    // this is very important, as for all the future queries for root
    // will look for this property
    metadata.setAppProperties(Map.of("type", "kodex_root", "createdBy", APP_NAME));

    File rootFolder = drive.files()
      .create(metadata)
      .setFields("id")
      .execute();

    return rootFolder.getId();
  }

  // TODO: refactor create project method here

  public List<DriveFile> listFilesRecursively(
    String accessToken,
    String googleDriveId
  ) throws IOException {
    Drive drive = buildDrive(accessToken);

    List<DriveFile> result = new ArrayList<> ();

    collectFiles(drive, googleDriveId, "", result);

    return result;
  }

  public DriveFile uploadFile(
    String accessToken,
    Path localFile,
    String relativePath,
    String parentFolderId,
    String googleFileId
  ) throws IOException {
    Drive drive = buildDrive(accessToken);
    String mimeType = Files.probeContentType(localFile);

    // if mime type is not explicit then just consider
    // it as a raw stream of bits
    if(mimeType == null) {
      mimeType = "application/octet-stream";
    }

    FileContent fileContent = new FileContent(mimeType, localFile.toFile());
    File metadata = new File();
    File response;

    if(googleFileId == null) {
      // if a file doesn't have a file id then it's a new file
      // parent folders exists in drive just create this file

      metadata.setName(localFile.getFileName().toString());
      metadata.setParents(List.of(parentFolderId));
      metadata.setAppProperties(Map.of("createdBy", APP_NAME));

      response = drive.files()
        .create(metadata, fileContent)
        .setFields("id, modifiedTime, md5Checksum")
        .execute();
    } else {
      // if file id is present, then it's an existing file
      // just update the content
      response = drive.files()
        .update(googleFileId, metadata, fileContent)
        .setFields("id, modifiedTime, md5Checksum, size")
        .execute();
    }

    return new DriveFile(
      response.getId(),
      relativePath,
      response.getMd5Checksum(),
      response.getModifiedTime().getValue(),
      response.getSize() != null ? response.getSize() : 0L
    );
  }

  public void deleteFile(
    String accessToken,
    String googleFileId
  ) throws IOException {
    File metadata = new File();
    metadata.setTrashed(true);

    Drive drive = buildDrive(accessToken);
    drive.files()
      .update(googleFileId, metadata)
      .execute();
  }

  public void downloadFile(
    String accessToken,
    String fileId,
    Path destinationPath
  ) throws IOException {
    Drive drive = buildDrive(accessToken);

    Files.createDirectories(destinationPath.getParent());

    try (OutputStream outputStream = new FileOutputStream(destinationPath.toFile())) {
      drive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
    }
  }

  public String ensureFolderPath(
    String accessToken,
    String rootFolderId,
    String relativePath
  ) throws IOException {
    Drive drive = buildDrive(accessToken);
    String[] parts = relativePath.split("/");
    String currentParentId = rootFolderId;

    // the last part is the file itself hence skip it
    for(int i = 0; i < parts.length - 1; i++) {
      currentParentId = getOrCreateSubfolder(drive, currentParentId, parts[i]);
    }

    return currentParentId;
  }

  private String getOrCreateSubfolder(Drive drive, String parentId, String name) throws IOException {
    String query = String.format(
      " '%s' in parents" +
      " and name='%s'" +
      " and mimeType='%s'" +
      " and appProperties has { key='createdBy' and value='%s' }" +
      " and trashed=false", parentId, name, MIME_TYPE_FOLDER, APP_NAME);

    FileList fileList = drive.files()
      .list()
      .setQ(query)
      .setFields("files(id)")
      .execute();

    if(!fileList.getFiles().isEmpty()) {
      return fileList.getFiles().getFirst().getId();
    }

    File folder = new File();
    folder.setName(name);
    folder.setParents(List.of(parentId));
    folder.setMimeType(MIME_TYPE_FOLDER);
    folder.setAppProperties(Map.of("createdBy", APP_NAME));

    return drive.files()
      .create(folder)
      .setFields("id")
      .execute()
      .getId();
  }

  private void collectFiles(
    Drive drive,
    String googleDriveId,
    String currentPath,
    List<DriveFile> result
  ) throws IOException {
    String query = String.format(
      " '%s' in parents" +
      " and appProperties has { key='createdBy' and value='%s' }" +
      " and trashed=false", googleDriveId, APP_NAME);

    FileList fileList = drive.files()
      .list()
      .setQ(query)
      .setFields("files(id, name, mimeType, modifiedTime, md5Checksum, size)")
      .execute();

    for(File file : fileList.getFiles()) {
      String relativePath = currentPath.isEmpty() ? file.getName() : currentPath + "/" + file.getName();

      if(MIME_TYPE_FOLDER.equals(file.getMimeType())) {
        collectFiles(drive, file.getId(), relativePath, result);
      } else {
        result.add(new DriveFile(
          file.getId(),
          relativePath,
          file.getMd5Checksum(),
          file.getModifiedTime().getValue(),
          file.getSize() != null ? file.getSize() : 0L
        ));
      }
    }
  }

}