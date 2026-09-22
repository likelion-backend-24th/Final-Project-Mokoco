package com.team2.postservice.post.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    record StoredFile(String imageUrl, String storedFileName) {}

    StoredFile store(MultipartFile file);

    void delete(String storedFileName);
}