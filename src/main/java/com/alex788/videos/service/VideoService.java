package com.alex788.videos.service;

import com.alex788.videos.entity.User;
import com.alex788.videos.entity.Video;
import com.alex788.videos.entity.VideoInfo;
import com.alex788.videos.exception.NoVideoByNameException;
import com.alex788.videos.exception.ParallelLoadLimitExceededException;
import com.alex788.videos.exception.VideoWithSameNameAlreadyExistsException;
import com.alex788.videos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RequiredArgsConstructor
public class VideoService {

    public static final int PARALLEL_LOAD_LIMIT = 2;

    private final ExecutorService executorService = Executors.newFixedThreadPool(16);
    private final ConcurrentHashMap<UUID, List<VideoInfo>> loadingVideoInfosByUserId = new ConcurrentHashMap<>();

    private final VideoRepository videoRepository;

    /**
     * @return Empty {@link Future} to keep track of load completion.
     * @throws ParallelLoadLimitExceededException      if you try to load more videos in parallel than the limit.
     * @throws VideoWithSameNameAlreadyExistsException if video with same name already loaded or are loading right now.
     */
    public Future<?> loadVideoParallelOnBehalfOf(Video video, User user) throws ParallelLoadLimitExceededException, VideoWithSameNameAlreadyExistsException {
        synchronized (this) {
            if (!canLoadParallelMore(user.getId())) {
                throw new ParallelLoadLimitExceededException();
            } else if (!isVideoNameUniqueForUser(video.videoInfo().name(), user.getId())) {
                throw new VideoWithSameNameAlreadyExistsException("Video with name '" + video.videoInfo().name() + "' already exists.");
            }

            addUserVideoToLoading(user.getId(), video.videoInfo());
        }

        return executorService.submit(() -> {
            videoRepository.save(video, user.getId());
            synchronized (this) {
                removeUserVideoFromLoading(user.getId(), video.videoInfo());
            }
        });
    }

    private void addUserVideoToLoading(UUID userId, VideoInfo videoInfo) {
        List<VideoInfo> videoInfos = loadingVideoInfosByUserId.computeIfAbsent(userId, id -> new ArrayList<>());
        videoInfos.add(videoInfo);
    }

    private void removeUserVideoFromLoading(UUID userId, VideoInfo videoInfo) {
        List<VideoInfo> videoInfos = loadingVideoInfosByUserId.get(userId);
        videoInfos.remove(videoInfo);
        if (videoInfos.isEmpty()) {
            loadingVideoInfosByUserId.remove(userId);
        }
    }

    private boolean isVideoNameUniqueForUser(String videoName, UUID userId) {
        return isVideoNameUniqueForUserInLoadingPool(videoName, userId)
                && isVideoNameUniqueForUserInRepository(videoName, userId);
    }

    private boolean isVideoNameUniqueForUserInLoadingPool(String videoName, UUID userId) {
        List<VideoInfo> userVideInfos = loadingVideoInfosByUserId.get(userId);
        if (userVideInfos == null) {
            return true;
        }
        return userVideInfos.stream()
                .noneMatch(videoInfo -> videoName.equals(videoInfo.name()));
    }

    private boolean isVideoNameUniqueForUserInRepository(String videoName, UUID userId) {
        return videoRepository.findByNameAndUser(videoName, userId).isEmpty();
    }

    private boolean canLoadParallelMore(UUID userId) {
        return getHowMuchMoreCanLoadParallel(userId) != 0;
    }

    public int getHowMuchMoreCanLoadParallel(UUID userId) {
        return PARALLEL_LOAD_LIMIT - getLoadingCountByUser(userId);
    }

    public int getLoadingCountByUser(UUID userId) {
        if (loadingVideoInfosByUserId.get(userId) == null) {
            return 0;
        }
        return loadingVideoInfosByUserId.get(userId).size();
    }

    public Video getVideoByNameAndUser(String name, UUID userId) {
        return videoRepository.findByNameAndUser(name, userId).orElseThrow(() -> {
            throw new NoVideoByNameException("No video by name '" + name + "'.");
        });
    }
}
