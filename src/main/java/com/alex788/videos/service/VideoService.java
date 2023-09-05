package com.alex788.videos.service;

import com.alex788.videos.entity.Video;
import com.alex788.videos.entity.VideoInfo;
import com.alex788.videos.exception.VideoNotFoundException;
import com.alex788.videos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;

    public void save(Video video) {
        videoRepository.save(video);
    }

    public Video getVideo(String videoName, UUID userId) {
        Optional<Video> videoOpt = videoRepository.findByUserAndName(userId, videoName);
        if (videoOpt.isEmpty()) {
            throw new VideoNotFoundException("Video '" + videoName + "' of user with id " + userId + " doesn't exists.");
        }
        return videoOpt.get();
    }

    public boolean doesUserHaveVideoWithName(String videoName, UUID userId) {
        return videoRepository.doesUserHaveVideoWithName(userId, videoName);
    }

    public List<VideoInfo> getAllVideoInfosByUserId(UUID userId) {
        return videoRepository.findInfosByUser(userId);
    }
}
