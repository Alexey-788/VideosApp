package com.alex788.videos.repository;

import com.alex788.videos.entity.Video;
import com.alex788.videos.service.VideoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VideoRepositoryInMemory implements VideoRepository {

    private final List<Video> videos = new ArrayList<>();

    @Override
    public void save(Video video) {
        videos.add(video);
    }

    @Override
    public Optional<Video> findByName(String name) {
        return videos.stream()
                .filter(video -> name.equals(video.name()))
                .findFirst();
    }
}
