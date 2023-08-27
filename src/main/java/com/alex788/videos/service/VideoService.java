package com.alex788.videos.service;

import com.alex788.videos.entity.Video;
import com.alex788.videos.entity.VideoInfo;
import com.alex788.videos.exception.NoVideoByNameException;
import com.alex788.videos.exception.ParallelLoadLimitExceededException;
import com.alex788.videos.exception.VideoWithSameNameAlreadyExistsException;
import com.alex788.videos.repository.VideoRepository;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VideoService {

    public static final int PARALLEL_LOAD_LIMIT = 2;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Set<VideoInfo> loadingVideoInfos = ConcurrentHashMap.newKeySet();

    private final VideoRepository videoRepository;

    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    /**
     * @return Empty {@link Future} to keep track of load completion.
     * @throws ParallelLoadLimitExceededException if you try to load more videos in parallel than the limit.
     * @throws VideoWithSameNameAlreadyExistsException if video with same name already loaded or are loading right now.
     */
    public Future<?> loadVideoParallel(Video video) throws ParallelLoadLimitExceededException, VideoWithSameNameAlreadyExistsException {
        synchronized (this) {
            if (canLoadParallelMore()) {
                throw new ParallelLoadLimitExceededException();
            } else if (!isVideoNameUnique(video.videoInfo().name())) {
                throw new VideoWithSameNameAlreadyExistsException("Video with name '" + video.videoInfo().name() + "' already exists.");
            }
            loadingVideoInfos.add(video.videoInfo());
        }

        return executorService.submit(() -> {
            videoRepository.save(video);
            loadingVideoInfos.remove(video.videoInfo());
        });
    }

    private boolean isVideoNameUnique(String videoName) {
        return loadingVideoInfos.stream()
                .noneMatch(info -> videoName.equals(info.name()))
                &&
                videoRepository.findByName(videoName).isEmpty();
    }

    private boolean canLoadParallelMore() {
        return getHowMuchMoreCanLoadInParallel() == 0;
    }

    public int getHowMuchMoreCanLoadInParallel() {
        return PARALLEL_LOAD_LIMIT - loadingVideoInfos.size();
    }

    public Video getVideoByName(String name) {
        return videoRepository.findByName(name).orElseThrow(() -> {
            throw new NoVideoByNameException("No video by name '" + name + "'.");
        });
    }
}
