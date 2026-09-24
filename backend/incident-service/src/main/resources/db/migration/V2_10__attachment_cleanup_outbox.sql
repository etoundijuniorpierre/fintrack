-- Sans cle etrangere : le nettoyage doit survivre a la suppression de l'incident.
CREATE TABLE attachment_cleanup_tasks (
  id UUID PRIMARY KEY,
  incident_id UUID NOT NULL,
  comment_id UUID,
  retry_after TIMESTAMP NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_attachment_cleanup_retry ON attachment_cleanup_tasks(retry_after);
