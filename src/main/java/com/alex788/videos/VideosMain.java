package com.alex788.videos;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@RequiredArgsConstructor
@SpringBootApplication
public class VideosMain {

    public static void main(String[] args) {
        SpringApplication.run(VideosMain.class, args);
    }
}
