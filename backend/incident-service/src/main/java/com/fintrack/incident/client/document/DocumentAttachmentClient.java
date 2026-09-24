package com.fintrack.incident.client.document;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "document-attachment-cleanup", url = "${DOCUMENT_SERVICE_URL:http://document-service:8083}")
public interface DocumentAttachmentClient {
  @DeleteMapping("/api/v1/documentService/internal/attachments/cleanup/{incidentId}")
  Boolean cleanup(@PathVariable UUID incidentId, @RequestParam(required = false) UUID commentId,
    @RequestParam boolean onlyIfEmpty, @RequestHeader("X-Internal-Service-Token") String token);
}
