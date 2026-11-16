package com.jobportal.resumeservice.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "resumes",
        indexes = {
                @Index(name = "idx_resumes_user_id", columnList = "user_id"),
                @Index(name = "idx_resumes_created_at", columnList = "created_at")
        }
)


@Data
@NoArgsConstructor
public class Resume {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String summary;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, unique = true, length = 100)
    private String storedFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;


    @PrePersist
    protected void onCreate(){
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }


    @PreUpdate
    protected void onUpdate(){
        updatedAt = Instant.now();
    }


    public void updateMetaData(String title, String summary){
        if(title == null || title.isBlank()){
            throw new IllegalArgumentException("Resume title is required.");
        }

        this.title = title.trim();
        this.summary = summary == null ? null : summary.trim();

    }


    public void updateFile(String originalFileName, String storedFileName, String contentType, long fileSize){
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }
}
