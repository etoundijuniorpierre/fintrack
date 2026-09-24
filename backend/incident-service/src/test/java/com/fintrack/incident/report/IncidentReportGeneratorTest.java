// Tests : valide le contenu, la langue et la chronologie de la fiche PDF d'un incident.

package com.fintrack.incident.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentCause;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.dto.response.IncidentCommentResponse;
import com.fintrack.incident.model.dto.response.IncidentHistoryResponse;
import com.fintrack.incident.model.dto.response.IncidentResponse;
import com.fintrack.incident.model.dto.response.IncidentTypeConfigResponse;
import com.fintrack.incident.model.dto.response.UserSummaryResponse;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class IncidentReportGeneratorTest {

  private static final UUID INCIDENT_ID = UUID.fromString(
    "2bd105d9-c3dd-45b5-ad28-97e4a6272506"
  );

  private IncidentReportGenerator generator;

  @BeforeEach
  void setUp() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("i18n/messages");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
    messageSource.setFallbackToSystemLocale(false);
    generator = new IncidentReportGenerator(messageSource);
  }

  @Test
  void generatesFullyLocalizedFrenchReport() throws IOException {
    String text = extractText(generator.generate(closedIncident(), Locale.FRENCH));

    assertThat(text)
      .contains("Fiche de traitement d'incident")
      .contains("Criticité")
      .contains("Élevée")
      .contains("Technique")
      .contains("Date de constatation")
      .contains("Respectée")
      .contains("INC-2BD105D9")
      .doesNotContain("Incident Treatment Report", "TECHNICAL", "Closed");
  }

  @Test
  void generatesFullyLocalizedEnglishReport() throws IOException {
    String text = extractText(generator.generate(closedIncident(), Locale.ENGLISH));

    assertThat(text)
      .contains("Incident Treatment Report")
      .contains("Criticality")
      .contains("High")
      .contains("Technical")
      .contains("Observation Date")
      .contains("Met")
      .doesNotContain("Fiche de traitement d'incident", "TECHNICAL", "Clôturé");
  }

  @Test
  void ordersHistoryChronologicallyAndKeepsWorkflowOrderForTies()
    throws IOException {
    String text = extractText(generator.generate(closedIncident(), Locale.ENGLISH));
    String historyText = text.substring(text.indexOf("Treatment History"));

    int creation = historyText.indexOf("Creation");
    int validation = historyText.indexOf("Validation");
    int transfer = historyText.indexOf("Transfer");
    int assignment = historyText.indexOf("Assignment");
    int resolved = historyText.indexOf("Resolved", assignment);
    int closed = historyText.indexOf("Closed", resolved);

    assertThat(creation).isNotNegative();
    assertThat(validation).isGreaterThan(creation);
    assertThat(transfer).isGreaterThan(validation);
    assertThat(assignment).isGreaterThan(transfer);
    assertThat(resolved).isGreaterThan(assignment);
    assertThat(closed).isGreaterThan(resolved);
  }

  @Test
  void avoidsDuplicatingIdenticalResolutionAndClosureText() throws IOException {
    String text = extractText(generator.generate(closedIncident(), Locale.FRENCH));

    assertThat(occurrences(text, "Intervention effectuée.")).isEqualTo(1);
    assertThat(text).contains("Clôture officielle de l'incident");
  }

  @Test
  void generatesValidPdfWhenOptionalFieldsAreNull() {
    IncidentResponse incident = new IncidentResponse();
    incident.setId(INCIDENT_ID);
    incident.setTitle("Incident minimal");
    incident.setStatus(IncidentStatus.OPEN);

    byte[] pdf = generator.generate(incident, Locale.FRENCH);

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII))
      .isEqualTo("%PDF-");
  }

  private IncidentResponse closedIncident() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 7, 17, 9, 3);
    LocalDateTime workflowAt = LocalDateTime.of(2026, 7, 17, 9, 4);
    LocalDateTime resolvedAt = LocalDateTime.of(2026, 7, 17, 18, 58);
    LocalDateTime closedAt = LocalDateTime.of(2026, 7, 17, 18, 59);

    IncidentResponse incident = new IncidentResponse();
    incident.setId(INCIDENT_ID);
    incident.setCreatedAt(createdAt);
    incident.setTitle("Niveau d'encre faible");
    incident.setDescription("Le niveau d'encre de l'imprimante est faible.");
    incident.setCriticality(Criticality.HIGH);
    incident.setStatus(IncidentStatus.CLOSED);
    incident.setCause(IncidentCause.TECHNICAL);
    incident.setCauseDetail("Cartouche épuisée");
    incident.setIncidentDate(LocalDate.of(2026, 7, 13));
    incident.setObservationDate(LocalDate.of(2026, 7, 14));
    incident.setDueDate(LocalDateTime.of(2026, 7, 19, 23, 59, 59));
    incident.setValidatedAt(workflowAt);
    incident.setTransferredAt(workflowAt);
    incident.setResolvedAt(resolvedAt);
    incident.setClosedAt(closedAt);
    incident.setResolutionDescription("Intervention effectuée.");
    incident.setClosureDescription("Intervention effectuée.");

    IncidentTypeConfigResponse type = new IncidentTypeConfigResponse();
    type.setDisplayName("Problème matériel");
    type.setSlaHours(48);
    incident.setType(type);

    UserSummaryResponse creator = user("Olivia", "Kuetche");
    UserSummaryResponse manager = user("Jordan", "Ngon");
    UserSummaryResponse assignee = user("Sheridane", "Fouoyo");
    incident.setCreatedBy(creator);
    incident.setValidatedBy(manager);
    incident.setAssignedTo(assignee);

    incident.setHistory(
      List.of(
        history(6, closedAt, assignee, ActionType.STATUS_CHANGE, "CLOSED", "Clôture validée"),
        history(1, createdAt, creator, ActionType.CREATION, null, null),
        history(5, resolvedAt, assignee, ActionType.STATUS_CHANGE, "RESOLVED", "Incident résolu"),
        history(4, workflowAt, manager, ActionType.ASSIGNMENT, null, "Assignation"),
        history(3, workflowAt, manager, ActionType.TRANSFER, null, "Transfert"),
        history(2, workflowAt, manager, ActionType.VALIDATION, null, "Validation"),
        history(7, createdAt.plusHours(2), manager, ActionType.COMMENT, null, null)
      )
    );

    IncidentCommentResponse comment = new IncidentCommentResponse();
    comment.setId(uuid(8));
    comment.setCreatedAt(createdAt.plusHours(2));
    comment.setAuthor(manager);
    comment.setContent("Le traitement est confirmé.");
    incident.setComments(List.of(comment));
    return incident;
  }

  private IncidentHistoryResponse history(
    int sequence,
    LocalDateTime createdAt,
    UserSummaryResponse user,
    ActionType action,
    String newValue,
    String comment
  ) {
    IncidentHistoryResponse entry = new IncidentHistoryResponse();
    entry.setId(uuid(sequence));
    entry.setCreatedAt(createdAt);
    entry.setUser(user);
    entry.setAction(action);
    entry.setNewValue(newValue);
    entry.setComment(comment);
    return entry;
  }

  private UserSummaryResponse user(String firstName, String lastName) {
    UserSummaryResponse user = new UserSummaryResponse();
    user.setFirstName(firstName);
    user.setLastName(lastName);
    return user;
  }

  private UUID uuid(int value) {
    return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", value));
  }

  private String extractText(byte[] pdf) throws IOException {
    PdfReader reader = new PdfReader(pdf);
    try {
      PdfTextExtractor extractor = new PdfTextExtractor(reader);
      StringBuilder text = new StringBuilder();
      for (int page = 1; page <= reader.getNumberOfPages(); page++) {
        text.append(extractor.getTextFromPage(page)).append('\n');
      }
      return text.toString();
    } finally {
      reader.close();
    }
  }

  private int occurrences(String text, String value) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(value, index)) >= 0) {
      count++;
      index += value.length();
    }
    return count;
  }
}
