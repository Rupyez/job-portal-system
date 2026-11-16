package com.jobportal.resumeservice.repository;

import com.jobportal.resumeservice.entity.Resume;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    List<Resume> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Resume> findByIdAndUserId(UUID id, Long userId);
    boolean existsByUserId(Long userId);
    boolean existsByIdAndUserId(UUID id, Long userId);
    long deleteByIdAndUserId(UUID id, Long userId);
    long countByUserId(Long userId);



    // ---- Null-safe overrides of JpaRepository methods ----

    @Override
    @NonNull
    Optional<Resume> findById(@NonNull UUID id);

    @Override
    @NonNull
    <S extends Resume> S save(@NonNull S entity);

    @Override
    void deleteById(@NonNull UUID id);

}
