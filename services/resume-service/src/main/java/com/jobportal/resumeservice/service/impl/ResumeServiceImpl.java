package com.jobportal.resumeservice.service.impl;

import com.jobportal.resumeservice.dto.request.ResumeUpdateRequest;
import com.jobportal.resumeservice.dto.request.ResumeUploadRequest;
import com.jobportal.resumeservice.dto.response.ResumeResponse;
import com.jobportal.resumeservice.entity.Resume;
import com.jobportal.resumeservice.exception.ResumeNotFoundException;
import com.jobportal.resumeservice.repository.ResumeRepository;
import com.jobportal.resumeservice.service.FileStorage;
import com.jobportal.resumeservice.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {
    private final ResumeRepository resumeRepository;
    private final FileStorage fileStorage;


    // ------------------------------------------------------------------
    // Upload — called right after Google login, from the home page
    // ------------------------------------------------------------------
    @Override
    @Transactional
    public ResumeResponse upload(Long userId, ResumeUploadRequest request) {
        MultipartFile file = request.file();
        validateFile(file);

        String storedFileName = fileStorage.store(file);


        try{
            Resume resume = new Resume();
            resume.setUserId(userId);
            resume.updateMetaData(request.title(), request.summary());
            resume.updateFile(file.getOriginalFilename(), storedFileName, file.getContentType(), file.getSize());

            Resume saved = resumeRepository.save(resume);
            log.info("User {} uploaded resume {}", userId, saved.getId());
            return ResumeResponse.from(saved);

        }catch (RuntimeException ex){
            fileStorage.delete(storedFileName);
            throw ex;
        }

    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse get(Long userId, UUID resumeId) {

        return ResumeResponse.from(loadOwned(userId, resumeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> list(Long userId) {

        return resumeRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ResumeResponse::from)
                .toList();
    }


    @Override
    @Transactional
    public ResumeResponse updateMetaData(Long userId, UUID resumeId, ResumeUpdateRequest request) {
       Resume resume = loadOwned(userId, resumeId);
       resume.updateMetaData(request.title(), request.summary());

        return ResumeResponse.from(resume);
    }


    @Override
    public ResumeResponse replaceFile(Long userId, UUID resumeId, MultipartFile file) {

        validateFile(file);

        Resume resume = loadOwned(userId, resumeId);
        String oldStoredFileName = resume.getStoredFileName();

        String newStoredFileName = fileStorage.store(file);

        try{
            resume.updateFile(
                    file.getOriginalFilename(),
                    newStoredFileName,
                    file.getContentType(),
                    file.getSize()
            );

            fileStorage.delete(oldStoredFileName);
            log.info("User {} replaced file for resume {}", userId, resumeId);
            return ResumeResponse.from(resume);
        }catch (RuntimeException ex){
            fileStorage.delete(newStoredFileName);
            throw ex;
        }
    }


    @Override
    @Transactional
    public void delete(Long userId, UUID resumeId) {
        Resume resume = loadOwned(userId, resumeId);
        String storedFileName = resume.getStoredFileName();

        resumeRepository.delete(resume);

        try {
            fileStorage.delete(storedFileName);
        }catch (RuntimeException ex){
            log.warn("Orphaned file {} after deleting resume {} for user {}: {}", storedFileName, resumeId, userId, ex.getMessage());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Resource download(Long userId, UUID resumeId) {
        Resume resume = loadOwned(userId, resumeId);
        return fileStorage.load(resume.getStoredFileName());
    }




    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Loads a resume only if it belongs to the given user.
     * Every single operation goes through this, so ownership
     * is enforced in exactly one place.
     */

    private Resume loadOwned(Long userId, UUID resumeId){
        return resumeRepository.findByIdAndUserId(resumeId, userId).orElseThrow(() -> new ResumeNotFoundException(resumeId));
    }

    private void validateFile(MultipartFile file){
        if(file == null || file.isEmpty()){
            throw new IllegalArgumentException("Resume file is Required.");
        }
    }
}
