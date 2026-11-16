
package com.jobportal.resumeservice.controller;

import com.jobportal.resumeservice.dto.request.ResumeUpdateRequest;
import com.jobportal.resumeservice.dto.request.ResumeUploadRequest;
import com.jobportal.resumeservice.dto.response.ResumeResponse;
import com.jobportal.resumeservice.security.AppUserPrincipal;
import com.jobportal.resumeservice.service.ResumeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Validated
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * Upload a new resume.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> upload(
            @AuthenticationPrincipal AppUserPrincipal me,
            @ModelAttribute @Valid ResumeUploadRequest request) {

        ResumeResponse response =
                resumeService.upload(me.userId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get all resumes belonging to the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ResumeResponse>> list(
            @AuthenticationPrincipal AppUserPrincipal me) {

        return ResponseEntity.ok(
                resumeService.list(me.userId())
        );
    }

    /**
     * Get a resume by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> get(
            @AuthenticationPrincipal AppUserPrincipal me,
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                resumeService.get(me.userId(), id)
        );
    }

    /**
     * Update resume metadata.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ResumeResponse> updateMetadata(
            @AuthenticationPrincipal AppUserPrincipal me,
            @PathVariable UUID id,
            @RequestBody @Valid ResumeUpdateRequest request) {

        return ResponseEntity.ok(
                resumeService.updateMetaData(
                        me.userId(),
                        id,
                        request
                )
        );
    }

    /**
     * Replace an existing resume file.
     */
    @PutMapping(
            value = "/{id}/file",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResumeResponse> replaceFile(
            @AuthenticationPrincipal AppUserPrincipal me,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                resumeService.replaceFile(
                        me.userId(),
                        id,
                        file
                )
        );
    }

    /**
     * Download a resume.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal AppUserPrincipal me,
            @PathVariable UUID id) {

        ResumeResponse metadata =
                resumeService.get(me.userId(), id);

        Resource resource =
                resumeService.download(me.userId(), id);

        MediaType contentType = metadata.contentType() != null
                ? MediaType.parseMediaType(metadata.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        ContentDisposition contentDisposition =
                ContentDisposition.attachment()
                        .filename(
                                metadata.originalFileName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(metadata.fileSize())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .body(resource);
    }

    /**
     * Delete a resume.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserPrincipal me,
            @PathVariable UUID id) {

        resumeService.delete(me.userId(), id);

        return ResponseEntity.noContent().build();
    }
}
