package com.securex.repository;

import com.securex.entity.FileEntity;
import com.securex.entity.ShareLink;
import com.securex.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByUuid(String uuid);
    Optional<ShareLink> findByTokenHash(String tokenHash);
    List<ShareLink> findByOwnerOrderByCreatedAtDesc(User owner);
    List<ShareLink> findByFileOrderByCreatedAtDesc(FileEntity file);
    List<ShareLink> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE ShareLink s SET s.downloadCount = s.downloadCount + 1, s.lastAccessedAt = :now WHERE s.id = :id AND s.downloadCount < s.maxDownloads")
    int incrementDownloadCountSafely(@Param("id") Long id, @Param("now") LocalDateTime now);

    long countByRevokedFalseAndExpiresAtAfter(LocalDateTime now);
    long countByExpiresAtBeforeOrRevokedTrue(LocalDateTime now);
}
