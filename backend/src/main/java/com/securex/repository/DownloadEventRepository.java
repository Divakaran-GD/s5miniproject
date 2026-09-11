package com.securex.repository;

import com.securex.entity.DownloadEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DownloadEventRepository extends JpaRepository<DownloadEvent, Long> {
    List<DownloadEvent> findByShareLinkIdOrderByAccessedAtDesc(Long shareLinkId);
    long countBySuccessTrue();
}
