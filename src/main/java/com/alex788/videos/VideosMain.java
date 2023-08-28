package com.alex788.videos;

import com.alex788.videos.entity.User;
import com.alex788.videos.entity.Video;
import com.alex788.videos.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@RequiredArgsConstructor
@SpringBootApplication
public class VideosMain implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(VideosMain.class, args);
    }

    private final VideoService videoService;

    @Override
    public void run(String... args) {
        videoService.save(new Video(new User().getId(), "NameOfVideo.mp4"));
    }
}
