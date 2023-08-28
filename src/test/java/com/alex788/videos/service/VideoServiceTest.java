package com.alex788.videos.service;

import com.alex788.videos.entity.User;
import com.alex788.videos.entity.Video;
import com.alex788.videos.entity.VideoInfo;
import com.alex788.videos.exception.ParallelLoadLimitExceededException;
import com.alex788.videos.exception.VideoWithSameNameAlreadyExistsException;
import com.alex788.videos.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.*;

import static com.alex788.videos.service.VideoService.PARALLEL_LOAD_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoServiceTest {

    static final long REPOSITORY_SLEEP_MS = 500;

    static VideoService videoService;
    static TestVideoRepository videoRepository;

    List<Exception> encounteredExceptions;

    @BeforeEach
    void beforeEach() {
        videoRepository = new TestVideoRepository();
        videoService = new VideoService(videoRepository);
        encounteredExceptions = new ArrayList<>();
    }

    @Test
    @Timeout(value = REPOSITORY_SLEEP_MS + 100, unit = TimeUnit.MILLISECONDS)
    void loadVideoParallel_EverythingIsOk_InvokesRepository() throws InterruptedException, ExecutionException {
        Video video = videoWithName("ShortVideo.mp4");
        User user = new User();

        Future<?> future = videoService.loadVideoParallelOnBehalfOf(video, user);
        future.get();

        Entry<UUID, Video> expectedUserIdVideoNameEntry = new SimpleEntry<>(user.getId(), video);
        assertThat(videoRepository.store).containsOnly(expectedUserIdVideoNameEntry);
    }

    @Test
    @Timeout(value = REPOSITORY_SLEEP_MS + 100, unit = TimeUnit.MILLISECONDS)
    void loadVideoParallel_TryingExceedLimit_ExtraParallelLoadThrowsException() throws InterruptedException, ExecutionException {
        User user = new User();

        List<Future<?>> futures = startLoadingsInSameTime(PARALLEL_LOAD_LIMIT + 1, user);
        for (Future<?> future : futures) {
            future.get();
        }

        assertThat(encounteredExceptions).hasSize(1);
        assertThat(encounteredExceptions.get(0)).isExactlyInstanceOf(ParallelLoadLimitExceededException.class);
    }

    @Test
    @Timeout(value = REPOSITORY_SLEEP_MS + 100, unit = TimeUnit.MILLISECONDS)
    void loadVideoParallel_WhenVideoIsLoaded_VideoFreeUpItsPlaces() throws ExecutionException, InterruptedException {
        User user = new User();

        List<Future<?>> futures = startLoadingsInSameTime(PARALLEL_LOAD_LIMIT, user);

        assertThat(encounteredExceptions).isEmpty();
        assertThat(videoService.getHowMuchMoreCanLoadParallel(user.getId())).isEqualTo(0);
        for (Future<?> future : futures) {
            future.get();
        }
        assertThat(videoService.getHowMuchMoreCanLoadParallel(user.getId())).isEqualTo(PARALLEL_LOAD_LIMIT);
    }

    @Test
    @Timeout(value = REPOSITORY_SLEEP_MS + 100, unit = TimeUnit.MILLISECONDS)
    void loadVideoParallel_DifferentUsersReachLimit_Ok() throws ExecutionException, InterruptedException {
        User user1 = new User();
        User user2 = new User();

        List<Future<?>> futures1 = startLoadingsInSameTime(PARALLEL_LOAD_LIMIT, user1);
        List<Future<?>> futures2 = startLoadingsInSameTime(PARALLEL_LOAD_LIMIT, user2);

        assertThat(encounteredExceptions).isEmpty();
        for (Future<?> future : futures1) {
            future.get();
        }
        for (Future<?> future : futures2) {
            future.get();
        }
    }

    @Test
    @Timeout(value = REPOSITORY_SLEEP_MS + 100, unit = TimeUnit.MILLISECONDS)
    void loadVideoParallel_VideoWithSameNameByDifferentUserIsLoading_Ok() throws ExecutionException, InterruptedException {
        String sameName = "LongVideo.mp4";
        User user1 = new User();
        User user2 = new User();
        Video video1 = videoWithName(sameName);
        Video video2 = videoWithName(sameName);

        Future<?> future1 = videoService.loadVideoParallelOnBehalfOf(video1, user1);
        Future<?> future2 = videoService.loadVideoParallelOnBehalfOf(video2, user2);

        assertThat(encounteredExceptions).isEmpty();
        future1.get();
        future2.get();
    }

    @Test
    @Timeout(value = REPOSITORY_SLEEP_MS + 100, unit = TimeUnit.MILLISECONDS)
    void loadVideoParallel_VideoWithSameNameByDifferentUserAlreadyLoaded_Ok() throws ExecutionException, InterruptedException {
        String sameName = "ShortVideo.mp4";
        User user1 = new User();
        User user2 = new User();
        Video video1 = videoWithName(sameName);
        Video video2 = videoWithName(sameName);

        Future<?> future1 = videoService.loadVideoParallelOnBehalfOf(video1, user1);
        future1.get();
        Future<?> future2 = videoService.loadVideoParallelOnBehalfOf(video2, user2);
        future2.get();

        assertThat(encounteredExceptions).isEmpty();
    }

    @Test
    @Timeout(value = REPOSITORY_SLEEP_MS + 100, unit = TimeUnit.MILLISECONDS)
    void loadVideoParallel_VideoWithSameNameAlreadyLoading_ThrowsException() throws ExecutionException, InterruptedException {
        String sameName = "LongVideo.mp4";
        Video video1 = videoWithName(sameName);
        Video video2 = videoWithName(sameName);
        User user = new User();

        Future<?> future = videoService.loadVideoParallelOnBehalfOf(video1, user);
        assertThatThrownBy(() -> videoService.loadVideoParallelOnBehalfOf(video2, user))
                .isExactlyInstanceOf(VideoWithSameNameAlreadyExistsException.class);
        future.get();
    }

    @Test
    void loadVideoParallel_VideoWithSameNameAlreadyLoaded_ThrowsException() throws ExecutionException, InterruptedException {
        String sameName = "LongVideo.mp4";
        Video video1 = videoWithName(sameName);
        Video video2 = videoWithName(sameName);
        User user = new User();

        Future<?> future = videoService.loadVideoParallelOnBehalfOf(video1, user);
        future.get();
        assertThatThrownBy(() -> videoService.loadVideoParallelOnBehalfOf(video2, user))
                .isExactlyInstanceOf(VideoWithSameNameAlreadyExistsException.class);
    }

    Video videoWithName(String name) {
        return new Video(
                new VideoInfo(name)
        );
    }

    /**
     * Starts parallel loads at the same time using {@link CyclicBarrier}.
     * Encountered exceptions are stored in {@link VideoServiceTest#encounteredExceptions}.
     */
    List<Future<?>> startLoadingsInSameTime(int loadCount, User user) throws InterruptedException {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(loadCount);
        List<Future<?>> futures = new ArrayList<>();

        Runnable runnable = () -> {
            VideoInfo videoInfo = new VideoInfo("LongVideo" + Thread.currentThread().getName());
            Video video = new Video(videoInfo);
            try {
                cyclicBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }

            Future<?> future;
            try {
                future = videoService.loadVideoParallelOnBehalfOf(video, user);
            } catch (Exception e) {
                encounteredExceptions.add(e);
                System.err.println("Error: " + e.getClass().getName());
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

    static class TestVideoRepository implements VideoRepository {
        List<Entry<UUID, Video>> store = new CopyOnWriteArrayList<>();

        @Override
        public void save(Video video, UUID userId) {
            String videoName = video.videoInfo().name();
            if (videoName.startsWith("LongVideo")) {
                sleep();
                store.add(new SimpleEntry<>(userId, video));
            } else if (videoName.startsWith("ShortVideo")) {
                store.add(new SimpleEntry<>(userId, video));
            } else {
                throw new RuntimeException("Video name must starts with 'LongVideo' or 'ShortVideo'.");
            }
        }

        @Override
        public Optional<Video> findByNameAndUser(String videoName, UUID userId) {
            return store.stream()
                    .filter(entry -> {
                        String entryVideoName = entry.getValue().videoInfo().name();
                        UUID entryUserId = entry.getKey();
                        return entryVideoName.equals(videoName) && entryUserId.equals(userId);
                    })
                    .findFirst().map(Entry::getValue);
        }

        private void sleep() {
            try {
                Thread.sleep(REPOSITORY_SLEEP_MS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}