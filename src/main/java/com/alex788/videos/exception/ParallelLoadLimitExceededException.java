package com.alex788.videos.exception;

public class ParallelLoadLimitExceededException extends RuntimeException {

    public ParallelLoadLimitExceededException(String message) {
        super(message);
    }
}
