package com.alex788.videos.loading_video_pool;

import com.alex788.videos.entity.User;
import com.alex788.videos.entity.Video;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static com.alex788.videos.loading_video_pool.InMemoryVideoLoadingPool.PARALLEL_LOAD_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InMemoryVideoLoadingPoolTest {

    InMemoryVideoLoadingPool inMemoryVideoLoadingPool;

    @BeforeEach
    void beforeEach() {
        inMemoryVideoLoadingPool = new InMemoryVideoLoadingPool();
    }

    @Test
    void load_WithCorrectData_AddsThenLoadsThenRemovesVideo() throws ExecutionException, InterruptedException {
        Video video = new Video(new User().getId(), null);
        Consumer<Video> loading = mock(Consumer.class);
        doAnswer(invocation -> {
            List<Video> userVideos = inMemoryVideoLoadingPool.loadingVideosByUserId.get(video.userId());
            assertThat(userVideos).containsOnly(video);
            return null;
        }).when(loading).accept(video);

        Future<?> future = inMemoryVideoLoadingPool.load(video, loading);

        future.get();
        verify(loading, times(1)).accept(video);
        List<Video> userVideos = inMemoryVideoLoadingPool.loadingVideosByUserId.get(video.userId());
        assertThat(userVideos).isNull();
    }

    @Test
    void canLoadParallelMore_onlyIfYouReachLimit_ReturnsFalse() {
        Video video = new Video(new User().getId(), null);
        Consumer<Video> loading = mock(Consumer.class);
        doAnswer(invocation -> {
            Thread.sleep(200);
            return null;
        }).when(loading).accept(video);

        for (int i = 0; i < PARALLEL_LOAD_LIMIT; i++) {
            assertThat(inMemoryVideoLoadingPool.canLoadParallelMore(video.userId())).isTrue();
            inMemoryVideoLoadingPool.load(video, loading);
        }

        assertThat(inMemoryVideoLoadingPool.canLoadParallelMore(video.userId())).isFalse();
    }

    @Test
    void doesUserHaveVideoWithName_onlyIfUserHave_ReturnsTrue() {
        Video video = new Video(new User().getId(), "name");
        Consumer<Video> loading = mock(Consumer.class);
        doAnswer(invocation -> {
            Thread.sleep(200);
            return null;
        }).when(loading).accept(video);

        assertThat(inMemoryVideoLoadingPool.doesUserHaveVideoWithName(video.userId(), video.name())).isFalse();
        inMemoryVideoLoadingPool.load(video, loading);
        assertThat(inMemoryVideoLoadingPool.doesUserHaveVideoWithName(video.userId(), video.name())).isTrue();
    }
}