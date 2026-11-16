
package com.jobportal.resumeservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(
            @Value("${resume.storage.dir:./uploads/resumes}") String directory) {

        this.root = Path.of(directory)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.root);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to initialize resume storage directory: " + this.root,
                    exception
            );
        }
    }

    @Override
    public String store(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file");
        }

        String originalFilename = file.getOriginalFilename();

        String safeFilename = sanitize(originalFilename);

        String storedFilename = UUID.randomUUID() + "_" + safeFilename;

        Path destination = root.resolve(storedFilename).normalize();

        if (!destination.startsWith(root)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        try (InputStream inputStream = file.getInputStream()) {

            Files.copy(
                    inputStream,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return storedFilename;

        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Failed to store resume file: " + storedFilename,
                    exception
            );
        }
    }

    @Override
    public Resource load(String storedFilename) {

        Path filePath = resolveSafePath(storedFilename);

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException(
                        "Resume file does not exist or is not readable: " + storedFilename
                );
            }

            return resource;

        } catch (MalformedURLException exception) {
            throw new IllegalStateException(
                    "Unable to load resume file: " + storedFilename,
                    exception
            );
        }
    }

    @Override
    public void delete(String storedFilename) {

        Path filePath = resolveSafePath(storedFilename);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Failed to delete resume file: " + storedFilename,
                    exception
            );
        }
    }

    private Path resolveSafePath(String filename) {

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        Path filePath = root.resolve(filename).normalize();

        if (!filePath.startsWith(root)) {
            throw new IllegalArgumentException("Invalid resume filename");
        }

        return filePath;
    }

    private String sanitize(String filename) {

        if (filename == null || filename.isBlank()) {
            return "resume";
        }

        String cleaned = Path.of(filename)
                .getFileName()
                .toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");

        return cleaned.isBlank() ? "resume" : cleaned;
    }
}
