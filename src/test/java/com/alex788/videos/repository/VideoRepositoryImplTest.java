package com.alex788.videos.repository;

import com.alex788.videos.adapter.StorageDriveAdapter;
import com.alex788.videos.entity.User;
import com.alex788.videos.entity.Video;
import com.alex788.videos.exception.ParallelLoadLimitExceededException;
import com.alex788.videos.exception.VideoWithSameNameAlreadyExistsException;
import com.alex788.videos.loading_video_pool.VideoLoadingPool;
import org.apache.http.impl.io.EmptyInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VideoRepositoryImplTest {

    VideoLoadingPool videoLoadingPool;
    StorageDriveAdapter storageDriveAdapter;
    VideoRepository videoRepository;

    @BeforeEach
    void beforeEach() {
        videoLoadingPool = mock(VideoLoadingPool.class);
        doReturn(true).when(videoLoadingPool).canLoadParallelMore(any());
        doReturn(false).when(videoLoadingPool).doesUserHaveVideoWithName(any(), any());

        storageDriveAdapter = mock(StorageDriveAdapter.class);
        doReturn(false).when(storageDriveAdapter).hasVideo(any(), any());

        videoRepository = new VideoRepositoryImpl(videoLoadingPool, storageDriveAdapter);
    }

    @Test
    void save_WithCorrectData_Ok() throws ExecutionException, InterruptedException {
        Video video = new Video(new User().getId(), "Video.mp4", null);

        Future<?> future = videoRepository.save(video);

        future.get();
        verify(videoLoadingPool, times(1)).add(video);
        verify(storageDriveAdapter, times(1)).save(any(), any(), any());
        verify(videoLoadingPool, times(1)).remove(video);
    }

    @Test
    void save_VideoLoadingPoolIsFull_ThrowsException() {
        Video video = new Video(new User().getId(), "Video.mp4", null);
        doReturn(false).when(videoLoadingPool).canLoadParallelMore(video.userId());

        Future<?> future = videoRepository.save(video);

        assertThatThrownBy(future::get)
                .hasCauseExactlyInstanceOf(ParallelLoadLimitExceededException.class);
    }

    @Test
    void save_VideoWithSameNameIsLoadingByCurrentUser_ThrowsException() {
        Video video = new Video(new User().getId(), "Video.mp4", null);
        doReturn(true).when(videoLoadingPool).doesUserHaveVideoWithName(any(), any());

        Future<?> future = videoRepository.save(video);

        assertThatThrownBy(future::get)
                .hasCauseExactlyInstanceOf(VideoWithSameNameAlreadyExistsException.class);
    }

    @Test
    void save_VideoWithSameNameAlreadySavedByCurrentUser_ThrowsException() {
        Video video = new Video(new User().getId(), "Video.mp4", null);
        doReturn(true).when(storageDriveAdapter).hasVideo(any(), any(), any());

        Future<?> future = videoRepository.save(video);

        assertThatThrownBy(future::get)
                .hasCauseExactlyInstanceOf(VideoWithSameNameAlreadyExistsException.class);
    }

    @Test
    void findByUserAndName_StorageDriveHasVideo_ReturnsVideoStream() {
        UUID userId = new User().getId();
        String videoName = "Video.mp4";
        doReturn(Optional.of(EmptyInputStream.nullInputStream())).when(storageDriveAdapter).getVideoStream(any(), any(), any());

        Optional<Video> videoOpt = videoRepository.findByUserAndName(userId, videoName);

        assertThat(videoOpt).isPresent();
    }

    @Test
    void findByUserAndName_StorageDriveHasNotVideo_ReturnsNothing() {
        UUID userId = new User().getId();
        String videoName = "Video.mp4";
        doReturn(Optional.empty()).when(storageDriveAdapter).getVideoStream(any(), any(), any());

        Optional<Video> videoOpt = videoRepository.findByUserAndName(userId, videoName);

        assertThat(videoOpt).isEmpty();
    }

    @Test
    void doesUserHaveVideoWithName_ThereIsVideoInStorageDriveOrLoadingPool_True() {
        UUID userId = new User().getId();
        String videoName = "Video.mp4";
        doReturn(true).when(storageDriveAdapter).hasVideo(any(), any(), any());

        assertThat(videoRepository.doesUserHaveVideoWithName(userId, videoName)).isTrue();
    }

    @Test
    void doesUserHaveVideoWithName_ThereIsNoVideoInStorageDriveAndLoadingPool_True() {
        UUID userId = new User().getId();
        String videoName = "Video.mp4";

        assertThat(videoRepository.doesUserHaveVideoWithName(userId, videoName)).isFalse();
    }
}