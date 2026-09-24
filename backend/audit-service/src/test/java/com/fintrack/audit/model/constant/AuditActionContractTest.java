package com.fintrack.audit.model.constant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

// Garde-fou de contrat sur les actions d'audit.
class AuditActionContractTest {

  private static final Set<String> EXPECTED_EMITTED_ACTIONS = Set.of(
    "LOGIN_SUCCESS",
    "LOGIN_FAILURE",
    "LOGOUT",
    "USER_CREATE",
    "USER_UPDATE",
    "USER_DELETE",
    "ROLE_CREATE",
    "ROLE_UPDATE",
    "ROLE_DELETE",
    "DEPARTMENT_CREATE",
    "DEPARTMENT_UPDATE",
    "DEPARTMENT_DELETE",
    "DEPARTMENT_ASSIGN_HEAD",
    "AGENCY_CREATE",
    "AGENCY_UPDATE",
    "AGENCY_DELETE",
    "AGENCY_ASSIGN_HEAD",
    "INCIDENT_CREATE",
    "INCIDENT_UPDATE",
    "INCIDENT_DELETE",
    "INCIDENT_VALIDATE",
    "INCIDENT_STATUS_CHANGE",
    "INCIDENT_RESOLVE",
    "INCIDENT_CLOSE",
    "INCIDENT_TRANSFER",
    "REPORT_ACCESS",
    "REPORT_EXPORT",
    "REPORT_DELETE",
    "REPORT_RERUN",
    "AUDIT_EXPORT",
    "CACHE_INVALIDATION",
    "JOB_TRIGGER",
    "ESCALATION_RULE_TOGGLE",
    "ESCALATION_RULE_TRIGGER",
    "AUDIT_PURGE",
    "SETTINGS_CHANGE"
  );

  @Test
  void canonicalEnumCoversEveryEmittedAction() {
    Set<String> canonical = Arrays.stream(AuditAction.values())
      .map(AuditAction::getName)
      .collect(Collectors.toSet());

    Set<String> missing = EXPECTED_EMITTED_ACTIONS.stream()
      .filter(action -> !canonical.contains(action))
      .collect(Collectors.toSet());

    assertThat(missing)
      .as(
        "Actions émises par les services mais absentes de l'enum canonique " +
          "AuditAction (seraient rejetées silencieusement par l'audit-service)"
      )
      .isEmpty();
  }

  @Test
  void everyCanonicalActionHasFrenchAndEnglishLabels() {
    Properties fr = load("/i18n/messages_fr.properties");
    Properties en = load("/i18n/messages_en.properties");

    for (AuditAction action : AuditAction.values()) {
      assertThat(fr.getProperty(action.getNameKey()))
        .as("libellé fr manquant pour " + action.getName())
        .isNotBlank();
      assertThat(fr.getProperty(action.getDescriptionKey()))
        .as("description fr manquante pour " + action.getName())
        .isNotBlank();
      assertThat(en.getProperty(action.getNameKey()))
        .as("libellé en manquant pour " + action.getName())
        .isNotBlank();
      assertThat(en.getProperty(action.getDescriptionKey()))
        .as("description en manquante pour " + action.getName())
        .isNotBlank();
    }
  }

  private Properties load(String path) {
    Properties props = new Properties();
    try (InputStream in = getClass().getResourceAsStream(path)) {
      assertThat(in)
        .as("ressource i18n introuvable: " + path)
        .isNotNull();
      props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return props;
  }
}
