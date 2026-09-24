package com.fintrack.document.repository;
import com.fintrack.document.model.entity.AttachmentTombstone;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AttachmentTombstoneRepository extends JpaRepository<AttachmentTombstone, String> {}
