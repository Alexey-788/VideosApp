package com.alex788.videos.configuration;

import com.alex788.videos.adapter.GoogleDriveAdapter;
import com.alex788.videos.adapter.StorageDriveAdapter;
import com.alex788.videos.loading_video_pool.InMemoryVideoLoadingPool;
import com.alex788.videos.loading_video_pool.VideoLoadingPool;
import com.alex788.videos.repository.VideoRepository;
import com.alex788.videos.repository.VideoRepositoryImpl;
import com.alex788.videos.service.VideoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Configuration
public class AppConfiguration {

    @Bean
    public VideoService videoLoader() {
        return new VideoService(videoRepository());
    }

    @Bean
    public VideoRepository videoRepository() {
        return new VideoRepositoryImpl(videoLoadingPool(), storageDriveAdapter());
    }

    @Bean
    public VideoLoadingPool videoLoadingPool() {
        return new InMemoryVideoLoadingPool();
    }

    @Bean
    public StorageDriveAdapter storageDriveAdapter() {
        try {
            return new GoogleDriveAdapter();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
