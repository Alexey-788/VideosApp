package com.alex788.videos.exception;

public class NoVideoByNameException extends RuntimeException {

    public NoVideoByNameException(String message) {
        super(message);
    }
}
