package com.fintrack.document.repository;
import com.fintrack.document.model.entity.AttachmentUploadReceipt;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AttachmentUploadReceiptRepository extends JpaRepository<AttachmentUploadReceipt, UUID> {}
