package com.fintrack.incident.scheduler;

import com.fintrack.incident.repository.AttachmentCleanupTaskRepository;
import com.fintrack.incident.service.AttachmentCleanupService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class AttachmentCleanupScheduler {
  private final AttachmentCleanupTaskRepository tasks;
  private final AttachmentCleanupService cleanup;
  @Scheduled(fixedDelayString = "${incident.attachment-cleanup.interval-ms:60000}")
  public void run() {
    for (var task : tasks.findTop50ByRetryAfterBeforeOrderByRetryAfterAsc(LocalDateTime.now())) {
      cleanup.process(task.getId());
    }
  }
}
