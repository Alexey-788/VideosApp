package com.alex788.videos.service;

import com.alex788.videos.entity.Video;
import com.alex788.videos.loading_video_pool.VideoLoadingPool;
import com.alex788.videos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class VideoService {

    private final VideoLoadingPool videoLoadingPool;
    private final VideoRepository videoRepository;

    public void save(Video video) {
        videoRepository.save(video);
    }

    private boolean doesUserHaveVideoWithName(String videoName, UUID userId) {
        return videoLoadingPool.doesUserHaveVideoWithName(userId, videoName)
                || videoRepository.doesUserHaveVideoWithName(userId, videoName);
    }
}
