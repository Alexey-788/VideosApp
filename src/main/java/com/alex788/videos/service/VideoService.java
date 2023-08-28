package com.alex788.videos.service;

import com.alex788.videos.entity.Video;
import com.alex788.videos.exception.ParallelLoadLimitExceededException;
import com.alex788.videos.exception.VideoWithSameNameAlreadyExistsException;
import com.alex788.videos.loading_video_pool.VideoLoadingPool;
import com.alex788.videos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.util.concurrent.Future;

@RequiredArgsConstructor
public class VideoService {

    private final VideoLoadingPool videoLoadingPool;
    private final VideoRepository videoRepository;

    /**
     * @return Empty {@link Future} to keep track of load completion.
     * @throws ParallelLoadLimitExceededException      if you try to load more videos in parallel than the limit.
     * @throws VideoWithSameNameAlreadyExistsException if video with same name already loaded or are loading right now.
     */
    public Future<?> loadParallel(Video video) throws ParallelLoadLimitExceededException, VideoWithSameNameAlreadyExistsException {
        synchronized (this) {
            if (!videoLoadingPool.canLoadParallelMore(video.userId())) {
                throw new ParallelLoadLimitExceededException();
            } else if (doesUserHaveVideoWithName(video.name(), video.userId())) {
                throw new VideoWithSameNameAlreadyExistsException("Video with name '" + video.name() + "' already exists.");
            }

            return videoLoadingPool.load(video, videoRepository::save);
        }
    }

    private boolean doesUserHaveVideoWithName(String videoName, UUID userId) {
        return videoLoadingPool.doesUserHaveVideoWithName(userId, videoName)
                || videoRepository.doesUserHaveVideoWithName(userId, videoName);
    }
}
