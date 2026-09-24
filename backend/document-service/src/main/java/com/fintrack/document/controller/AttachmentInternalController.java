package com.fintrack.document.controller;

import com.fintrack.document.service.AttachmentDeletionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documentService/internal/attachments")
@RequiredArgsConstructor
public class AttachmentInternalController {
  private final AttachmentDeletionService deletion;
  @DeleteMapping("/cleanup/{incidentId}")
  @PreAuthorize("hasAuthority('ATTACHMENT_CLEANUP_INTERNAL')")
  public boolean cleanup(@PathVariable UUID incidentId, @RequestParam(required = false) UUID commentId,
      @RequestParam(defaultValue = "false") boolean onlyIfEmpty) {
    return deletion.deleteScope(incidentId, commentId, onlyIfEmpty);
  }
}
