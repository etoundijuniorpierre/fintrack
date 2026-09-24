package com.fintrack.document.repository;
import com.fintrack.document.model.entity.AttachmentStorageCleanup;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AttachmentStorageCleanupRepository extends JpaRepository<AttachmentStorageCleanup, UUID> {
  List<AttachmentStorageCleanup> findTop50ByRetryAfterBeforeOrderByRetryAfterAsc(LocalDateTime now);
}
