package com.alex788.videos.configuration;

import com.alex788.videos.loading_video_pool.InMemoryVideoLoadingPool;
import com.alex788.videos.loading_video_pool.VideoLoadingPool;
import com.alex788.videos.repository.VideoRepository;
import com.alex788.videos.repository.VideoRepositoryInMemory;
import com.alex788.videos.service.VideoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

    @Bean
    public VideoService videoLoader() {
        return new VideoService(videoLoadingPool(), videoRepository());
    }

    @Bean
    public VideoRepository videoRepository() {
        return new VideoRepositoryInMemory();
    }

    @Bean
    public VideoLoadingPool videoLoadingPool() {
        return new InMemoryVideoLoadingPool();
    }
}
