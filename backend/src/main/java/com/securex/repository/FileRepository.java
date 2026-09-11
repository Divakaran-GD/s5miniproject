package com.securex.repository;

import com.securex.entity.FileEntity;
import com.securex.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByUuid(String uuid);
    Optional<FileEntity> findByUuidAndDeletedAtIsNull(String uuid);
    List<FileEntity> findByOwnerAndDeletedAtIsNullOrderByCreatedAtDesc(User owner);
    List<FileEntity> findByDeletedAtIsNullOrderByCreatedAtDesc();
    long countByDeletedAtIsNull();
}
