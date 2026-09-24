package com.fintrack.document.scheduler;
import com.fintrack.document.repository.AttachmentStorageCleanupRepository;
import com.fintrack.document.service.AttachmentStorageCleanupService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.*;
import org.springframework.context.annotation.Configuration;
@Configuration @EnableScheduling @RequiredArgsConstructor
public class AttachmentCleanupScheduler {
  private final AttachmentStorageCleanupRepository jobs;
  private final AttachmentStorageCleanupService cleanup;
  @Scheduled(fixedDelayString = "${document.cleanup.interval-ms:60000}")
  public void cleanFiles() {
    for (var task : jobs.findTop50ByRetryAfterBeforeOrderByRetryAfterAsc(LocalDateTime.now())) {
      cleanup.process(task.getId());
    }
  }
}
