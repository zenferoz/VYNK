package com.vynk.file.repository;

import com.vynk.file.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    List<UploadedFile> findByAssociationTypeAndAssociationId(String associationType, Long associationId);

    Optional<UploadedFile> findByStoredFileName(String storedFileName);
}
