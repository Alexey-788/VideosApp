package com.alex788.videos.repository;

import com.alex788.videos.entity.Video;
import com.alex788.videos.entity.VideoInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Video repository that displays loaded videos and videos currently loading.
 */
public interface VideoRepository {

    Future<?> save(Video video);

    Optional<Video> findByUserAndName(UUID userId, String videoName);

    boolean doesUserHaveVideoWithName(UUID userId, String videoName);

    List<VideoInfo> findInfosByUser(UUID userId);
}
