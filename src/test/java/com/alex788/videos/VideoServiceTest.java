package com.alex788.videos;

import com.alex788.videos.entity.Video;
import com.alex788.videos.exception.ParallelLoadLimitExceededException;
import com.alex788.videos.service.VideoRepository;
import com.alex788.videos.service.VideoService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.alex788.videos.service.VideoService.PARALLEL_LOAD_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VideoServiceTest {

    static final long REPOSITORY_SLEEP_MS = 500;

    static VideoService videoService;
    static VideoRepository sleepingVideoRepository;
    static VideoRepository immediateVideoRepository;

    List<Exception> encounteredExceptions;

    @BeforeEach
    void beforeEach() {
        encounteredExceptions = new ArrayList<>();
    }

    @BeforeAll
    static void beforeAll() {
        sleepingVideoRepository = mock(VideoRepository.class);
        doAnswer(invocation -> {
            Thread.sleep(REPOSITORY_SLEEP_MS);
            return null;
        }).when(sleepingVideoRepository).save(any());

        immediateVideoRepository = mock(VideoRepository.class);
    }

    @Test
    void loadVideoParallel_EverythingIsOk_InvokesVideoLoader() {
        videoService = new VideoService(immediateVideoRepository);
        Video video = new Video("");

        videoService.loadVideoParallel(video);

        verify(immediateVideoRepository, times(1)).save(video);
    }

    @Test
    @Timeout(value = REPOSITORY_SLEEP_MS + 100, unit = TimeUnit.MILLISECONDS)
    void loadVideoParallel_TryingExceedLimit_ExtraParallelLoadThrowsException() throws InterruptedException {
        videoService = new VideoService(sleepingVideoRepository);

        startLoadingsInSameTime(PARALLEL_LOAD_LIMIT + 1);

        assertThat(encounteredExceptions.size()).isOne();
        assertThat(encounteredExceptions.get(0)).isExactlyInstanceOf(ParallelLoadLimitExceededException.class);
    }

    @Test
    @Timeout(value = REPOSITORY_SLEEP_MS + 100, unit = TimeUnit.MILLISECONDS)
    void loadVideoParallel_WhenVideoIsLoaded_VideoFreeUpItsPlaces() throws ExecutionException, InterruptedException {
        videoService = new VideoService(sleepingVideoRepository);

        List<Future<?>> futures = startLoadingsInSameTime(PARALLEL_LOAD_LIMIT);

        assertThat(videoService.getHowMuchMoreCanLoadInParallel()).isZero();
        for (Future<?> future : futures) {
            future.get();
        }
        assertThat(videoService.getHowMuchMoreCanLoadInParallel()).isEqualTo(PARALLEL_LOAD_LIMIT);
    }

    /**
     * Starts parallel loads at the same time using {@link CyclicBarrier}.
     * All encountered exceptions are stored in {@link VideoServiceTest#encounteredExceptions}.
     */
    List<Future<?>> startLoadingsInSameTime(int loadCount) throws InterruptedException {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(loadCount);
        List<Future<?>> futures = new ArrayList<>();

        Runnable runnable = () -> {
            Video video = new Video("");
            try {
                cyclicBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }

            Future<?> future;
            try {
                future = videoService.loadVideoParallel(video);
            } catch (Exception e) {
                encounteredExceptions.add(e);
                return;
            }
            futures.add(future);
        };

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < loadCount; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
            threads.add(thread);
        }
        for (Thread thread : threads) {
            thread.join();
        }
        return futures;
    }
}