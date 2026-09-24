// Genere la fiche de traitement d'un incident (PDF) : document de cloture lisible,
package com.fintrack.incident.report;

import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.dto.response.IncidentCommentResponse;
import com.fintrack.incident.model.dto.response.IncidentHistoryResponse;
import com.fintrack.incident.model.dto.response.IncidentResponse;
import com.fintrack.incident.model.dto.response.UserSummaryResponse;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

// Construit le PDF d'une fiche de traitement a partir du detail enrichi de l'incident.
@Component
@RequiredArgsConstructor
public class IncidentReportGenerator {

  private final MessageSource messageSource;

  private String t(Locale locale, String key, Object... args) {
    return messageSource.getMessage(key, args, key, locale);
  }

  private static final Color NAVY = new Color(30, 41, 59); // #1E293B
  private static final Color RED = new Color(229, 57, 53); // #E53935
  private static final Color SLATE_LIGHT = new Color(241, 245, 249);
  private static final Color DARK = new Color(33, 33, 33);
  private static final Color MUTED = new Color(100, 116, 139);
  private static final Color WHITE = Color.WHITE;
  private static final Color GOLD = new Color(200, 162, 46);
  private static final Color AUDIT_BG = new Color(254, 249, 237);

  private static final byte[] FINSTAR_LOGO_BYTES = loadLogo("/reports/finstar-logo.png");

  // Charge un logo depuis le classpath.
  private static byte[] loadLogo(String path) {
    try (var in = IncidentReportGenerator.class.getResourceAsStream(path)) {
      return in != null ? in.readAllBytes() : null;
    } catch (Exception e) {
      return null;
    }
  }

  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern(
    "dd/MM/yyyy HH:mm"
  );
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(
    "dd/MM/yyyy"
  );

  // Produit la fiche PDF de l'incident fourni.
  public byte[] generate(IncidentResponse incident) {
    return generate(incident, LocaleContextHolder.getLocale());
  }

