package com.alex788.videos.service;

import com.alex788.videos.entity.VideoInfo;

import java.util.List;

public interface VideoInfoRepository {

    List<VideoInfo> findAllVideoInfos();
}
