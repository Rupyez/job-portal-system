package com.jobportal.resumeservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage localFileStorage;

    @BeforeEach
    void setUp() {
        localFileStorage = new LocalFileStorage(tempDir.toString());
    }

    @Test
    void shouldStoreFileSuccessfully() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "sample resume content".getBytes()
        );

        String storedFileName = localFileStorage.store(file);

        assertNotNull(storedFileName);
        assertTrue(storedFileName.endsWith("_resume.pdf"));
        assertTrue(Files.exists(tempDir.resolve(storedFileName)));
    }

    @Test
    void shouldGenerateUniqueFileNames() {
        MockMultipartFile firstFile = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "first resume".getBytes()
        );

        MockMultipartFile secondFile = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "second resume".getBytes()
        );

        String firstStoredName = localFileStorage.store(firstFile);
        String secondStoredName = localFileStorage.store(secondFile);

        assertNotEquals(firstStoredName, secondStoredName);
    }

    @Test
    void shouldSanitizeOriginalFileName() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my resume (final).pdf",
                "application/pdf",
                "resume content".getBytes()
        );

        String storedFileName = localFileStorage.store(file);

        assertNotNull(storedFileName);
        assertFalse(storedFileName.contains(" "));
        assertFalse(storedFileName.contains("("));
        assertFalse(storedFileName.contains(")"));
    }

    @Test
    void shouldLoadStoredFileSuccessfully() throws IOException {
        byte[] content = "sample resume content".getBytes();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                content
        );

        String storedFileName = localFileStorage.store(file);

        Resource resource = localFileStorage.load(storedFileName);

        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
        assertArrayEquals(content, resource.getInputStream().readAllBytes());
    }

    @Test
    void shouldDeleteStoredFileSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "resume content".getBytes()
        );

        String storedFileName = localFileStorage.store(file);

        assertTrue(Files.exists(tempDir.resolve(storedFileName)));

        localFileStorage.delete(storedFileName);

        assertFalse(Files.exists(tempDir.resolve(storedFileName)));
    }

    @Test
    void shouldThrowExceptionWhenLoadingMissingFile() {
        assertThrows(
                IllegalStateException.class,
                () -> localFileStorage.load("missing-resume.pdf")
        );
    }
}