package com.jobportal.resumeservice.service.impl;

import com.jobportal.resumeservice.dto.request.ResumeUpdateRequest;
import com.jobportal.resumeservice.dto.request.ResumeUploadRequest;
import com.jobportal.resumeservice.entity.Resume;
import com.jobportal.resumeservice.exception.ResumeNotFoundException;
import com.jobportal.resumeservice.repository.ResumeRepository;
import com.jobportal.resumeservice.service.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    private Long userId;
    private UUID resumeId;
    private Resume resume;
    private MultipartFile file;

    @BeforeEach
    void setUp() {
        userId = 100L;
        resumeId = UUID.randomUUID();

        resume = mock(Resume.class);

        file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "Sample resume content".getBytes()
        );
    }

    @Test
    void shouldUploadResumeSuccessfully() {
        ResumeUploadRequest request =
                new ResumeUploadRequest("My Resume", "Java Developer", file);

        when(fileStorage.store(file)).thenReturn("stored-resume.pdf");
        when(resumeRepository.save(any(Resume.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = resumeService.upload(userId, request);

        assertNotNull(response);

        verify(fileStorage).store(file);
        verify(resumeRepository).save(any(Resume.class));
    }

    @Test
    void shouldRejectEmptyResumeFile() {
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                new byte[0]
        );

        ResumeUploadRequest request =
                new ResumeUploadRequest("My Resume", "Summary", emptyFile);

        assertThrows(
                IllegalArgumentException.class,
                () -> resumeService.upload(userId, request)
        );

        verifyNoInteractions(fileStorage);
        verifyNoInteractions(resumeRepository);
    }

    @Test
    void shouldDeleteStoredFileWhenUploadFails() {
        ResumeUploadRequest request =
                new ResumeUploadRequest("My Resume", "Summary", file);

        when(fileStorage.store(file)).thenReturn("stored-resume.pdf");
        when(resumeRepository.save(any(Resume.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(
                RuntimeException.class,
                () -> resumeService.upload(userId, request)
        );

        verify(fileStorage).delete("stored-resume.pdf");
    }

    @Test
    void shouldGetResumeOwnedByUser() {
        when(resumeRepository.findByIdAndUserId(resumeId, userId))
                .thenReturn(Optional.of(resume));

        var response = resumeService.get(userId, resumeId);

        assertNotNull(response);

        verify(resumeRepository).findByIdAndUserId(resumeId, userId);
    }

    @Test
    void shouldThrowExceptionWhenResumeDoesNotBelongToUser() {
        when(resumeRepository.findByIdAndUserId(resumeId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResumeNotFoundException.class,
                () -> resumeService.get(userId, resumeId)
        );
    }

    @Test
    void shouldListUserResumes() {
        when(resumeRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(resume));

        var responses = resumeService.list(userId);

        assertNotNull(responses);
        assertEquals(1, responses.size());

        verify(resumeRepository)
                .findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoResumes() {
        when(resumeRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());

        var responses = resumeService.list(userId);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void shouldUpdateResumeMetadata() {
        ResumeUpdateRequest request =
                new ResumeUpdateRequest("Updated Resume", "Updated summary");

        when(resumeRepository.findByIdAndUserId(resumeId, userId))
                .thenReturn(Optional.of(resume));

        var response = resumeService.updateMetaData(userId, resumeId, request);

        assertNotNull(response);

        verify(resume).updateMetaData(
                "Updated Resume",
                "Updated summary"
        );
    }

    @Test
    void shouldReplaceResumeFileSuccessfully() {
        when(resumeRepository.findByIdAndUserId(resumeId, userId))
                .thenReturn(Optional.of(resume));

        when(resume.getStoredFileName()).thenReturn("old-resume.pdf");
        when(fileStorage.store(file)).thenReturn("new-resume.pdf");

        var response = resumeService.replaceFile(userId, resumeId, file);

        assertNotNull(response);

        verify(resume).updateFile(
                "resume.pdf",
                "new-resume.pdf",
                "application/pdf",
                file.getSize()
        );

        verify(fileStorage).delete("old-resume.pdf");
    }

    @Test
    void shouldDeleteNewFileWhenReplacementFails() {
        when(resumeRepository.findByIdAndUserId(resumeId, userId))
                .thenReturn(Optional.of(resume));

        when(resume.getStoredFileName()).thenReturn("old-resume.pdf");
        when(fileStorage.store(file)).thenReturn("new-resume.pdf");

        doThrow(new RuntimeException("Update failed"))
                .when(resume)
                .updateFile(any(), any(), any(), any(Long.class));

        assertThrows(
                RuntimeException.class,
                () -> resumeService.replaceFile(userId, resumeId, file)
        );

        verify(fileStorage).delete("new-resume.pdf");
        verify(fileStorage, never()).delete("old-resume.pdf");
    }

    @Test
    void shouldDeleteResumeAndStoredFile() {
        when(resumeRepository.findByIdAndUserId(resumeId, userId))
                .thenReturn(Optional.of(resume));

        when(resume.getStoredFileName()).thenReturn("stored-resume.pdf");

        assertDoesNotThrow(() -> resumeService.delete(userId, resumeId));

        verify(resumeRepository).delete(resume);
        verify(fileStorage).delete("stored-resume.pdf");
    }

    @Test
    void shouldNotFailWhenFileDeletionFails() {
        when(resumeRepository.findByIdAndUserId(resumeId, userId))
                .thenReturn(Optional.of(resume));

        when(resume.getStoredFileName()).thenReturn("stored-resume.pdf");

        doThrow(new RuntimeException("File deletion failed"))
                .when(fileStorage)
                .delete("stored-resume.pdf");

        assertDoesNotThrow(() -> resumeService.delete(userId, resumeId));

        verify(resumeRepository).delete(resume);
    }

    @Test
    void shouldDownloadResumeOwnedByUser() {
        Resource resource = mock(Resource.class);

        when(resumeRepository.findByIdAndUserId(resumeId, userId))
                .thenReturn(Optional.of(resume));

        when(resume.getStoredFileName()).thenReturn("stored-resume.pdf");
        when(fileStorage.load("stored-resume.pdf")).thenReturn(resource);

        Resource result = resumeService.download(userId, resumeId);

        assertNotNull(result);
        assertSame(resource, result);

        verify(fileStorage).load("stored-resume.pdf");
    }

    @Test
    void shouldNotDownloadResumeOwnedByAnotherUser() {
        when(resumeRepository.findByIdAndUserId(resumeId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResumeNotFoundException.class,
                () -> resumeService.download(userId, resumeId)
        );

        verify(fileStorage, never()).load(any());
    }
}
