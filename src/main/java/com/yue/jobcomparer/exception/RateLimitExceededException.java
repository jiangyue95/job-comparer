package com.yue.jobcomparer.exception;

public class RateLimitExceededException extends RuntimeException {
    public  RateLimitExceededException(String message) {
        super(message);
    }
}
