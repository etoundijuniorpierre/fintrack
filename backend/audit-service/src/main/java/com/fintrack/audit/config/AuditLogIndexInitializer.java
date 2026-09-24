// Configuration Spring : declare les regles techniques liees a audit log index initializer.

package com.fintrack.audit.config;

import com.fintrack.audit.model.entity.AuditLog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

// Cree au demarrage les index MongoDB de la collection audit_logs (performances
// des requetes), sans echouer si un index equivalent existe deja.
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogIndexInitializer implements ApplicationRunner {

  private final MongoTemplate mongoTemplate;

  // Demarre ou initialise le traitement applicatif attendu.

  @Override
  public void run(ApplicationArguments args) {
    IndexOperations indexOps = mongoTemplate.indexOps(AuditLog.class);
    List<IndexInfo> existing = indexOps.getIndexInfo();

    // Les noms de champs ci-dessous correspondent aux champs du document MongoDB (voir @Field sur AuditLog).
    ensureSingleFieldIndex(
      indexOps,
      existing,
      "timestamp",
      Sort.Direction.DESC
    );
    ensureSingleFieldIndex(indexOps, existing, "user_id", Sort.Direction.ASC);
    ensureSingleFieldIndex(indexOps, existing, "action", Sort.Direction.ASC);
    ensureSingleFieldIndex(
      indexOps,
      existing,
      "resource_type",
      Sort.Direction.ASC
    );
    ensureSingleFieldIndex(indexOps, existing, "status", Sort.Direction.ASC);
  }

  // Cree un index mono-champ seulement si aucun index existant ne couvre deja
  // ce champ (comparaison par cle, pas par nom).
  @SuppressWarnings("removal")
  private void ensureSingleFieldIndex(
    IndexOperations indexOps,
    List<IndexInfo> existing,
    String field,
    Sort.Direction direction
  ) {
    boolean alreadyIndexed = existing
      .stream()
      .anyMatch(
        info ->
          info.getIndexFields().size() == 1 &&
          field.equals(info.getIndexFields().get(0).getKey())
      );

    if (alreadyIndexed) {
      log.debug(
        "Index on 'audit_logs.{}' existe déjà — création ignorée",
        field
      );
      return;
    }

    indexOps.ensureIndex(new Index().on(field, direction).named(field));
    log.info("Index créé on 'audit_logs.{}'", field);
  }
}
