package com.something.kodex_backend.project;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.something.kodex_backend.config.OAuthConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProjectUtil {

  private final OAuthConfig oAuthConfig;

  public String getAccessTokenFromRequestHeader(HttpServletRequest request) {
    String requestHeader = request.getHeader("OAuthAccessToken");

    if(requestHeader == null || !requestHeader.startsWith("Bearer ")) {
      throw new RuntimeException("Missing Expected header or malformed request header!");
    }

    // "Bearer " is 7 char long
    return requestHeader.substring(7);
  }

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

  public String findAppRootFolderOrCreate(String accessToken) {
    Drive drive = buildDrive(accessToken);

    // look for a particular app property that root folder is supposed have
    String query = "mimeType='application/vnd.google-apps.folder' " +
      "and appProperties has { key='type' and value='kodex_root' } " +
      "and appProperties has { key='createdBy' and value='KodeX' } " +
      "and trashed=false";

    try {
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
      metadata.setAppProperties(Map.of("type", "kodex_root", "createdBy", "KodeX"));

      File rootFolder = drive.files()
        .create(metadata)
        .setFields("id")
        .execute();

      return rootFolder.getId();
    } catch(IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}