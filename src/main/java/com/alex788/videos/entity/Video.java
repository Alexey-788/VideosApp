package com.alex788.videos.entity;

import java.io.InputStream;
import java.util.UUID;

public record Video(UUID userId, String name, InputStream inputStream) {

    private static final String VIDEO_EXTENSION = ".mp4";

    public Video {
        if (!name.endsWith(VIDEO_EXTENSION) || name.length() <= VIDEO_EXTENSION.length()) {
            throw new RuntimeException("The video extension must be .mp4 and the name must not be empty.");
        }
    }
}
