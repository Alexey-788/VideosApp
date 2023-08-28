package com.alex788.videos.loading_video_pool;

import com.alex788.videos.entity.Video;

import java.util.UUID;

public interface VideoLoadingPool {

    void add(Video video);

    void remove(Video video);

    boolean canLoadParallelMore(UUID userId);

    boolean doesUserHaveVideoWithName(UUID userId, String videoName);
}
