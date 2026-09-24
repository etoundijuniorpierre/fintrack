CREATE TABLE attachment_upload_receipts (
    id uuid PRIMARY KEY,
    attachment_id uuid NOT NULL,
    fingerprint varchar(64) NOT NULL
);
CREATE TABLE attachment_tombstones (id varchar(100) PRIMARY KEY);
CREATE TABLE attachment_storage_cleanup (
    id uuid PRIMARY KEY,
    object_key varchar(500) NOT NULL,
    retry_after timestamp NOT NULL,
    attempts integer NOT NULL DEFAULT 0
);
CREATE INDEX idx_attachment_cleanup_due ON attachment_storage_cleanup(retry_after);
CREATE INDEX idx_attachment_comment ON attachments_metadata(comment_id);
