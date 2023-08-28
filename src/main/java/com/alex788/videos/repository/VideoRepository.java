package com.alex788.videos.repository;

import com.alex788.videos.entity.Video;

import java.util.Optional;
import java.util.UUID;

/**
 * Video repository that displays only loaded videos. Videos that are in the process of being loaded are not returned.
 */
public interface VideoRepository {

    void save(Video video, UUID userId);

    Optional<Video> findByNameAndUser(String videoName, UUID userId);
}
