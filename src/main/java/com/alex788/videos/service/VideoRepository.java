package com.alex788.videos.service;

import com.alex788.videos.entity.Video;

import java.util.Optional;

public interface VideoRepository {

    void save(Video video);

    Optional<Video> findByName(String name);
}
