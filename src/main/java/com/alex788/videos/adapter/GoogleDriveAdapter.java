package com.alex788.videos.adapter;

import com.alex788.videos.entity.Video;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GoogleDriveAdapter implements StorageDriveAdapter { //todo: прописать в сигнатуре throws
    // todo: переделать под InputStreams/OutputStream, избваиться от зависимости на Video. И переименовать методы на более абстрактные(без использования слова видео)

    private static final String APPLICATION_NAME = "VideosApp"; // todo: все конфиги вынести в GDrive конфиг файл
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);

    private final NetHttpTransport httpTransport;
    private final Drive drive;

    public GoogleDriveAdapter() throws GeneralSecurityException, IOException {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        drive = new Drive.Builder(httpTransport, JSON_FACTORY, getCredentials())
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Credential getCredentials() throws IOException {
        InputStream in = GoogleDriveAdapter.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    @Override
    public void save(Video video, String... path) {
        Optional<String> folderIdOpt = getFolderId(path);
        if (folderIdOpt.isEmpty()) {
            return;
        }
        String folderId = folderIdOpt.get();
        File fileMetadata = new File();
        fileMetadata.setParents(List.of(folderId));
        fileMetadata.setName(video.name());
        InputStreamContent content = new InputStreamContent("video/mp4", video.inputStream());
        try {
            drive.files().create(fileMetadata, content).execute();
        } catch (IOException ignored) {
        }
    }

    @Override
    public boolean hasVideo(String videoName, String... path) {
        return getFile(videoName, "video/mp4", path).isPresent();
    }

    @Override
    public Optional<InputStream> getVideoStream(String videoName, String... path) {
        Path downloadingDirectoryPath = Path.of("downloading_videos"); //todo: директория должна быть заточена под пользователя, т.е. должен использоваться параметр path
        try {
            Files.createDirectories(downloadingDirectoryPath);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create video downloading directory.");
        }

        Path downloadingVideoPath = downloadingDirectoryPath.resolve(videoName);

        Optional<File> fileOpt = getFile(videoName, "video/mp4", path);
        if (fileOpt.isEmpty()) {
            return Optional.empty();
        }
        try (OutputStream outputStream = new FileOutputStream(downloadingVideoPath.toFile())) {
            File file = fileOpt.get();
            drive.files().get(file.getId()).executeMediaAndDownloadTo(outputStream);
        } catch (IOException e) {
            return Optional.empty();
        }

        try {
            InputStream inputStream = Files.newInputStream(downloadingVideoPath);
            return Optional.of(inputStream);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<String> getAllFileNames(String... path) {
        return getFiles("video/mp4", path).stream()
                .map(File::getName)
                .toList();
    }

    private Optional<String> getFileId(String fileName, String fileType, String... path) {
        return getFile(fileName, fileType, path).map(File::getId);
    }

    private List<File> getFiles(String filesType, String... path) {
        Optional<String> folderIdOpt = getFolderId(path);
        if (folderIdOpt.isEmpty()) {
            return Collections.emptyList();
        }

        return getFilesInFolder(filesType, folderIdOpt.get());
    }

    private Optional<File> getFile(String fileName, String fileType, String... path) {
        Optional<String> folderIdOpt = getFolderId(path);
        if (folderIdOpt.isEmpty()) {
            return Optional.empty();
        }
        String folderId = folderIdOpt.get();
        return getFileInFolder(fileName, fileType, folderId);
    }

    private Optional<String> getFolderId(String... path) {
        if (path.length == 0) {
            return Optional.of("root");
        }

        Optional<String> parentFolderIdOpt = getRootFolderId(path[0]);
        for (int i = 1; i < path.length; i++) {
            if (parentFolderIdOpt.isEmpty()) {
                return parentFolderIdOpt;
            }
            String parentFolderId = parentFolderIdOpt.get();

            parentFolderIdOpt = getFolderIdInOtherFolder(path[i], parentFolderId);
        }
        return parentFolderIdOpt;
    }

    private Optional<String> getRootFolderId(String folderName) {
        return getFolderIdInOtherFolder(folderName, "root");
    }

    private Optional<String> getFolderIdInOtherFolder(String desireFolderName, String parentFolderId) {
        return getFileIdInFolder(desireFolderName, "application/vnd.google-apps.folder", parentFolderId);
    }

    private Optional<String> getFileIdInFolder(String fileName, String fileType, String folderId) {
        Optional<File> fileOpt = getFileInFolder(fileName, fileType, folderId);
        if (fileOpt.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(fileOpt.get().getId());
        }
    }

    private Optional<File> getFileInFolder(String fileName, String fileType, String folderId) {
        FileList fileList;
        try {
            fileList = drive.files().list()
                    .setPageSize(1)
                    .setFields("nextPageToken, files(id, name)")
                    .setQ("'" + folderId + "' in parents and name = '" + fileName + "' and mimeType = '" + fileType + "'")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<File> files = fileList.getFiles();
        if (files.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(files.get(0));
        }
    }

    private List<File> getFilesInFolder(String fileType, String folderId) {
        FileList fileList;
        try {
            fileList = drive.files().list()
                    .setPageSize(1)
                    .setFields("nextPageToken, files(id, name)")
                    .setQ("'" + folderId + "' in parents and mimeType = '" + fileType + "'")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fileList.getFiles();
    }
}
