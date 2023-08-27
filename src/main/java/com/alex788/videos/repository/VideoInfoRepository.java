package com.alex788.videos.repository;

import com.alex788.videos.entity.VideoInfo;

import java.util.List;

/**
 * Video repository that displays only loaded video infos. Video infos that are in the process of being loaded are not returned.
 */
public interface VideoInfoRepository {

    List<VideoInfo> findAllVideoInfos();
}
