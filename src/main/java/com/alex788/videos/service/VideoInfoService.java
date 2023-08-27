package com.alex788.videos.service;

import com.alex788.videos.entity.VideoInfo;
import com.alex788.videos.repository.VideoInfoRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class VideoInfoService {

    private final VideoInfoRepository videoInfoRepository;

    public List<VideoInfo> getAllVideoInfos() {
        return videoInfoRepository.findAllVideoInfos();
    }
}