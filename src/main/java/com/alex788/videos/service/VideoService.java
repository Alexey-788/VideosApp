package com.alex788.videos.service;

import com.alex788.videos.exception.NoVideoByNameException;
import com.alex788.videos.exception.ParallelLoadLimitExceededException;
import com.alex788.videos.entity.Video;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class VideoService {

    public static final int PARALLEL_LOAD_LIMIT = 2;

    private final AtomicInteger parallelLoadCounter = new AtomicInteger(0);
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private final VideoRepository videoRepository;
    
    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    /**
     * @return Empty {@link Future} to keep track of load completion.
     * @throws ParallelLoadLimitExceededException if you try to load more videos in parallel than the limit.
     */
    public Future<?> loadVideoParallel(Video video) throws ParallelLoadLimitExceededException {
        synchronized (this) {
            if (canLoadInParallelMore()) {
                throw new ParallelLoadLimitExceededException();
            }
            parallelLoadCounter.incrementAndGet();
        }

        Runnable loadVideoRunnable = () -> {
            loadVideo(video);
            parallelLoadCounter.decrementAndGet();
        };
        return executorService.submit(loadVideoRunnable);
    }

    public int getHowMuchMoreCanLoadInParallel() {
        return PARALLEL_LOAD_LIMIT - parallelLoadCounter.get();
    }
    
    public boolean canLoadInParallelMore() {
        return parallelLoadCounter.get() >= PARALLEL_LOAD_LIMIT;
    }

    /**
     * Use {@link VideoService#loadVideoParallel} method.
     */
    private void loadVideo(Video video) {
        videoRepository.save(video);
    }

    public Video getVideoByName(String name) {
        return videoRepository.findByName(name).orElseThrow(() -> {
            throw new NoVideoByNameException("No video by name '" + name + "'.");
        });
    }
}
