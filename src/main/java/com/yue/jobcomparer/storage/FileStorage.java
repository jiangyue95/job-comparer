package com.yue.jobcomparer.storage;

public interface FileStorage {

    String upload(byte[] content, String originalFilename, String contentType);

    String generatePresignedUrl(String key);

    void delete(String key);
}
