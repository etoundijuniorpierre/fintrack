package com.fintrack.document.service;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// Verrou PostgreSQL partage entre replicas, libere avec la transaction.
@Component @RequiredArgsConstructor
public class AttachmentScopeLock {
  private final JdbcTemplate jdbc;
  public void lock(String key) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException("Une transaction est requise pour verrouiller les pieces jointes");
    }
    long value = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).getMostSignificantBits();
    jdbc.query("select pg_advisory_xact_lock(?)", rs -> {}, value);
  }
}
