package com.alex788.videos.adapter;

import com.alex788.videos.entity.Video;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface StorageDriveAdapter {

    void save(Video video, String... path);

    boolean hasVideo(String videoName, String... path);

    Optional<InputStream> getVideoStream(String videoName, String... path);

    List<String> getAllFileNames(String... path);
}
