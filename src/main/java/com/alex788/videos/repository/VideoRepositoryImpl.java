package com.alex788.videos.repository;

import com.alex788.videos.adapter.StorageDriveAdapter;
import com.alex788.videos.entity.Video;
import com.alex788.videos.exception.ParallelLoadLimitExceededException;
import com.alex788.videos.exception.VideoWithSameNameAlreadyExistsException;
import com.alex788.videos.loading_video_pool.VideoLoadingPool;
import com.antkorwin.xsync.XSync;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RequiredArgsConstructor
public class VideoRepositoryImpl implements VideoRepository {

    private final XSync<UUID> xSync = new XSync<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(16);

    private final VideoLoadingPool videoLoadingPool;
    private final StorageDriveAdapter storageDriveAdapter;

    @Override
    public Future<?> save(Video video) {
        return executorService.submit(() -> {
            xSync.execute(video.userId(), () -> {
                if (!videoLoadingPool.canLoadParallelMore(video.userId())) {
                    throw new ParallelLoadLimitExceededException("User with id '" + video.userId() + "' already has video with name '" + video.name() + "'.");
                } else if (doesUserHaveVideoWithName(video.userId(), video.name())) {
                    throw new VideoWithSameNameAlreadyExistsException("Video with name '" + video.name() + "' already exists.");
                }

                videoLoadingPool.add(video);
            });

            storageDriveAdapter.save(video, video.userId().toString());

            xSync.execute(video.userId(), () ->
                    videoLoadingPool.remove(video)
            );
        });
    }

    @Override
    public Optional<Video> findByUserAndName(UUID userId, String videoName) {
        return Optional.ofNullable(storageDriveAdapter.getVideo(videoName, userId.toString()));
    }

    @Override
    public boolean doesUserHaveVideoWithName(UUID userId, String videoName) {
        return videoLoadingPool.doesUserHaveVideoWithName(userId, videoName)
                || storageDriveAdapter.hasVideo(videoName, userId.toString());
    }
}
