package com.alex788.videos.service;

import com.alex788.videos.entity.Video;
import com.alex788.videos.entity.VideoInfo;
import com.alex788.videos.exception.ParallelLoadLimitExceededException;
import com.alex788.videos.exception.VideoWithSameNameAlreadyExistsException;
import com.alex788.videos.repository.VideoRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static com.alex788.videos.service.VideoService.PARALLEL_LOAD_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class VideoServiceTest {

    static final long REPOSITORY_SLEEP_MS = 500;

    static VideoService videoService;
    static VideoRepository sleepingOnSaveVideoRepository;
    static VideoRepository immediateVideoRepository;
    static VideoRepository emptySleepingOnSaveVideoRepository;
    static VideoRepository emptyImmediateVideoRepository;

    List<Exception> encounteredExceptions;

    @BeforeEach
    void beforeEach() {
        encounteredExceptions = new ArrayList<>();
    }

    @BeforeAll
    static void beforeAll() {
        sleepingOnSaveVideoRepository = mock(VideoRepository.class);
        doAnswer(invocation -> {
            Thread.sleep(REPOSITORY_SLEEP_MS);
            return null;
        }).when(sleepingOnSaveVideoRepository).save(any());

        immediateVideoRepository = mock(VideoRepository.class);
        doReturn(Optional.of(new Video(new VideoInfo("name"))))
                .when(immediateVideoRepository).findByName(any());

        emptySleepingOnSaveVideoRepository = mock(VideoRepository.class);
        doAnswer(invocation -> {
            Thread.sleep(REPOSITORY_SLEEP_MS);
            return null;
        }).when(emptySleepingOnSaveVideoRepository).save(any());

        emptyImmediateVideoRepository = mock(VideoRepository.class);
    }

    @Test
    void loadVideoParallel_EverythingIsOk_InvokesVideoLoader() throws InterruptedException, ExecutionException {
        videoService = new VideoService(emptyImmediateVideoRepository);
        VideoInfo videoInfo = new VideoInfo("name");
        Video video = new Video(videoInfo);

        Future<?> future = videoService.loadVideoParallel(video);
        future.get();

        verify(emptyImmediateVideoRepository, times(1)).save(video);
    }

    @Test
    @Timeout(value = REPOSITORY_SLEEP_MS + 100, unit = TimeUnit.MILLISECONDS)
    void loadVideoParallel_TryingExceedLimit_ExtraParallelLoadThrowsException() throws InterruptedException {
        videoService = new VideoService(sleepingOnSaveVideoRepository);

        startLoadingsInSameTime(PARALLEL_LOAD_LIMIT + 1);

        assertThat(encounteredExceptions.size()).isOne();
        assertThat(encounteredExceptions.get(0)).isExactlyInstanceOf(ParallelLoadLimitExceededException.class);
    }

    @Test
    @Timeout(value = REPOSITORY_SLEEP_MS + 100, unit = TimeUnit.MILLISECONDS)
    void loadVideoParallel_WhenVideoIsLoaded_VideoFreeUpItsPlaces() throws ExecutionException, InterruptedException {
        videoService = new VideoService(sleepingOnSaveVideoRepository);

        List<Future<?>> futures = startLoadingsInSameTime(PARALLEL_LOAD_LIMIT);

        assertThat(videoService.getHowMuchMoreCanLoadInParallel()).isEqualTo(0);
        for (Future<?> future : futures) {
            future.get();
        }
        assertThat(videoService.getHowMuchMoreCanLoadInParallel()).isEqualTo(PARALLEL_LOAD_LIMIT);
    }

    @Test
    void loadVideoParallel_VideoWithSameNameAlreadyLoading_ThrowsException() {
        videoService = new VideoService(emptySleepingOnSaveVideoRepository);
        String sameName = "SameVideoName.mp4";
        VideoInfo videoInfo1 = new VideoInfo(sameName);
        VideoInfo videoInfo2 = new VideoInfo(sameName);
        Video video1 = new Video(videoInfo1);
        Video video2 = new Video(videoInfo2);

        videoService.loadVideoParallel(video1);
        assertThatThrownBy(() -> videoService.loadVideoParallel(video2))
                .isExactlyInstanceOf(VideoWithSameNameAlreadyExistsException.class);
    }

    @Test
    void loadVideoParallel_VideoWithSameNameAlreadyLoaded_ThrowsException() {
        videoService = new VideoService(immediateVideoRepository);
        VideoInfo videoInfo = new VideoInfo("name");
        Video video = new Video(videoInfo);

        assertThatThrownBy(() -> videoService.loadVideoParallel(video))
                .isExactlyInstanceOf(VideoWithSameNameAlreadyExistsException.class);
    }

    /**
     * Starts parallel loads at the same time using {@link CyclicBarrier}.
     * All encountered exceptions are stored in {@link VideoServiceTest#encounteredExceptions}.
     */
    List<Future<?>> startLoadingsInSameTime(int loadCount) throws InterruptedException {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(loadCount);
        List<Future<?>> futures = new ArrayList<>();

        Runnable runnable = () -> {
            VideoInfo videoInfo = new VideoInfo(UUID.randomUUID().toString());
            Video video = new Video(videoInfo);
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