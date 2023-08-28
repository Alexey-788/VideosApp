package com.alex788.videos.adapter;

import com.alex788.videos.entity.Video;

public class GoogleDriveAdapter implements StorageDriveAdapter {

    @Override
    public void save(Video video, String... path) {

    }

    @Override
    public boolean hasVideo(String videoName, String... path) {
        return false;
    }

    @Override
    public Video getVideo(String videoName, String... path) {
        return null;
    }
}
