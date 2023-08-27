package com.alex788.videos.exception;

public class VideoWithSameNameAlreadyExistsException extends RuntimeException {

    public VideoWithSameNameAlreadyExistsException(String message) {
        super(message);
    }
}
