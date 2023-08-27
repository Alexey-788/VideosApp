package com.alex788.videos.service;

import com.alex788.videos.entity.VideoInfo;

import java.util.List;

public class VideoInfoService {

    private final VideoInfoRepository videoInfoRepository;

    public VideoInfoService(VideoInfoRepository videoInfoRepository) {
        this.videoInfoRepository = videoInfoRepository;
    }

    public List<VideoInfo> getAllVideoInfos() {
        return videoInfoRepository.findAllVideoInfos();
    }
}