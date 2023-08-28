package com.alex788.videos.loading_video_pool;

import com.alex788.videos.entity.User;
import com.alex788.videos.entity.Video;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.alex788.videos.loading_video_pool.InMemoryVideoLoadingPool.PARALLEL_LOAD_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;

class InMemoryVideoLoadingPoolTest {

    InMemoryVideoLoadingPool inMemoryVideoLoadingPool;

    @BeforeEach
    void beforeEach() {
        inMemoryVideoLoadingPool = new InMemoryVideoLoadingPool();
    }

    @Test
    void canLoadParallelMore_onlyIfYouReachLimit_ReturnsFalse() {
        Video video = new Video(new User().getId(), null);

        for (int i = 0; i < PARALLEL_LOAD_LIMIT; i++) {
            assertThat(inMemoryVideoLoadingPool.canLoadParallelMore(video.userId())).isTrue();
            inMemoryVideoLoadingPool.add(video);
        }

        assertThat(inMemoryVideoLoadingPool.canLoadParallelMore(video.userId())).isFalse();
    }

    @Test
    void doesUserHaveVideoWithName_onlyIfUserHave_ReturnsTrue() {
        Video video = new Video(new User().getId(), "name");

        assertThat(inMemoryVideoLoadingPool.doesUserHaveVideoWithName(video.userId(), video.name())).isFalse();
        inMemoryVideoLoadingPool.add(video);
        assertThat(inMemoryVideoLoadingPool.doesUserHaveVideoWithName(video.userId(), video.name())).isTrue();
    }
}