  // Produit la fiche PDF dans la langue explicitement demandee par l'interface.
  public byte[] generate(IncidentResponse incident, Locale requestedLocale) {
    Objects.requireNonNull(incident, "incident");
    Locale locale = supportedLocale(requestedLocale);
    LocalDateTime generatedAt = LocalDateTime.now();
    String reference = reference(incident);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 45, 45, 50, 55);
    try {
      PdfWriter writer = PdfWriter.getInstance(document, out);
      writer.setPdfVersion(PdfWriter.VERSION_1_7);
      writer.setTagged();
      writer.setPageEvent(
        new FooterEvent(
          t(locale, "report.footer.title"),
          t(locale, "report.footer.page"),
          t(locale, "report.continuation", reference)
        )
      );
      document.addTitle(t(locale, "report.title"));
      document.addSubject(t(locale, "report.metadata.subject", reference));
      document.addAuthor("FINSTAR-CM S.A.");
      document.addCreator("FinTrack");
      document.open();

      addHeader(document, incident, locale, generatedAt, reference);
      addIdentity(document, incident, locale);
      addParties(document, incident, locale);
      addTimeline(document, incident, locale, generatedAt.toLocalDate());
      addTextSection(
        document,
        t(locale, "report.section.description"),
        incident.getDescription()
      );
      addCause(document, incident, locale);
      addOutcome(document, incident, locale);
      addHistory(document, incident.getHistory(), incident.getComments(), locale);
      addComments(document, incident.getComments(), locale);

      document.close();
    } catch (DocumentException ex) {
      throw new IllegalStateException(
        "Échec de génération de la fiche d'incident",
        ex
      );
    }
    return out.toByteArray();
  }

  // Bandeau d'entete : logo FINSTAR + identite FinTrack + titre du document.
  private void addHeader(
    Document document,
    IncidentResponse incident,
    Locale locale,
    LocalDateTime generatedAt,
    String reference
  )
    throws DocumentException {

    // --- Ligne des logos : Finstar (gauche) + Marque FinTrack (droite) ---
    PdfPTable logoRow = fullWidthTable(2);
    logoRow.setWidths(new float[] { 1.4f, 2.6f });

    // Cellule du logo Finstar
    PdfPCell logoCell = new PdfPCell();
    logoCell.setBorder(Rectangle.NO_BORDER);
    logoCell.setPadding(6f);
    logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    if (FINSTAR_LOGO_BYTES != null) {
      try {
        Image finstarImg = Image.getInstance(FINSTAR_LOGO_BYTES);
        finstarImg.scaleToFit(140, 50);
        logoCell.addElement(finstarImg);
      } catch (Exception ignored) {
        logoCell.addElement(
          new Phrase("FINSTAR-CM S.A.",
            font(FontFactory.HELVETICA_BOLD, 14, GOLD))
        );
      }
    } else {
      logoCell.addElement(
        new Phrase("FINSTAR-CM S.A.",
          font(FontFactory.HELVETICA_BOLD, 14, GOLD))
      );
    }
    logoRow.addCell(logoCell);

    // Cellule de la marque FinTrack
    PdfPCell brandCell = new PdfPCell();
    brandCell.setBorder(Rectangle.NO_BORDER);
    brandCell.setPadding(6f);
    brandCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    brandCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    Paragraph fintrackName = new Paragraph();
    fintrackName.setAlignment(Element.ALIGN_RIGHT);
    fintrackName.add(new Phrase("Fin",
      font(FontFactory.HELVETICA_BOLD, 16, RED)));
    fintrackName.add(new Phrase("Track",
      font(FontFactory.HELVETICA_BOLD, 16, NAVY)));
    Paragraph fintrackSub = new Paragraph(
      t(locale, "report.fintrack_subtitle"),
      font(FontFactory.HELVETICA, 8, MUTED)
    );
    fintrackSub.setAlignment(Element.ALIGN_RIGHT);
    brandCell.addElement(fintrackName);
    brandCell.addElement(fintrackSub);
    logoRow.addCell(brandCell);

    logoRow.setSpacingAfter(6f);
    document.add(logoRow);

    // --- Bandeau de titre (fond bleu marine) ---
    PdfPTable band = fullWidthTable(1);
    PdfPCell cell = new PdfPCell();
    cell.setBackgroundColor(NAVY);
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setPadding(14f);

    Paragraph title = new Paragraph(
      t(locale, "report.title"),
      font(FontFactory.HELVETICA_BOLD, 18, WHITE)
    );
    cell.addElement(title);
    band.addCell(cell);
    document.add(band);

    // --- Ligne d'accentuation rouge ---
    PdfPTable rule = fullWidthTable(1);
    PdfPCell red = new PdfPCell();
    red.setFixedHeight(3f);
    red.setBackgroundColor(RED);
    red.setBorder(Rectangle.NO_BORDER);
    rule.addCell(red);
    document.add(rule);

    // --- Métadonnées de référence / date ---
    PdfPTable metadata = fullWidthTable(2);
    metadata.setWidths(new float[] { 1f, 1f });
    metadata.addCell(
      metadataCell(
        t(locale, "report.reference_prefix"),
        reference,
        Element.ALIGN_LEFT
      )
    );
    metadata.addCell(
      metadataCell(
        t(locale, "report.edited_at"),
        generatedAt.format(DATE_TIME),
        Element.ALIGN_RIGHT
      )
    );
    metadata.setSpacingBefore(6f);
    metadata.setSpacingAfter(8f);
    document.add(metadata);
  }

  private void addIdentity(
    Document document,
    IncidentResponse incident,
    Locale locale
  )
    throws DocumentException {
    Map<String, String> rows = new LinkedHashMap<>();
    // Le code metier ouvre la fiche : c'est par lui que l'incident est designe.
    rows.put(t(locale, "report.field.reference"), reference(incident));
    rows.put(t(locale, "report.field.title"), safe(incident.getTitle()));
    rows.put(
      t(locale, "report.field.type"),
      incident.getType() != null ? incident.getType().getDisplayName() : "-"
    );
    rows.put(
      t(locale, "report.field.criticality"),
      criticality(incident.getCriticality(), locale)
    );
    rows.put(
      t(locale, "report.field.status"),
      status(incident.getStatus(), locale)
    );
    rows.put(
      t(locale, "report.field.technical_id"),
      incident.getId() != null ? incident.getId().toString() : "-"
    );
    addSection(document, t(locale, "report.section.identity"), rows);
  }

  private void addParties(
    Document document,
    IncidentResponse incident,
    Locale locale
  )
    throws DocumentException {
    Map<String, String> rows = new LinkedHashMap<>();
    rows.put(t(locale, "report.field.reporter"), user(incident.getCreatedBy()));
    rows.put(t(locale, "report.field.assignee"), user(incident.getAssignedTo()));
    rows.put(t(locale, "report.field.validated_by"), user(incident.getValidatedBy()));
    addStatusActor(rows, incident, IncidentStatus.RESOLVED, "report.field.resolved_by", locale);
    addStatusActor(rows, incident, IncidentStatus.CLOSED, "report.field.closed_by", locale);
    rows.put(
      t(locale, "report.field.agency"),
      incident.getAgency() != null ? safe(incident.getAgency().getName()) : "-"
    );
    if (incident.getTransferredToService() != null) {
      rows.put(
        t(locale, "report.field.target_service"),
        safe(incident.getTransferredToService().getName())
      );
    }
    addSection(document, t(locale, "report.section.parties"), rows);
  }

  private void addTimeline(
    Document document,
    IncidentResponse incident,
    Locale locale,
    LocalDate generatedDate
  )
    throws DocumentException {
    Map<String, String> rows = new LinkedHashMap<>();
    rows.put(t(locale, "report.field.reported_at"), dateTime(incident.getCreatedAt()));
    rows.put(t(locale, "report.field.incident_date"), date(incident.getIncidentDate()));
    rows.put(t(locale, "report.field.observation_date"), date(incident.getObservationDate()));
    putDateTimeIfPresent(rows, t(locale, "report.field.validated_at"), incident.getValidatedAt());
    putDateTimeIfPresent(rows, t(locale, "report.field.transferred_at"), incident.getTransferredAt());
    putDateTimeIfPresent(rows, t(locale, "report.field.blocked_at"), incident.getBlockedAt());
    putDateTimeIfPresent(rows, t(locale, "report.field.reopened_at"), incident.getReopenedAt());
    putDateTimeIfPresent(rows, t(locale, "report.field.resolved_at"), incident.getResolvedAt());
    putDateTimeIfPresent(rows, t(locale, "report.field.closed_at"), incident.getClosedAt());
    rows.put(
      t(locale, "report.field.due_date"),
      date(
        incident.getDueDate() != null
          ? incident.getDueDate().toLocalDate()
          : null
      )
    );
    if (incident.getType() != null && incident.getType().getSlaHours() != null) {
      rows.put(
        t(locale, "report.field.sla_target"),
        t(locale, "report.value.hours", incident.getType().getSlaHours())
      );
    }
    rows.put(
      t(locale, "report.field.sla_result"),
      slaResult(incident, generatedDate, locale)
    );
    addSection(document, t(locale, "report.section.timeline"), rows);
    addLifecycleDetails(document, incident, locale);
  }

  private void addCause(
    Document document,
    IncidentResponse incident,
    Locale locale
  )
    throws DocumentException {
    if (incident.getCause() == null && isBlank(incident.getCauseDetail())) {
      return;
    }
    Map<String, String> rows = new LinkedHashMap<>();
    if (incident.getCause() != null) {
      rows.put(
        t(locale, "report.field.cause"),
        t(locale, incident.getCause().getNameKey())
      );
    }
    if (!isBlank(incident.getCauseDetail())) {
      rows.put(t(locale, "report.field.detail"), incident.getCauseDetail());
    }
    addSection(document, t(locale, "report.section.cause_analysis"), rows);
  }

  // Presente une seule fois la resolution et la cloture lorsqu'elles sont identiques.
  private void addOutcome(
    Document document,
    IncidentResponse incident,
    Locale locale
  ) throws DocumentException {
    String treatmentText = treatmentText(incident);
    String resolutionText = resolutionText(incident);
    String closureText = incident.getClosureDescription();
    boolean closed = IncidentStatus.CLOSED.equals(incident.getStatus());

    // Le traitement (ce qui a ete fait) precede sa validation (la resolution).
    if (!isBlank(treatmentText) && !sameText(treatmentText, resolutionText)) {
      addTextSection(
        document,
        t(locale, "report.section.treatment_note"),
        treatmentText
      );
    }

    if (!closed) {
      if (!isBlank(resolutionText)) {
        addTextSection(
          document,
          t(locale, "report.section.resolution_note"),
          resolutionText
        );
      }
      return;
    }

    String officialClosure = !isBlank(closureText) ? closureText : resolutionText;
    if (!isBlank(resolutionText) && !sameText(resolutionText, officialClosure)) {
      addTextSection(
        document,
        t(locale, "report.section.resolution_note"),
        resolutionText
      );
    }
    addClosure(document, officialClosure, locale);
  }

  // Met en valeur la conclusion officielle d'un incident cloture.
  private void addClosure(
    Document document,
    String closureText,
    Locale locale
  ) throws DocumentException {

    Paragraph heading = new Paragraph(
      t(locale, "report.section.closure"),
      font(FontFactory.HELVETICA_BOLD, 14, NAVY)
    );
    heading.setSpacingBefore(14f);
    heading.setSpacingAfter(4f);
    document.add(heading);

    String text = isBlank(closureText)
      ? t(locale, "report.no_closure")
      : closureText;

    // Conteneur extérieur : bordure rouge complète pour attirer l'attention
    PdfPTable container = fullWidthTable(1);
    PdfPCell outerCell = new PdfPCell();
    outerCell.setBorder(Rectangle.BOX);
    outerCell.setBorderColor(RED);
    outerCell.setBorderWidth(1.5f);
    outerCell.setPadding(0f);

    // Tableau intérieur avec barre d'accentuation rouge à gauche
    PdfPTable inner = fullWidthTable(1);
    PdfPCell contentCell = new PdfPCell();
    contentCell.setBackgroundColor(AUDIT_BG);
    contentCell.setBorder(Rectangle.LEFT);
    contentCell.setBorderColor(RED);
    contentCell.setBorderWidthLeft(4f);
    contentCell.setPadding(12f);

    Paragraph closureBody = new Paragraph(
      text,
      font(FontFactory.HELVETICA, 11f, DARK)
    );
    closureBody.setLeading(16f);
    contentCell.addElement(closureBody);

    inner.addCell(contentCell);
    outerCell.addElement(inner);
    container.addCell(outerCell);
    container.setSpacingAfter(12f);
    document.add(container);
  }

  private void addHistory(
    Document document,
    List<IncidentHistoryResponse> history,
    List<IncidentCommentResponse> comments,
    Locale locale
  ) throws DocumentException {
    if (history == null || history.isEmpty()) {
      return;
    }
    boolean commentsShownSeparately = comments != null && !comments.isEmpty();
    List<IncidentHistoryResponse> orderedHistory = history.stream()
      .filter(Objects::nonNull)
      .filter(entry ->
        !commentsShownSeparately || !ActionType.COMMENT.equals(entry.getAction())
      )
      .sorted(historyComparator())
      .toList();
    if (orderedHistory.isEmpty()) {
      return;
    }
    sectionTitle(document, t(locale, "report.section.history"));

    PdfPTable table = fullWidthTable(4);
    table.setWidths(new float[] { 1.5f, 2f, 2f, 4.5f });
    table.setSplitLate(false);
    table.setSplitRows(true);

    headerCell(table, t(locale, "report.history.date"));
    headerCell(table, t(locale, "report.history.action"));
    headerCell(table, t(locale, "report.history.author"));
    headerCell(table, t(locale, "report.history.comment"));
    table.setHeaderRows(1);

    boolean alt = false;
    for (IncidentHistoryResponse entry : orderedHistory) {
      Color bg = alt ? SLATE_LIGHT : WHITE;
      bodyCell(table, dateTime(entry.getCreatedAt()), bg);
      bodyCell(table, actionLabel(entry, locale), bg);
      bodyCell(table, user(entry.getUser()), bg);
      bodyCell(table, safe(entry.getComment()), bg);
      alt = !alt;
    }
    table.setSpacingAfter(10f);
    document.add(table);
  }

  private void addComments(
    Document document,
    List<IncidentCommentResponse> comments,
    Locale locale
  ) throws DocumentException {
    if (comments == null || comments.isEmpty()) {
      return;
    }
    sectionTitle(document, t(locale, "report.section.comments"));

    List<IncidentCommentResponse> orderedComments = comments.stream()
      .filter(Objects::nonNull)
      .sorted(commentComparator())
      .toList();
    for (IncidentCommentResponse comment : orderedComments) {
      String metaText = user(comment.getAuthor()) + " | " + dateTime(comment.getCreatedAt()) +
        (comment.isInternal() ? t(locale, "report.comment.internal") : "");
      Paragraph meta = new Paragraph(
        metaText,
        font(FontFactory.HELVETICA_BOLD, 9, NAVY)
      );
      meta.setSpacingBefore(6f);
      meta.setSpacingAfter(2f);
      document.add(meta);

      Paragraph text = new Paragraph(
        safe(comment.getContent()),
        font(FontFactory.HELVETICA, 10, DARK)
      );
      document.add(text);
    }
  }

  // ----- helpers de mise en page -----

  private void addSection(
    Document document,
    String title,
    Map<String, String> rows
  ) throws DocumentException {
    sectionTitle(document, title);
    PdfPTable table = fullWidthTable(2);
    table.setWidths(new float[] { 1.2f, 3f });
    for (Map.Entry<String, String> row : rows.entrySet()) {
      PdfPCell label = new PdfPCell(
        new Phrase(row.getKey(), font(FontFactory.HELVETICA, 9.5f, MUTED))
      );
      label.setBorder(Rectangle.NO_BORDER);
      label.setPadding(4f);
      PdfPCell value = new PdfPCell(
        new Phrase(
          row.getValue(),
          font(FontFactory.HELVETICA_BOLD, 10, DARK)
        )
      );
      value.setBorder(Rectangle.NO_BORDER);
      value.setPadding(4f);
      table.addCell(label);
      table.addCell(value);
    }
    table.setSpacingAfter(10f);
    document.add(table);
  }

  private void addTextSection(Document document, String title, String text)
    throws DocumentException {
    PdfPTable block = fullWidthTable(1);
    block.setKeepTogether(true);

    PdfPCell headingCell = new PdfPCell();
    headingCell.setBorder(Rectangle.NO_BORDER);
    headingCell.setPaddingTop(8f);
    headingCell.setPaddingBottom(4f);
    headingCell.addElement(
      new Paragraph(title, font(FontFactory.HELVETICA_BOLD, 13, NAVY))
    );
    block.addCell(headingCell);

    PdfPCell bodyCell = new PdfPCell();
    bodyCell.setBorder(Rectangle.NO_BORDER);
    bodyCell.setPadding(0f);
    Paragraph body = new Paragraph(
      isBlank(text) ? "-" : text,
      font(FontFactory.HELVETICA, 10.5f, DARK)
    );
    bodyCell.addElement(body);
    block.addCell(bodyCell);
    block.setSpacingAfter(10f);
    document.add(block);
  }

  // Construit une cellule compacte pour les metadonnees principales.
  private PdfPCell metadataCell(String label, String value, int alignment) {
    PdfPCell cell = new PdfPCell();
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setBackgroundColor(SLATE_LIGHT);
    cell.setPadding(7f);
    Paragraph text = new Paragraph();
    text.setAlignment(alignment);
    text.add(new Phrase(label + " ", font(FontFactory.HELVETICA, 8, MUTED)));
    text.add(new Phrase(value, font(FontFactory.HELVETICA_BOLD, 8.5f, NAVY)));
    cell.addElement(text);
    return cell;
  }

  private void sectionTitle(Document document, String title)
    throws DocumentException {
    Paragraph heading = new Paragraph(
      title,
      font(FontFactory.HELVETICA_BOLD, 13, NAVY)
    );
    heading.setSpacingBefore(8f);
    heading.setSpacingAfter(4f);
    document.add(heading);
  }

  private void headerCell(PdfPTable table, String text) {
    PdfPCell cell = new PdfPCell(
      new Phrase(text, font(FontFactory.HELVETICA_BOLD, 9, WHITE))
    );
    cell.setBackgroundColor(NAVY);
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setPadding(5f);
    table.addCell(cell);
  }

  private void bodyCell(PdfPTable table, String text, Color background) {
    PdfPCell cell = new PdfPCell(
      new Phrase(text, font(FontFactory.HELVETICA, 9, DARK))
    );
    cell.setBackgroundColor(background);
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setPadding(5f);
    table.addCell(cell);
  }

  private PdfPTable fullWidthTable(int columns) throws DocumentException {
    PdfPTable table = new PdfPTable(columns);
    table.setWidthPercentage(100);
    return table;
  }

  private Font font(String family, float size, Color color) {
    return FontFactory.getFont(family, size, color);
  }

  // ----- helpers de formatage -----

  private String user(UserSummaryResponse user) {
    if (user == null) {
      return "-";
    }
    String full = (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
    return full.isEmpty() ? safe(user.getUsername()) : full;
  }

  // Traduit la criticite dans la langue du document.
  private String criticality(Criticality value, Locale locale) {
    if (value == null) {
      return "-";
    }
    return t(locale, value.getNameKey());
  }

  // Traduit le statut dans la langue du document.
  private String status(IncidentStatus value, Locale locale) {
    if (value == null) {
      return "-";
    }
    return t(locale, value.getNameKey());
  }

  // Actions portant une transition de statut (oldValue/newValue renseignes).
  private static final java.util.Set<ActionType> STATUS_TRANSITION_ACTIONS =
    java.util.Set.of(
      ActionType.STATUS_CHANGE,
      ActionType.REOPENING,
      ActionType.RESOLUTION_REJECTED
    );

  // Ajoute le nouveau statut a une action de changement d'etat.
  private String actionLabel(IncidentHistoryResponse entry, Locale locale) {
    if (entry.getAction() == null) {
      return "-";
    }
    String base = t(locale, entry.getAction().getNameKey());
    if (
      STATUS_TRANSITION_ACTIONS.contains(entry.getAction()) &&
      !isBlank(entry.getNewValue())
    ) {
      return base + " - " + statusFromCode(entry.getNewValue(), locale);
    }
    return base;
  }

  private String statusFromCode(String code, Locale locale) {
    for (IncidentStatus s : IncidentStatus.values()) {
      if (s.getName().equals(code)) {
        return t(locale, s.getNameKey());
      }
    }
    return code;
  }

  // Ajoute l'acteur ayant produit un statut cle du workflow.
  private void addStatusActor(
    Map<String, String> rows,
    IncidentResponse incident,
    IncidentStatus status,
    String labelKey,
    Locale locale
  ) {
    findStatusEntry(incident.getHistory(), status)
      .map(IncidentHistoryResponse::getUser)
      .ifPresent(actor -> rows.put(t(locale, labelKey), user(actor)));
  }

  // Recherche le dernier evenement correspondant au statut demande.
  private java.util.Optional<IncidentHistoryResponse> findStatusEntry(
    List<IncidentHistoryResponse> history,
    IncidentStatus status
  ) {
    if (history == null) {
      return java.util.Optional.empty();
    }
    return history.stream()
      .filter(Objects::nonNull)
      .filter(entry -> STATUS_TRANSITION_ACTIONS.contains(entry.getAction()))
      .filter(entry -> status.getName().equals(entry.getNewValue()))
      .max(historyComparator());
  }

  // Selectionne le compte rendu de traitement le plus fiable disponible ; le repli sur
  // l'historique couvre les incidents anterieurs au champ dedie.
  private String treatmentText(IncidentResponse incident) {
    if (!isBlank(incident.getTreatmentDescription())) {
      return incident.getTreatmentDescription();
    }
    return findStatusEntry(incident.getHistory(), IncidentStatus.TREATED)
      .map(IncidentHistoryResponse::getComment)
      .orElse(null);
  }

  // Selectionne la description de resolution la plus fiable disponible.
  private String resolutionText(IncidentResponse incident) {
    if (!isBlank(incident.getResolutionDescription())) {
      return incident.getResolutionDescription();
    }
    return findStatusEntry(incident.getHistory(), IncidentStatus.RESOLVED)
      .map(IncidentHistoryResponse::getComment)
      .orElse(null);
  }

  // Affiche uniquement les motifs renseignes pour les transitions exceptionnelles.
  private void addLifecycleDetails(
    Document document,
    IncidentResponse incident,
    Locale locale
  ) throws DocumentException {
    Map<String, String> rows = new LinkedHashMap<>();
    putIfPresent(rows, t(locale, "report.field.transfer_reason"), incident.getTransferReason());
    putIfPresent(rows, t(locale, "report.field.blocked_reason"), incident.getBlockedReason());
    putIfPresent(rows, t(locale, "report.field.reject_reason"), incident.getRejectReason());
    putIfPresent(rows, t(locale, "report.field.cancel_reason"), incident.getCancelReason());
    putIfPresent(rows, t(locale, "report.field.reopen_reason"), incident.getReopenReason());
    if (incident.getReopenCount() > 0) {
      rows.put(
        t(locale, "report.field.reopen_count"),
        Integer.toString(incident.getReopenCount())
      );
    }
    if (!rows.isEmpty()) {
      addSection(document, t(locale, "report.section.lifecycle_details"), rows);
    }
  }

  // Qualifie le respect de l'echeance a partir des dates persistantes.
  private String slaResult(
    IncidentResponse incident,
    LocalDate generatedDate,
    Locale locale
  ) {
    if (incident.getDueDate() == null) {
      return "-";
    }
    // L'echeance est horaire : on compare des instants, pas des jours.
    LocalDateTime completedAt = incident.getClosedAt() != null
      ? incident.getClosedAt()
      : incident.getResolvedAt();
    if (completedAt != null) {
      return completedAt.isAfter(incident.getDueDate())
        ? t(locale, "report.sla.exceeded")
        : t(locale, "report.sla.respected");
    }
    return generatedDate.atTime(LocalTime.MAX).isAfter(incident.getDueDate())
      ? t(locale, "report.sla.exceeded")
      : t(locale, "report.sla.in_progress");
  }

  // Ordonne l'historique par date puis par sequence metier en cas d'egalite.
  private Comparator<IncidentHistoryResponse> historyComparator() {
    return Comparator.comparing(
      IncidentHistoryResponse::getCreatedAt,
      Comparator.nullsLast(Comparator.naturalOrder())
    )
      .thenComparingInt(this::historySequence)
      .thenComparing(
        IncidentHistoryResponse::getId,
        Comparator.nullsLast(Comparator.naturalOrder())
      );
  }

  // Ordonne les commentaires du plus ancien au plus recent.
  private Comparator<IncidentCommentResponse> commentComparator() {
    return Comparator.comparing(
      IncidentCommentResponse::getCreatedAt,
      Comparator.nullsLast(Comparator.naturalOrder())
    ).thenComparing(
      IncidentCommentResponse::getId,
      Comparator.nullsLast(Comparator.naturalOrder())
    );
  }

  // Retourne l'ordre metier d'un evenement partageant le meme horodatage.
  private int historySequence(IncidentHistoryResponse entry) {
    if (entry.getAction() == null) {
      return 100;
    }
    if (ActionType.CREATION.equals(entry.getAction())) {
      return 0;
    }
    if (ActionType.VALIDATION.equals(entry.getAction())) {
      return 10;
    }
    if (ActionType.TRANSFER.equals(entry.getAction())) {
      return 20;
    }
    if (ActionType.ASSIGNMENT.equals(entry.getAction())) {
      return 30;
    }
    if (STATUS_TRANSITION_ACTIONS.contains(entry.getAction())) {
      return 40 + statusSequence(entry.getNewValue());
    }
    if (ActionType.UPDATE.equals(entry.getAction())) {
      return 70;
    }
    if (ActionType.ROUTING.equals(entry.getAction())) {
      return 80;
    }
    return 90;
  }

  // Retourne l'ordre naturel d'un statut dans le cycle de vie.
  private int statusSequence(String statusCode) {
    if (statusCode == null) {
      return 0;
    }
    for (int index = 0; index < IncidentStatus.values().length; index++) {
      if (IncidentStatus.values()[index].getName().equals(statusCode)) {
        return index;
      }
    }
    return IncidentStatus.values().length;
  }

  // Ajoute une date uniquement lorsqu'elle existe.
  private void putDateTimeIfPresent(
    Map<String, String> rows,
    String label,
    LocalDateTime value
  ) {
    if (value != null) {
      rows.put(label, dateTime(value));
    }
  }

  // Ajoute une valeur textuelle uniquement lorsqu'elle est renseignee.
  private void putIfPresent(Map<String, String> rows, String label, String value) {
    if (!isBlank(value)) {
      rows.put(label, value);
    }
  }

  // Compare deux descriptions sans tenir compte des espaces de presentation.
  private boolean sameText(String first, String second) {
    return !isBlank(first) &&
      !isBlank(second) &&
      first.trim().replaceAll("\\s+", " ").equalsIgnoreCase(
        second.trim().replaceAll("\\s+", " ")
      );
  }

  // Limite la langue du document aux locales officiellement prises en charge.
  private Locale supportedLocale(Locale locale) {
    return locale != null && Locale.FRENCH.getLanguage().equals(locale.getLanguage())
      ? Locale.FRENCH
      : Locale.ENGLISH;
  }

  // Produit une reference courte stable tout en conservant l'UUID dans le detail.
  // Le code metier designe l'incident dans l'entete, le pied de page et les
  // metadonnees du PDF. Repli sur l'UUID abrege pour les incidents anterieurs.
  private String reference(IncidentResponse incident) {
    if (incident.getReference() != null && !incident.getReference().isBlank()) {
      return incident.getReference();
    }
    UUID id = incident.getId();
    if (id == null) {
      return "INC-UNKNOWN";
    }
    return "INC-" + id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
  }

  private String dateTime(LocalDateTime value) {
    return value == null ? "-" : value.format(DATE_TIME);
  }

  private String date(LocalDate value) {
    return value == null ? "-" : value.format(DATE);
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  // Pied de page : identite + numero de page sur chaque feuille.
  private static final class FooterEvent extends PdfPageEventHelper {

    private final String titleText;
    private final String pageText;
    private final String continuationText;
    private final Font footerFont = FontFactory.getFont(
      FontFactory.HELVETICA,
      8,
      MUTED
    );

    public FooterEvent(
      String titleText,
      String pageText,
      String continuationText
    ) {
      this.titleText = titleText;
      this.pageText = pageText;
      this.continuationText = continuationText;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      PdfContentByte cb = writer.getDirectContent();

      if (writer.getPageNumber() > 1) {
        ColumnText.showTextAligned(
          cb,
          Element.ALIGN_LEFT,
          new Phrase(continuationText, footerFont),
          document.left(),
          document.top() + 20,
          0
        );
        cb.setColorStroke(RED);
        cb.setLineWidth(0.8f);
        cb.moveTo(document.left(), document.top() + 14);
        cb.lineTo(document.right(), document.top() + 14);
        cb.stroke();
      }

      ColumnText.showTextAligned(
        cb,
        Element.ALIGN_LEFT,
        new Phrase(titleText, footerFont),
        document.left(),
        document.bottom() - 15,
        0
      );

      String text = pageText + writer.getPageNumber();
      ColumnText.showTextAligned(
        cb,
        Element.ALIGN_RIGHT,
        new Phrase(text, footerFont),
        document.right(),
        document.bottom() - 15,
        0
      );
    }
  }
}
