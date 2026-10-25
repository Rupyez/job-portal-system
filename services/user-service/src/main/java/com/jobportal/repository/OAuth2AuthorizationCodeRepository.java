package com.jobportal.repository;

import com.jobportal.modal.OAuth2AuthorizationCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OAuth2AuthorizationCodeRepository
        extends JpaRepository<OAuth2AuthorizationCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OAuth2AuthorizationCode c join fetch c.user where c.codeHash = :codeHash")
    Optional<OAuth2AuthorizationCode> findByCodeHashForUpdate(@Param("codeHash") String codeHash);
}