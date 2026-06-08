package com.yue.jobcomparer.exception;

public class JobLimitExceededException extends RuntimeException {
    public JobLimitExceededException(String message) {
        super(message);
    }
}
