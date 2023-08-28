package com.alex788.videos.loading_video_pool;

import com.alex788.videos.entity.Video;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class InMemoryVideoLoadingPool implements VideoLoadingPool {

    public static final int PARALLEL_LOAD_LIMIT = 2;

    private final ExecutorService executorService = Executors.newFixedThreadPool(16);

    final Map<UUID, List<Video>> loadingVideosByUserId = new ConcurrentHashMap<>();

    @Override
    public Future<?> load(Video video, Consumer<Video> loading) {
        add(video);
        return executorService.submit(() -> {
            loading.accept(video);
            remove(video);
        });
    }

    private void add(Video video) {
        List<Video> videos = loadingVideosByUserId.computeIfAbsent(video.userId(), id -> new ArrayList<>());
        videos.add(video);
    }

    private void remove(Video video) {
        List<Video> videos = loadingVideosByUserId.get(video.userId());
        videos.remove(video);
        if (videos.isEmpty()) {
            loadingVideosByUserId.remove(video.userId());
        }
    }

    @Override
    public boolean canLoadParallelMore(UUID userId) {
        return getHowMuchMoreCanLoadParallel(userId) != 0;
    }

    private int getHowMuchMoreCanLoadParallel(UUID userId) {
        return PARALLEL_LOAD_LIMIT - getLoadingCountByUser(userId);
    }

    private int getLoadingCountByUser(UUID userId) {
        if (loadingVideosByUserId.get(userId) == null) {
            return 0;
        }
        return loadingVideosByUserId.get(userId).size();
    }

    @Override
    public boolean doesUserHaveVideoWithName(UUID userId, String videoName) {
        List<Video> userVideos = loadingVideosByUserId.get(userId);
        if (userVideos == null) {
            return false;
        }
        return userVideos.stream()
                .anyMatch(video -> videoName.equals(video.name()));
    }
}