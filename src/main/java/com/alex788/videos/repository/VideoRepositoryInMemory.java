package com.alex788.videos.repository;

import com.alex788.videos.entity.Video;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VideoRepositoryInMemory implements VideoRepository {

    private final List<Video> videos = new ArrayList<>();

    @Override
    public void save(Video video, UUID userId) {
        videos.add(video);
    }

    @Override
    public Optional<Video> findByNameAndUser(String videoName, UUID userId) {
        return videos.stream()
                .filter(video -> videoName.equals(video.videoInfo().name()))
                .findFirst();
    }
}
