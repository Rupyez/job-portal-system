package com.jobportal.resumeservice.service;

import com.jobportal.resumeservice.dto.request.ResumeUpdateRequest;
import com.jobportal.resumeservice.dto.request.ResumeUploadRequest;
import com.jobportal.resumeservice.dto.response.ResumeResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ResumeService {

    ResumeResponse upload(Long userId, ResumeUploadRequest request);

    ResumeResponse get(Long userId, UUID resumeId);

    List<ResumeResponse> list(Long userId);

    ResumeResponse updateMetaData(Long userId, UUID resumeId, ResumeUpdateRequest request);
    ResumeResponse replaceFile(Long userId, UUID resumeId, MultipartFile file);
    void delete(Long userId, UUID resumeId);
    Resource download(Long userId, UUID resumeId);
}
