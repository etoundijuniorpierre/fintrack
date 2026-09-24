package com.fintrack.incident.repository;

import com.fintrack.incident.model.entity.AttachmentCleanupTask;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AttachmentCleanupTaskRepository extends JpaRepository<AttachmentCleanupTask, UUID> {
  List<AttachmentCleanupTask> findTop50ByRetryAfterBeforeOrderByRetryAfterAsc(LocalDateTime now);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from AttachmentCleanupTask t where t.id = :id")
  Optional<AttachmentCleanupTask> findLocked(@Param("id") UUID id);
}
