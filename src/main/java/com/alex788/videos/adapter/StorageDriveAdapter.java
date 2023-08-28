package com.alex788.videos.adapter;

import com.alex788.videos.entity.Video;

public interface StorageDriveAdapter {

    void save(Video video, String... path);

    boolean hasVideo(String videoName, String... path);

    Video getVideo(String videoName, String... path);
}
