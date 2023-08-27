package com.alex788.videos.configuration;

import com.alex788.videos.repository.VideoRepository;
import com.alex788.videos.repository.VideoRepositoryInMemory;
import com.alex788.videos.service.VideoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

    @Bean
    public VideoService videoLoader() {
        return new VideoService(videoRepository());
    }

    @Bean
    public VideoRepository videoRepository() {
        return new VideoRepositoryInMemory();
    }
}
