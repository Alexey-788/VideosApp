package com.alex788.videos.loading_video_pool;

import com.alex788.videos.entity.Video;

import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface VideoLoadingPool {

    Future<?> load(Video video, Consumer<Video> loading);

    boolean canLoadParallelMore(UUID userId);

    boolean doesUserHaveVideoWithName(UUID userId, String videoName);
}
