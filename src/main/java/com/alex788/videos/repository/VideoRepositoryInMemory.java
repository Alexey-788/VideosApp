package com.alex788.videos.repository;

import com.alex788.videos.entity.Video;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @deprecated does not work correctly with users.
 */
@Deprecated
public class VideoRepositoryInMemory implements VideoRepository {

    private final List<Video> videos = new ArrayList<>();

    @Override
    public void save(Video video) {
        videos.add(video);
    }

    @Override
    public Optional<Video> findByUserAndName(UUID userId, String videoName) {
        return videos.stream()
                .filter(video -> videoName.equals(video.name()))
                .findFirst();
    }

    @Override
    public boolean doesUserHaveVideoWithName(UUID userId, String videoName) {
        return videos.stream().anyMatch(video -> videoName.equals(video.name()));
    }
}
