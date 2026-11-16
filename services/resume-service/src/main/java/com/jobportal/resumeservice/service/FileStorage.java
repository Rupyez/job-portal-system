package com.jobportal.resumeservice.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    String store(MultipartFile file);
    Resource load(String storedFileName);
    void delete(String storedFileName);
}