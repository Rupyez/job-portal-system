package com.jobportal.resumeservice.dto.mapper;

import com.jobportal.resumeservice.dto.response.ResumeResponse;
import com.jobportal.resumeservice.entity.Resume;

import java.util.List;

public class ResumeMapper {

    public ResumeResponse toResponse(Resume resume){
        return new ResumeResponse(
                resume.getId(),
                resume.getUserId(),
                resume.getTitle(),
                resume.getSummary(),
                resume.getOriginalFileName(),
                resume.getContentType(),
                resume.getFileSize(),
                resume.getCreatedAt(),
                resume.getUpdatedAt(),
                resume.getVersion()
        );
    }


    public List<ResumeResponse> toResponseList(List<Resume> resumes){
        return resumes.stream().map(this::toResponse).toList();
    }
}
