// Composant backend : porte la logique liee a report document generator.

package com.fintrack.reporting.report;

import com.fintrack.reporting.exception.BusinessRuleViolationException;
import com.fintrack.reporting.exception.ErrorCode;
import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportGenerationType;
import com.fintrack.reporting.model.entity.GeneratedReport;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.awt.Paint;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Génère le document de rapport téléchargeable aux couleurs de FINSTAR (PDF, Excel ou JSON).
 * Le PDF contient l'en-tête en étoile de FINSTAR sur chaque page.
 */

@Slf4j
@Component
@RequiredArgsConstructor
// Modelise la responsabilite applicative liee a rapport.
public class ReportDocumentGenerator {

  private final ObjectMapper objectMapper;
  private final MessageSource messageSource;

  // Traduit une cle i18n pour les libelles du document genere.
  private String t(String key, Object... args) {
    return messageSource.getMessage(
      key,
      args,
      key,
      LocaleContextHolder.getLocale()
    );
  }

  private static final Color GOLD = new Color(200, 162, 46);
  private static final Color PINK = new Color(229, 0, 109);
  private static final Color DARK = new Color(33, 33, 33);
  // Accents FinTrack (hybride : en-tete/logo FINSTAR conserves, corps aux couleurs FinTrack).
  private static final Color NAVY = new Color(30, 41, 59); // #1E293B (primary)
  private static final Color SLATE_LIGHT = new Color(241, 245, 249); // #F1F5F9
  // Palette categorielle (type, age, cohorte...) sans magenta ni violet.
  private static final Color[] CATEGORICAL = {
    new Color(30, 41, 59), // navy
    new Color(245, 158, 11), // ambre
    new Color(16, 185, 129), // vert
    new Color(59, 130, 246), // bleu
    new Color(20, 184, 166), // teal
    new Color(100, 116, 139), // slate
    new Color(148, 163, 184), // slate clair
  };
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern(
    "dd/MM/yyyy HH:mm"
  );
  private static final List<String> PERIOD_FLOW_METRICS = List.of(
    "inflow",
    "outflow",
    "rejectedIncidents",
    "cancelledIncidents",
    "netBacklog"
  );
  private static final List<String> CURRENT_SNAPSHOT_METRICS = List.of(
    "activeIncidents",
    "blockedIncidents",
    "slaBreachNow"
  );
  private static final List<String> PERFORMANCE_METRICS = List.of(
    "avgClosureHours",
    "medianClosureHours",
    "p90ClosureHours",
    "avgNetClosureHours",
    "medianNetClosureHours",
    "closureSampleSize",
    "avgResolutionHours",
    "medianResolutionHours",
    "p90ResolutionHours",
    "resolutionSampleSize",
    "avgTimeToFirstResponse",
    "slaComplianceRate",
    "transferRate",
    "resolutionReopenRate"
  );
  private static final List<String> PERSONAL_METRICS = List.of(
    "assignedToMe",
    "transferredByMe",
    "closedByMe",
    "createdByMe",
    "resolvedByMe"
  );
  private static final Set<String> HOUR_METRICS = Set.of(
    "avgClosureHours",
    "medianClosureHours",
    "p90ClosureHours",
    "avgNetClosureHours",
    "medianNetClosureHours",
    "avgResolutionHours",
    "medianResolutionHours",
    "p90ResolutionHours",
    "avgTimeToFirstResponse"
  );
  // Avant la separation des jalons, la famille *ResolutionHours portait le delai de cloture.
  private static final Map<String, String> LEGACY_CLOSURE_KEYS = Map.of(
    "avgResolutionHours",
    "avgClosureHours",
    "medianResolutionHours",
    "medianClosureHours",
    "p90ResolutionHours",
    "p90ClosureHours",
    "monthlyAvgResolutionHours",
    "monthlyAvgClosureHours"
  );
  private static final Set<String> PERCENTAGE_METRICS = Set.of(
    "transferRate",
    "slaComplianceRate",
    "resolutionReopenRate"
  );
  private static final Set<String> HIDDEN_FILTER_KEYS = Set.of(
    "view",
    "agencyId",
    "serviceId",
    "subjectUserId"
  );
  private static final byte[] LOGO_BYTES = loadLogo();

  // Charge le logo utilise dans les documents generes.

  private static byte[] loadLogo() {
    try (
      var in = ReportDocumentGenerator.class.getResourceAsStream(
        "/reports/finstar-logo.png"
      )
    ) {
      return in != null ? in.readAllBytes() : null;
    } catch (Exception e) {
      return null;
    }
  }

  // Genere la sortie attendue pour le domaine rapport document generator.

  public byte[] generate(GeneratedReport report) {
    Locale previousLocale = LocaleContextHolder.getLocale();
    LocaleContextHolder.setLocale(resolveDocumentLocale(report, previousLocale));
    try {
      ReportFormat format =
        report.getFormat() != null ? report.getFormat() : ReportFormat.PDF;
      return switch (format) {
        case EXCEL -> generateExcel(report);
        case JSON -> generateJson(report);
        case PDF -> generatePdf(report);
      };
    } finally {
      LocaleContextHolder.setLocale(previousLocale);
    }
  }

  // Determine l'extension de fichier associee au format demande.
  public String fileExtension(ReportFormat format) {
    return switch (format != null ? format : ReportFormat.PDF) {
      case EXCEL -> "xlsx";
      case JSON -> "json";
      case PDF -> "pdf";
    };
  }

  // PDF

  private byte[] generatePdf(GeneratedReport report) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 25, 25, 85, 45);
    try {
      PdfWriter writer = PdfWriter.getInstance(document, out);
      writer.setPageEvent(new FinstarHeaderFooter());
      document.open();

      Map<String, Object> metrics = normalizeMilestoneKeys(
        readJson(report.getMetrics())
      );
      Map<String, Object> filters = readJson(report.getFilters());
      Collection<?> groupedReports = groupedReports(metrics);
      boolean hasMetrics = hasDisplayableMetrics(metrics);
      boolean hasIncidents =
        hasIncidentRows(metrics) ||
        groupedReports
          .stream()
          .map(this::groupMetrics)
          .anyMatch(this::hasIncidentRows);

      // --- Cover Page ---
      Font coverTitleFont = FontFactory.getFont(
        FontFactory.HELVETICA_BOLD,
        24,
        NAVY
      );
      Paragraph coverTitle = new Paragraph(t("report.title"), coverTitleFont);
      coverTitle.setAlignment(Element.ALIGN_CENTER);
      coverTitle.setSpacingBefore(82);
      coverTitle.setSpacingAfter(10);

      Font coverSubFont = FontFactory.getFont(
        FontFactory.HELVETICA,
        13,
        new Color(75, 85, 99)
      );
      Paragraph coverSubtitle = new Paragraph(
        safe(report.getName()),
        coverSubFont
      );
      coverSubtitle.setAlignment(Element.ALIGN_CENTER);
      coverSubtitle.setSpacingAfter(18);

      document.add(coverTitle);
      document.add(coverSubtitle);
      document.add(coverPeriodBanner(report));

      document.add(
        coverSummaryTable(report, filters, hasMetrics, hasIncidents)
      );
      if (!hasMetrics && !hasIncidents) {
        document.add(
          statusBox(t("report.empty.title"), t("report.empty.message"))
        );
      }

      document.newPage();
      // --- End Cover Page ---

      // Libelle "Incidents crees ..." precis selon la periodicite du rapport.
      String createdLabel = createdIncidentsLabel(report);
      // Suffixe de periode ("aujourd'hui" / "cette semaine" / "ce mois") pour les libelles de flux.
      String periodPhrase = statusFlowPeriodPhrase(report);
      // Titre date de la section activite ("Activite du jour / de la semaine / du mois").
      String activityTitle = statusActivityTitle(report);

      if (!groupedReports.isEmpty()) {
        addGroupedPdfSections(
          document,
          groupedReports,
          createdLabel,
          periodPhrase,
          activityTitle
        );
      } else if (isIncidentTypeAnalysis(metrics)) {
        addIncidentTypePdfContent(document, metrics);
      } else if (isIncidentStatusOverview(metrics)) {
        addIncidentStatusPdfContent(
          document,
          metrics,
          createdLabel,
          periodPhrase,
          activityTitle
        );
      } else {
        document.add(sectionTitle(t("report.summary.title")));
        if (!hasMetrics) {
          document.add(
            statusBox(
              t("report.summary.no_synthetic"),
              t("report.summary.no_metrics")
            )
          );
        } else {
          addOperationalSummary(document, metrics, createdLabel);
          addMetricDetailTables(document, metrics);
        }

        document.add(sectionTitle(t("report.incidents.title")));
        if (
          hasIncidents &&
          metrics.get("incidentsList") instanceof Collection<?> list
        ) {
          document.add(incidentsListTable(list));
        } else {
          document.add(note(t("report.incidents.none")));
        }
      }

      addAppliedFilters(document, filters);

      document.close();
      return out.toByteArray();
    } catch (Exception e) {
      log.error("Échec de generer le rapport PDF {}", report.getId(), e);
      throw new BusinessRuleViolationException(
        ErrorCode.EXPORT_ERROR,
        t("reporting.error.generate_pdf")
      );
    }
  }

  // Ajoute un titre de section au document PDF.

  private PdfPTable sectionTitle(String text) {
    PdfPTable table = new PdfPTable(1);
    table.setWidthPercentage(100);
    table.setSpacingBefore(18);
    table.setSpacingAfter(10);
    Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, NAVY);
    PdfPCell cell = new PdfPCell(new Phrase(text, font));
    cell.setBackgroundColor(new Color(246, 248, 251));
    cell.setPaddingTop(10);
    cell.setPaddingBottom(10);
    cell.setPaddingLeft(12);
    cell.setBorder(PdfPCell.LEFT | PdfPCell.BOTTOM);
    cell.setBorderColor(GOLD);
    cell.setBorderWidthLeft(4f);
    cell.setBorderWidthBottom(0.8f);
    table.addCell(cell);
    return table;
  }

  // Ajoute un sous-titre de section au document PDF.

  private Paragraph subSectionTitle(String text) {
    Font markerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, GOLD);
    Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, NAVY);
    Paragraph p = new Paragraph();
    p.add(new Phrase("|  ", markerFont));
    p.add(new Phrase(text, font));
    // Le sous-titre "colle" au tableau qui le suit : large espace au-dessus
    // (separation du bloc precedent) et faible espace en-dessous (rattachement
    // visuel au tableau), pour qu'on voie qu'ils forment une meme unite.
    p.setSpacingBefore(16);
    p.setSpacingAfter(6);
    return p;
  }

  // Maintient un sous-titre avec son graphique ou son tableau sur la meme page.
  private PdfPTable titledBlock(String title, Element... elements) {
    PdfPTable block = new PdfPTable(1);
    block.setWidthPercentage(100);
    block.setKeepTogether(true);
    block.setSpacingBefore(14);

    PdfPCell titleCell = new PdfPCell();
    titleCell.setBorder(PdfPCell.NO_BORDER);
    titleCell.setPadding(0);
    // Petit interligne sous le sous-titre avant le tableau, sans le detacher.
    titleCell.setPaddingBottom(3);
    titleCell.addElement(subSectionTitle(title));
    block.addCell(titleCell);

    for (Element element : elements) {
      PdfPCell contentCell = new PdfPCell();
      contentCell.setBorder(PdfPCell.NO_BORDER);
      contentCell.setPaddingLeft(0);
      contentCell.setPaddingRight(0);
      contentCell.setPaddingTop(0);
      // Espace sous les notes/descriptions avant le tableau suivant.
      contentCell.setPaddingBottom(element instanceof PdfPTable ? 0 : 8);
      if (element instanceof Image image) {
        image.setAlignment(Element.ALIGN_CENTER);
      }
      contentCell.addElement(element);
      block.addCell(contentCell);
    }
    return block;
  }

  // Ajoute une note explicative au document PDF.

  private Paragraph note(String text) {
    Font noteFont = FontFactory.getFont(
      FontFactory.HELVETICA_OBLIQUE,
      10,
      Color.GRAY
    );
    Paragraph note = new Paragraph(text, noteFont);
    note.setSpacingBefore(6);
    return note;
  }

  // Ajoute un encart de statut au document PDF.

  private PdfPTable statusBox(String title, String message) {
    PdfPTable table = new PdfPTable(1);
    table.setWidthPercentage(100);
    table.setSpacingBefore(12);
    table.setSpacingAfter(10);

    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, DARK);
    Font messageFont = FontFactory.getFont(
      FontFactory.HELVETICA,
      10,
      Color.GRAY
    );
    PdfPCell cell = new PdfPCell();
    cell.setBackgroundColor(SLATE_LIGHT);
    cell.setBorderColor(NAVY);
    cell.setBorderWidth(1.1f);
    cell.setPadding(12);
    Paragraph heading = new Paragraph(title, titleFont);
    heading.setSpacingAfter(6);
    cell.addElement(heading);
    cell.addElement(new Paragraph(message, messageFont));
    table.addCell(cell);
    return table;
  }

  // Construit le tableau de synthese de la page de garde.
  private PdfPTable coverSummaryTable(
    GeneratedReport report,
    Map<String, Object> filters,
    boolean hasMetrics,
    boolean hasIncidents
  ) {
    Map<String, Object> rows = new LinkedHashMap<>();
    rows.put(t("report.field.type"), reportTypeLabel(report));
    Object scopeLabel = filters.get("scopeLabel");
    if (scopeLabel instanceof String label && !label.isBlank()) {
      rows.put(t("report.field.perimeter"), label);
    }
    rows.put(t("report.field.generation"), generationLabel(report));
    rows.put(t("report.field.format"), reportFormatLabel(report));
    rows.put(t("report.field.status"), reportStatusLabel(report.getStatus()));
    rows.put(
      t("report.field.generatedAt"),
      report.getCreatedAt() != null
        ? report.getCreatedAt().format(DATE_FMT)
        : "-"
    );
    rows.put(t("report.field.createdBy"), displayCreator(report, filters));
    rows.put(
      t("report.cover.data_status"),
      hasMetrics || hasIncidents
        ? t("report.cover.data_available")
        : t("report.cover.no_data")
    );

    PdfPTable table = keyValueTable(rows);
    table.setWidthPercentage(72);
    table.setHorizontalAlignment(Element.ALIGN_CENTER);
    table.setSpacingBefore(12);
    table.setSpacingAfter(8);
    return table;
  }

  // Determine le createur affichable d'un rapport.

  private String displayCreator(
    GeneratedReport report,
    Map<String, Object> filters
  ) {
    Object label = filters.get("createdByLabel");
    if (label instanceof String value && !value.isBlank()) {
      return value;
    }
    if (report.getGenerationType() == ReportGenerationType.AUTOMATIC) {
      return t("report.creator.system");
    }
    return t("report.creator.unknown");
  }

  // Traduit le type fonctionnel du rapport.

  private String reportTypeLabel(GeneratedReport report) {
    return report.getType() != null ? t(report.getType().getNameKey()) : "-";
  }

  private String createdIncidentsLabel(GeneratedReport report) {
    String type = report.getType() != null ? report.getType().name() : "";
    switch (type) {
      case "DAILY":
        return t("report.statusOverview.created.daily");
      case "WEEKLY":
        return t("report.statusOverview.created.weekly");
      case "MONTHLY":
        return t("report.statusOverview.created.monthly");
      default:
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String start = report.getPeriodStart() != null
          ? report.getPeriodStart().toLocalDate().format(fmt)
          : "-";
        String end = report.getPeriodEnd() != null
          ? report.getPeriodEnd().toLocalDate().format(fmt)
          : "-";
        return t("report.statusOverview.created.custom", start, end);
    }
  }

  // Suffixe de periode pour les libelles de flux ("aujourd'hui", "cette semaine"...).
  private String statusFlowPeriodPhrase(GeneratedReport report) {
    String type = report.getType() != null ? report.getType().name() : "";
    return switch (type) {
      case "DAILY" -> t("report.statusOverview.flow.period.daily");
      case "WEEKLY" -> t("report.statusOverview.flow.period.weekly");
      case "MONTHLY" -> t("report.statusOverview.flow.period.monthly");
      default -> t("report.statusOverview.flow.period.custom");
    };
  }

  // Titre date de la section d'activite ("Activite du jour / de la semaine / du mois").
  private String statusActivityTitle(GeneratedReport report) {
    String type = report.getType() != null ? report.getType().name() : "";
    return switch (type) {
      case "DAILY" -> t("report.statusOverview.activity.title.daily");
      case "WEEKLY" -> t("report.statusOverview.activity.title.weekly");
      case "MONTHLY" -> t("report.statusOverview.activity.title.monthly");
      default -> t("report.statusOverview.activity.title.custom");
    };
  }

  // Extrait la sous-carte "situation globale" injectee par le service de rapport.
  private Map<String, Object> globalSituation(Map<String, Object> metrics) {
    if (metrics.get("globalSituation") instanceof Map<?, ?> raw) {
      Map<String, Object> situation = new LinkedHashMap<>();
      raw.forEach((key, value) -> situation.put(String.valueOf(key), value));
      return situation;
    }
    return Map.of();
  }

  // Traduit le format de sortie du rapport.

  private String reportFormatLabel(GeneratedReport report) {
    return report.getFormat() != null
      ? t(report.getFormat().getNameKey())
      : "-";
  }

  // Traduit le mode de generation du rapport.

  private String generationLabel(GeneratedReport report) {
    ReportGenerationType gen = report.getGenerationType();
    return gen != null ? t("report.gen." + gen.name().toLowerCase()) : "-";
  }

  // Verifie que les regles metier autorisent l operation sur piece jointe.

  private boolean hasIncidentRows(Map<String, Object> metrics) {
    return (
      metrics.get("incidentsList") instanceof Collection<?> list &&
      !list.isEmpty()
    );
  }

  // Ajoute une section PDF complete pour chaque entite du regroupement.
  private void addGroupedPdfSections(
    Document document,
    Collection<?> groups,
    String createdLabel,
    String periodPhrase,
    String activityTitle
  ) throws Exception {
    int index = 0;
    for (Object item : groups) {
      if (!(item instanceof Map<?, ?> group)) continue;
      if (index++ > 0) document.newPage();
      document.add(
        sectionTitle(
          group.get("label") != null ? String.valueOf(group.get("label")) : "-"
        )
      );
      Map<String, Object> metrics = groupMetrics(group);
      if (isIncidentTypeAnalysis(metrics)) {
        addIncidentTypePdfContent(document, metrics);
      } else if (isIncidentStatusOverview(metrics)) {
        addIncidentStatusPdfContent(
          document,
          metrics,
          createdLabel,
          periodPhrase,
          activityTitle
        );
      } else {
        addOperationalSummary(document, metrics, createdLabel);
        addMetricDetailTables(document, metrics);
        document.add(sectionTitle(t("report.incidents.title")));
        if (
          metrics.get("incidentsList") instanceof Collection<?> incidents &&
          !incidents.isEmpty()
        ) {
          document.add(incidentsListTable(incidents));
        } else {
          document.add(note(t("report.incidents.none")));
        }
      }
    }
  }

  // Met en evidence la periode analysee sur la page de garde.
  private PdfPTable coverPeriodBanner(GeneratedReport report) {
    PdfPTable table = new PdfPTable(1);
    table.setWidthPercentage(72);
    table.setHorizontalAlignment(Element.ALIGN_CENTER);
    table.setSpacingAfter(18);
    Font font = FontFactory.getFont(
      FontFactory.HELVETICA_BOLD,
      11,
      Color.WHITE
    );
    PdfPCell cell = new PdfPCell(
      new Phrase(t("report.period") + " " + formatPeriod(report), font)
    );
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setBackgroundColor(NAVY);
    cell.setBorderColor(NAVY);
    cell.setPadding(10);
    table.addCell(cell);
    return table;
  }

  // Ajoute la synthese et les listes detaillees d'un rapport par type.
  private void addIncidentTypePdfContent(
    Document document,
    Map<String, Object> metrics
  ) throws Exception {
    document.add(sectionTitle(t("report.typeAnalysis.title")));
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put(
      t("report.metric.totalIncidents"),
      metrics.getOrDefault("totalIncidents", 0)
    );
    summary.put(
      t("report.metric.representedTypeCount"),
      metrics.getOrDefault("representedTypeCount", 0)
    );
    summary.put(
      t("report.metric.dominantType"),
      metrics.getOrDefault("dominantType", "-")
    );
    summary.put(
      t("report.metric.dominantTypeShare"),
      formatPercentage(metrics.get("dominantTypeShare"))
    );
    document.add(kpiCardsTable(summary));

    if (metrics.get("typeSections") instanceof Collection<?> sections) {
      int chartIndex = 0;
      for (Image chart : createTypeDistributionCharts(sections)) {
        document.add(
          titledBlock(
            chartIndex++ == 0
              ? t("report.metric.distributionByType")
              : t("report.metric.distributionByType.continued"),
            chart
          )
        );
      }
      document.add(typeSummaryTable(sections));
      for (Object item : sections) {
        if (!(item instanceof Map<?, ?> section)) continue;
        int incidentCount = (int) numberValue(section.get("count"));
        document.add(
          subSectionTitle(
            t(
              incidentCount == 1
                ? "report.typeAnalysis.sectionTitle.one"
                : "report.typeAnalysis.sectionTitle.many",
              mapValue(section, "typeName"),
              incidentCount,
              formatPercentage(section.get("share"))
            )
          )
        );
        if (
          section.get("incidents") instanceof Collection<?> incidents &&
          !incidents.isEmpty()
        ) {
          document.add(incidentsListTable(incidents));
        }
      }
    }
  }

  // Construit le tableau comparatif des volumes et parts par type.
  private PdfPTable typeSummaryTable(Collection<?> sections) {
    PdfPTable table = simpleTable(new String[] {
      t("report.table.type"),
      t("report.table.incidents"),
      t("report.table.share"),
      t("report.metric.activeIncidents"),
      t("report.metric.closedIncidents"),
      t("report.metric.rejectedIncidents"),
    });
    for (Object item : sections) {
      if (!(item instanceof Map<?, ?> section)) continue;
      addCells(
        table,
        mapValue(section, "typeName"),
        mapValue(section, "count"),
        formatPercentage(section.get("share")),
        mapValue(section, "active"),
        mapValue(section, "closed"),
        mapValue(section, "rejected")
      );
    }
    return table;
  }

  // Indique si les metriques suivent le modele thematique par type.
  private boolean isIncidentTypeAnalysis(Map<String, Object> metrics) {
    return "INCIDENT_TYPE_ANALYSIS".equals(
      String.valueOf(metrics.get("reportContentType"))
    );
  }

  // Indique si les metriques suivent le modele "situation par statut".
  private boolean isIncidentStatusOverview(Map<String, Object> metrics) {
    return "INCIDENT_STATUS_OVERVIEW".equals(
      String.valueOf(metrics.get("reportContentType"))
    );
  }

  // Ajoute la synthese "situation par statut" : synthese en tete, section des
  // retards a surveiller, puis un bloc insecable (titre + phrase + tableau) par
  // famille non vide, pour que chaque incident reste rattache a sa famille.
  private void addIncidentStatusPdfContent(
    Document document,
    Map<String, Object> metrics,
    String createdLabel,
    String periodPhrase,
    String activityTitle
  ) throws Exception {
    Map<String, Object> situation = globalSituation(metrics);

    // --- 1. Activite de la periode (du jour / de la semaine / du mois) ---
    document.add(sectionTitle(activityTitle));
    Map<String, Object> activity = new LinkedHashMap<>();
    activity.put(createdLabel, metrics.getOrDefault("totalIncidents", 0));
    activity.put(
      t("report.statusOverview.flow.treated", periodPhrase),
      situation.getOrDefault("treatedInPeriod", 0)
    );
    activity.put(
      t("report.statusOverview.flow.resolved", periodPhrase),
      situation.getOrDefault("resolvedInPeriod", 0)
    );
    activity.put(
      t("report.statusOverview.flow.closed", periodPhrase),
      situation.getOrDefault("closedInPeriod", 0)
    );
    document.add(kpiCardsTable(activity));
    // Chaque compteur de la periode adosse a UNE liste plate, meme format : creations,
    // puis les trois flux. Pas de decomposition par famille ici (la famille = statut
    // courant, pertinente uniquement pour la situation globale ci-dessous). Un incident
    // cree, traite et resolu le meme jour apparait donc dans les trois listes concernees.
    renderPeriodList(
      document,
      metrics.get("incidentsList"),
      "report.statusOverview.flow.created.list",
      "report.statusOverview.flow.created.list.hint",
      periodPhrase
    );
    renderPeriodList(
      document,
      situation.get("treatedList"),
      "report.statusOverview.flow.treated.list",
      "report.statusOverview.flow.treated.list.hint",
      periodPhrase
    );
    renderPeriodList(
      document,
      situation.get("resolvedList"),
      "report.statusOverview.flow.resolved.list",
      "report.statusOverview.flow.resolved.list.hint",
      periodPhrase
    );
    renderPeriodList(
      document,
      situation.get("closedList"),
      "report.statusOverview.flow.closed.list",
      "report.statusOverview.flow.closed.list.hint",
      periodPhrase
    );

    // --- 2. Situation globale : tout ce qui reste ouvert, toutes periodes confondues ---
    document.add(sectionTitle(t("report.statusOverview.global.title")));
    document.add(note(t("report.statusOverview.global.hint")));
    Map<String, Object> global = new LinkedHashMap<>();
    global.put(
      t("report.statusOverview.global.untreated"),
      situation.getOrDefault("untreatedBacklog", 0)
    );
    global.put(
      t("report.statusOverview.global.overdue"),
      situation.getOrDefault("overdueBacklog", 0)
    );
    document.add(kpiCardsTable(global));
    // Le backlog courant liste par famille (avec les retards), pour voir quoi traiter.
    renderStatusFamilies(document, situation);
  }

  // Rend la liste plate adossant un compteur de la periode (crees/traites/resolus/clotures) :
  // bloc insecable titre + phrase + tableau, omis si la population est vide.
  private void renderPeriodList(
    Document document,
    Object data,
    String titleKey,
    String hintKey,
    String periodPhrase
  ) throws Exception {
    if (!(data instanceof Collection<?> incidents) || incidents.isEmpty()) {
      return;
    }
    String title = t(titleKey, periodPhrase, incidents.size());
    document.add(
      titledBlock(title, note(t(hintKey)), statusIncidentsTable(incidents))
    );
  }

  // Rend la repartition par famille d'une population : tableau de synthese, section
  // des retards, puis un bloc insecable (titre + phrase + tableau) par famille non vide.
  private void renderStatusFamilies(
    Document document,
    Map<String, Object> metrics
  ) throws Exception {
    if (metrics.get("categorySections") instanceof Collection<?> sections) {
      document.add(statusSummaryTable(sections));
    }
    if (metrics.get("categorySections") instanceof Collection<?> sections) {
      for (Object item : sections) {
        if (!(item instanceof Map<?, ?> section)) continue;
        int count = (int) numberValue(section.get("count"));
        if (count == 0) continue;
        String key = mapValue(section, "key");
        String title = t(
          "report.statusOverview.sectionTitle",
          categoryLabel(key),
          count,
          formatPercentage(section.get("share"))
        );
        if (
          section.get("incidents") instanceof Collection<?> incidents &&
          !incidents.isEmpty()
        ) {
          document.add(
            titledBlock(
              title,
              note(categoryHint(key)),
              statusIncidentsTable(incidents)
            )
          );
        } else {
          document.add(titledBlock(title, note(categoryHint(key))));
        }
      }
    }
  }

  // Extrait les incidents marques en retard depuis la population du rapport.
  private List<Map<String, Object>> overdueIncidents(
    Map<String, Object> metrics
  ) {
    List<Map<String, Object>> overdue = new ArrayList<>();
    if (metrics.get("incidentsList") instanceof Collection<?> all) {
      for (Object item : all) {
        if (
          item instanceof Map<?, ?> row &&
          Boolean.TRUE.equals(row.get("overdue"))
        ) {
          @SuppressWarnings("unchecked")
          Map<String, Object> typed = (Map<String, Object>) row;
          overdue.add(typed);
        }
      }
    }
    return overdue;
  }

  // Tableau recapitulatif : une ligne par famille (volume et part).
  private PdfPTable statusSummaryTable(Collection<?> sections) {
    PdfPTable table = simpleTable(new String[] {
      t("report.statusOverview.table.category"),
      t("report.table.incidents"),
      t("report.table.share"),
    });
    for (Object item : sections) {
      if (!(item instanceof Map<?, ?> section)) continue;
      addCells(
        table,
        categoryLabel(mapValue(section, "key")),
        mapValue(section, "count"),
        formatPercentage(section.get("share"))
      );
    }
    return table;
  }

  // Liste d'incidents par statut (reference, titre, type, agence, signalant, traiteur/assigne, statut+date, retard).
  private PdfPTable statusIncidentsTable(Collection<?> incidents) {
    PdfPTable table = simpleTable(new String[] {
      t("report.table.reference"),
      t("report.table.title"),
      t("report.table.type"),
      t("report.table.agency"),
      t("report.table.creator"),
      t("report.table.assignee"),
      t("report.statusOverview.statusAndCreated"),
      t("report.statusOverview.overdueColumn"),
    });
    try {
      table.setWidths(new int[] { 3, 4, 3, 3, 3, 3, 3, 2 });
    } catch (Exception ignored) {}
    for (Object item : incidents) {
      if (!(item instanceof Map<?, ?> row)) continue;
      addCells(
        table,
        dash(mapValue(row, "reference")),
        dash(mapValue(row, "title")),
        dash(
          firstNonBlank(
            nestedValue(row, "type", "displayName"),
            nestedValue(row, "type", "name")
          )
        ),
        dash(nestedValue(row, "agency", "name")),
        dash(personName(row.get("createdBy"))),
        dash(incidentAssignmentPerson(row)),
        statusAndCreated(row),
        Boolean.TRUE.equals(row.get("overdue"))
          ? t("report.statusOverview.overdue.yes")
          : t("report.statusOverview.overdue.no")
      );
    }
    return table;
  }


  private String statusAndCreated(Map<?, ?> row) {
    String status = translateDimensionValue(
      "distributionByStatus",
      mapValue(row, "status")
    );
    return joinLines(status, formatDateValue(row.get("createdAt")));
  }

  // Libelle traduit d'une famille de statut.
  private String categoryLabel(String key) {
    return t("report.statusOverview.category." + key + ".label");
  }

  // Phrase explicative traduite d'une famille de statut.
  private String categoryHint(String key) {
    return t("report.statusOverview.category." + key + ".hint");
  }

  // Remplace une valeur vide par un tiret pour l'affichage tabulaire.
  private String dash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }

  // Formate une part numerique en pourcentage lisible.
  private String formatPercentage(Object value) {
    return value instanceof Number number
      ? String.format(
          LocaleContextHolder.getLocale(),
          "%.1f%%",
          number.doubleValue()
        )
      : "0,0%";
  }

  // Retourne la collection de sous-rapports stockee dans les metriques.
  private Collection<?> groupedReports(Map<String, Object> metrics) {
    return metrics.get("groupedReports") instanceof Collection<?> groups
      ? groups
      : List.of();
  }

  // Extrait les metriques typees d'une section groupee.
  private Map<String, Object> groupMetrics(Object value) {
    if (
      !(value instanceof Map<?, ?> group) ||
      !(group.get("metrics") instanceof Map<?, ?> rawMetrics)
    ) {
      return Map.of();
    }
    Map<String, Object> metrics = new LinkedHashMap<>();
    rawMetrics.forEach((key, metric) ->
      metrics.put(String.valueOf(key), metric)
    );
    return normalizeMilestoneKeys(metrics);
  }

  // Relit un rapport anterieur a la separation cloture/resolution sous les cles actuelles.
  private Map<String, Object> normalizeMilestoneKeys(
    Map<String, Object> metrics
  ) {
    if (
      metrics.containsKey("avgClosureHours") ||
      !metrics.containsKey("avgResolutionHours")
    ) {
      return metrics;
    }
    Map<String, Object> normalized = new LinkedHashMap<>();
    metrics.forEach((key, value) ->
      normalized.put(LEGACY_CLOSURE_KEYS.getOrDefault(key, key), value)
    );
    return normalized;
  }

  // Verifie que les regles metier autorisent l operation sur piece jointe.

  private boolean hasDisplayableMetrics(Map<String, Object> metrics) {
    if (metrics == null || metrics.isEmpty()) {
      return false;
    }
    return metrics
      .entrySet()
      .stream()
      .filter(entry -> !"incidentsList".equals(entry.getKey()))
      .anyMatch(entry -> hasDisplayableValue(entry.getValue()));
  }

  // Verifie que les regles metier autorisent l operation sur piece jointe.

  private boolean hasDisplayableValue(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof Collection<?> collection) {
      return !collection.isEmpty();
    }
    if (value instanceof Map<?, ?> map) {
      return !map.isEmpty();
    }
    return true;
  }

  // Construit le tableau des filtres appliques au rapport.

  private PdfPTable filtersTable(Map<String, Object> filters) {
    Map<String, Object> rows = flattenFilters(filters);
    if (rows.isEmpty()) {
      return null;
    }
    return keyValueTable(rows);
  }

  // Ajoute en annexe uniquement les filtres fonctionnels effectivement appliques.
  private void addAppliedFilters(Document document, Map<String, Object> filters)
    throws Exception {
    PdfPTable table = filtersTable(filters);
    if (table == null) {
      return;
    }
    document.add(sectionTitle(t("report.filters.title")));
    document.add(table);
  }

  // Construit un tableau cle-valeur pour le PDF.

  private PdfPTable keyValueTable(Map<String, Object> rows) {
    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100);
    try {
      table.setWidths(new int[] { 2, 3 });
    } catch (Exception ignored) {
      // Conserve les largeurs par defaut si iText refuse la configuration.
    }
    Font keyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, NAVY);
    Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, DARK);
    for (Map.Entry<String, Object> entry : rows.entrySet()) {
      PdfPCell keyCell = new PdfPCell(new Phrase(entry.getKey(), keyFont));
      keyCell.setBackgroundColor(new Color(235, 240, 246));
      keyCell.setBorderColor(new Color(211, 219, 229));
      keyCell.setPadding(8);
      keyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
      keyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
      table.addCell(keyCell);

      PdfPCell valueCell = new PdfPCell(
        new Phrase(
          entry.getValue() != null ? entry.getValue().toString() : "-",
          valueFont
        )
      );
      valueCell.setBackgroundColor(Color.WHITE);
      valueCell.setBorderColor(new Color(225, 229, 235));
      valueCell.setPadding(8);
      valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
      valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
      table.addCell(valueCell);
    }
    return table;
  }

  // Structure la synthese en distinguant flux, photographie actuelle et performance.
  private void addOperationalSummary(
    Document document,
    Map<String, Object> metrics,
    String createdLabel
  ) throws Exception {
    addMetricGroup(
      document,
      t("report.summary.period_activity"),
      metrics,
      periodFlowMetricKeys(metrics),
      createdLabel
    );
    addMetricGroup(
      document,
      t("report.summary.current_snapshot"),
      metrics,
      CURRENT_SNAPSHOT_METRICS,
      createdLabel
    );
    addMetricGroup(
      document,
      t("report.summary.performance"),
      metrics,
      PERFORMANCE_METRICS,
      createdLabel
    );
    addMetricGroup(
      document,
      t("report.summary.personal_activity"),
      metrics,
      PERSONAL_METRICS,
      createdLabel
    );
  }

  // Retourne les metriques de flux avec compatibilite pour les anciens rapports.
  private List<String> periodFlowMetricKeys(Map<String, Object> metrics) {
    List<String> keys = new ArrayList<>(PERIOD_FLOW_METRICS);
    if (!metrics.containsKey("inflow") && metrics.containsKey("totalIncidents")) {
      keys.set(0, "totalIncidents");
    }
    if (!metrics.containsKey("outflow") && metrics.containsKey("closedIncidents")) {
      keys.set(1, "closedIncidents");
    }
    return keys;
  }

  // Ajoute un groupe de cartes en masquant les metriques absentes.
  private void addMetricGroup(
    Document document,
    String title,
    Map<String, Object> metrics,
    Collection<String> keys,
    String createdLabel
  ) throws Exception {
    Map<String, Object> rows = new LinkedHashMap<>();
    keys.forEach(key -> {
      if (metrics.containsKey(key)) {
        rows.put(
          flowMetricLabel(key, createdLabel),
          formatMetricValue(key, metrics.get(key), metrics)
        );
      }
    });
    if (rows.isEmpty()) {
      return;
    }
    document.add(titledBlock(title, kpiCardsTable(rows)));
  }

  // Rangee de KPI sous forme de cartes (look executive summary).
  private PdfPTable kpiCardsTable(Map<String, Object> rows) {
    int columns = Math.min(4, Math.max(1, rows.size()));
    PdfPTable table = new PdfPTable(columns);
    table.setWidthPercentage(100);
    table.setSpacingBefore(4);
    table.setSpacingAfter(6);
    Font labelFont = FontFactory.getFont(
      FontFactory.HELVETICA_BOLD,
      8,
      new Color(92, 92, 92)
    );
    for (Map.Entry<String, Object> entry : rows.entrySet()) {
      PdfPCell cell = new PdfPCell();
      cell.setPadding(10);
      cell.setBorderWidth(1);
      cell.setBorderColor(new Color(202, 162, 46));
      cell.setBackgroundColor(new Color(249, 250, 252));
      Paragraph label = new Paragraph(entry.getKey(), labelFont);
      label.setAlignment(Element.ALIGN_CENTER);
      label.setSpacingAfter(6);
      cell.addElement(label);
      String valueText = entry.getValue() != null
        ? entry.getValue().toString()
        : "-";
      Font valueFont = FontFactory.getFont(
        FontFactory.HELVETICA_BOLD,
        valueText.length() > 12 ? 14 : 17,
        NAVY
      );
      Paragraph value = new Paragraph(valueText, valueFont);
      value.setAlignment(Element.ALIGN_CENTER);
      cell.addElement(value);
      table.addCell(cell);
    }
    int remainder = columns - (rows.size() % columns);
    if (remainder != columns) {
      for (int k = 0; k < remainder; k++) {
        PdfPCell filler = new PdfPCell(new Phrase(""));
        filler.setBorder(0);
        table.addCell(filler);
      }
    }
    return table;
  }

  // Construit le tableau des incidents inclus dans le rapport.

  private PdfPTable incidentsListTable(Collection<?> incidents) {
    PdfPTable table = simpleTable(new String[] {
      t("report.table.incident"),
      t("report.table.creator"),
      t("report.table.type") + " / " + t("report.table.agency"),
      t("report.table.service") + " / " + t("report.table.assignee"),
      t("report.table.status") + " / " + t("report.table.criticality"),
      t("report.table.key_dates"),
    });
    try {
      table.setWidths(new int[] { 4, 2, 3, 3, 2, 3 });
    } catch (Exception ignored) {}

    for (Object item : incidents) {
      if (item instanceof Map<?, ?> row) {
        addCells(
          table,
          incidentTitle(row),
          personName(row.get("createdBy")),
          incidentTypeAndAgency(row),
          incidentAssignment(row),
          incidentStatusAndCriticality(row),
          incidentDates(row)
        );
        if (hasIncidentBusinessDetails(row)) {
          PdfPCell detailCell = new PdfPCell(
            new Phrase(
              incidentBusinessDetails(row),
              FontFactory.getFont(FontFactory.HELVETICA, 8, DARK)
            )
          );
          detailCell.setColspan(6);
          detailCell.setPadding(6);
          detailCell.setHorizontalAlignment(Element.ALIGN_CENTER);
          detailCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
          detailCell.setBackgroundColor(new Color(252, 252, 252));
          detailCell.setBorderColor(new Color(230, 230, 230));
          table.addCell(detailCell);
        }
      }
    }
    return table;
  }

  // Cree un element du domaine rapport document generator apres validation metier.

  private void addMetricDetailTables(
    Document document,
    Map<String, Object> metrics
  ) throws Exception {
    addMapMetricTable(
      document,
      "distributionByType",
      metrics.get("distributionByType")
    );
    addMapMetricTable(
      document,
      "distributionByCriticality",
      metrics.get("distributionByCriticality")
    );
    addMapMetricTable(
      document,
      "distributionByStatus",
      metrics.get("distributionByStatus")
    );
    addMapMetricTable(
      document,
      "ageDistribution",
      metrics.get("ageDistribution")
    );
    addMapMetricTable(
      document,
      "cohortOutcome",
      metrics.get("cohortOutcome")
    );
    addNamedCountTable(
      document,
      t("report.metric.workload"),
      metrics.get("workload")
    );
    addNamedDurationTable(
      document,
      t("report.metric.closureHoursByType"),
      metrics.get("closureHoursByType")
    );
    addNamedDurationTable(
      document,
      t("report.metric.closureHoursByCriticality"),
      metrics.get("closureHoursByCriticality")
    );
    addCohortCompletionTable(document, metrics.get("cohortCompletion"));
    addScorecardTable(document, metrics.get("efficiencyScorecard"));
    addTopServicesTable(document, metrics.get("topServices"));
    addRankingCountTable(
      document,
      t("report.metric.topAgencies"),
      metrics.get("topAgencies")
    );
    addRankingCountTable(
      document,
      t("report.metric.topResolvers"),
      metrics.get("topResolvers")
    );
    addMonthlySeriesTable(
      document,
      t("report.metric.monthlyClosures"),
      metrics.get("monthlyClosures"),
      t("report.table.incidents")
    );
    addMonthlySeriesTable(
      document,
      t("report.metric.monthlyAvgClosureHours"),
      metrics.get("monthlyAvgClosureHours"),
      t("report.table.hours")
    );
  }

  // Cree un element du domaine rapport document generator apres validation metier.

  private void addNamedCountTable(Document document, String title, Object value)
    throws Exception {
    if (!(value instanceof Collection<?> rows) || rows.isEmpty()) {
      return;
    }
    PdfPTable table = simpleTable(new String[] {
      t("report.table.label"),
      t("report.table.incidents"),
    });
    for (Object row : rows) {
      if (row instanceof Map<?, ?> entry) {
        addCells(table, mapValue(entry, "name"), mapValue(entry, "count"));
      }
    }
    document.add(titledBlock(title, table));
  }

  // Delai moyen par categorie : la moyenne reste lisible grace a l'effectif affiche.
  private void addNamedDurationTable(
    Document document,
    String title,
    Object value
  )
    throws Exception {
    if (!(value instanceof Collection<?> rows) || rows.isEmpty()) {
      return;
    }
    PdfPTable table = simpleTable(new String[] {
      t("report.table.label"),
      t("report.table.hours"),
      t("report.table.incidents"),
    });
    for (Object row : rows) {
      if (row instanceof Map<?, ?> entry) {
        addCells(
          table,
          mapValue(entry, "name"),
          formatHours(entry.get("avgHours")),
          mapValue(entry, "sampleSize")
        );
      }
    }
    document.add(titledBlock(title, table));
  }

  // Affiche le rang et rend explicites les egalites de compte.
  private void addRankingCountTable(
    Document document,
    String title,
    Object value
  )
    throws Exception {
    List<Map<?, ?>> rows = rankingRows(value);
    if (rows.isEmpty()) {
      return;
    }
    PdfPTable table = simpleTable(new String[] {
      t("report.table.rank"),
      t("report.table.label"),
      t("report.table.incidents"),
    });
    for (int index = 0; index < rows.size(); index++) {
      Map<?, ?> row = rows.get(index);
      addCells(
        table,
        rankingPosition(rows, index),
        mapValue(row, "name"),
        mapValue(row, "count")
      );
    }
    document.add(titledBlock(title, table));
  }

  // Cree un element du domaine rapport document generator apres validation metier.

  // Convention cohorte : un percentile non atteint est annonce comme tel, jamais
  // remplace par la valeur des seuls incidents deja clotures.
  private void addCohortCompletionTable(Document document, Object value)
    throws Exception {
    if (!(value instanceof Map<?, ?> cohort) || cohort.isEmpty()) {
      return;
    }
    Map<String, Object> rows = new LinkedHashMap<>();
    rows.put(t("report.metric.cohort.size"), mapValue(cohort, "size"));
    rows.put(
      t("report.metric.cohort.closed"),
      mapValue(cohort, "closedCount")
    );
    rows.put(
      t("report.metric.cohort.p50"),
      cohortPercentile(cohort, "p50Hours", "p50Reached")
    );
    rows.put(
      t("report.metric.cohort.p90"),
      cohortPercentile(cohort, "p90Hours", "p90Reached")
    );
    rows.put(
      t("report.metric.cohort.open_age"),
      formatHours(cohort.get("openMedianAgeHours"))
    );
    document.add(
      titledBlock(t("report.metric.cohortCompletion"), keyValueTable(rows))
    );
  }

  // Rend explicite qu'un percentile n'a pas ete atteint par la cohorte.
  private String cohortPercentile(
    Map<?, ?> cohort,
    String valueKey,
    String reachedKey
  ) {
    String formatted = formatHours(cohort.get(valueKey));
    return Boolean.TRUE.equals(cohort.get(reachedKey))
      ? formatted
      : t("report.metric.cohort.not_reached", formatted);
  }

  private void addScorecardTable(Document document, Object value)
    throws Exception {
    if (!(value instanceof Map<?, ?> score) || score.isEmpty()) {
      return;
    }
    Map<String, Object> rows = new LinkedHashMap<>();
    rows.put(
      t("report.metric.scorecard.delay"),
      formatScore(score.get("delayScore"))
    );
    rows.put(
      t("report.metric.scorecard.quality"),
      formatScore(score.get("qualityScore"))
    );
    rows.put(
      t("report.metric.scorecard.throughput"),
      formatScore(score.get("throughputScore"))
    );
    rows.put(
      t("report.metric.scorecard.composite"),
      formatScore(score.get("compositeScore"))
    );
    document.add(
      titledBlock(
        t("report.metric.efficiencyScorecard"),
        keyValueTable(rows)
      )
    );
  }

  // Formate un score numerique pour l'affichage PDF.

  private String formatScore(Object value) {
    if (value instanceof Number n) {
      return String.format(
        LocaleContextHolder.getLocale(),
        "%.0f/100",
        n.doubleValue()
      );
    }
    return "-";
  }

  // Fournit month label a la couche appelante.

  private String getMonthLabel(int month) {
    if (month >= 1 && month <= 12) {
      return Month.of(month).getDisplayName(
        TextStyle.FULL,
        LocaleContextHolder.getLocale()
      );
    }
    return String.valueOf(month);
  }

  // Cree un element du domaine rapport document generator apres validation metier.

  private void addMonthlySeriesTable(
    Document document,
    String title,
    Object value,
    String valueHeader
  ) throws Exception {
    if (!(value instanceof Collection<?> rows) || rows.isEmpty()) {
      return;
    }
    Image chartImage = createBarChart(
      t("report.table.month"),
      valueHeader,
      rows
    );
    PdfPTable table = simpleTable(new String[] {
      t("report.table.month"),
      valueHeader,
    });
    for (Object row : rows) {
      if (row instanceof Map<?, ?> entry) {
        Object monthRaw = entry.get("month");
        int month = monthRaw instanceof Number n ? n.intValue() : 0;
        String monthLabel = getMonthLabel(month);
        Object v = entry.get("value");
        String valueLabel = formatMonthlyValue(title, v);
        addCells(table, monthLabel, valueLabel);
      }
    }
    document.add(
      chartImage != null
        ? titledBlock(title, chartImage, table)
        : titledBlock(title, table)
    );
  }

  // Cree un element du domaine rapport document generator apres validation metier.

  private void addMapMetricTable(
    Document document,
    String metricKey,
    Object value
  )
    throws Exception {
    if (!(value instanceof Map<?, ?> map) || map.isEmpty()) {
      return;
    }

    Map<String, Number> displayableValues = normalizedDistribution(
      metricKey,
      map
    );
    if (displayableValues.isEmpty()) {
      return;
    }

    String title = metricLabel(metricKey);

    Image chartImage = createDistributionBarChart(displayableValues);

    Map<String, Object> rows = new LinkedHashMap<>();
    displayableValues.forEach(rows::put);
    PdfPTable table = keyValueTable(rows);
    document.add(
      chartImage != null
        ? titledBlock(title, chartImage, table)
        : titledBlock(title, table)
    );
  }

  // Cree un element du domaine rapport document generator apres validation metier.

  private void addTopServicesTable(Document document, Object value)
    throws Exception {
    List<Map<?, ?>> services = rankingRows(value);
    if (services.isEmpty()) {
      return;
    }

    PdfPTable table = simpleTable(new String[] {
      t("report.table.rank"),
      t("report.table.service"),
      t("report.table.incidents"),
    });
    for (int index = 0; index < services.size(); index++) {
      Map<?, ?> row = services.get(index);
      addCells(
        table,
        rankingPosition(services, index),
        mapValue(row, "serviceName"),
        mapValue(row, "count")
      );
    }
    document.add(titledBlock(t("report.metric.topServices"), table));
  }

  // Normalise les lignes de classement recues depuis les services metier.
  private List<Map<?, ?>> rankingRows(Object value) {
    if (!(value instanceof Collection<?> rows)) {
      return List.of();
    }
    List<Map<?, ?>> rankingRows = new ArrayList<>();
    for (Object row : rows) {
      if (row instanceof Map<?, ?> rankingRow) {
        rankingRows.add(rankingRow);
      }
    }
    return rankingRows;
  }

  // Calcule un rang de competition et signale les comptes identiques.
  private String rankingPosition(List<Map<?, ?>> rows, int index) {
    long count = rankingCount(rows.get(index));
    int rank = index + 1;
    for (int previous = 0; previous < index; previous++) {
      if (rankingCount(rows.get(previous)) == count) {
        rank = previous + 1;
        break;
      }
    }
    boolean tied =
      rows.stream().filter(row -> rankingCount(row) == count).count() > 1;
    return tied
      ? rank + " " + t("report.table.tied")
      : Integer.toString(rank);
  }

  // Lit le nombre d'incidents d'une ligne de classement.
  private long rankingCount(Map<?, ?> row) {
    Object count = row.get("count");
    if (count instanceof Number number) {
      return number.longValue();
    }
    try {
      return count == null ? 0L : Long.parseLong(count.toString());
    } catch (NumberFormatException ignored) {
      return 0L;
    }
  }

  // Convertit les donnees du domaine rapport document generator entre les modeles utilises.

  private String mapValue(Map<?, ?> row, String key) {
    Object value = row.get(key);
    return value != null ? value.toString() : "-";
  }

  // Lit une valeur imbriquee dans les donnees du rapport.

  private String nestedValue(
    Map<?, ?> row,
    String objectKey,
    String nestedKey
  ) {
    Object nested = row.get(objectKey);
    if (nested instanceof Map<?, ?> map) {
      Object value = map.get(nestedKey);
      return value != null ? value.toString() : "-";
    }
    return "-";
  }

  // Retourne la premiere valeur non vide disponible.

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank() && !"-".equals(value)) {
        return value;
      }
    }
    return "-";
  }

  // Determine le titre affichable d'un incident.

  private String incidentTitle(Map<?, ?> row) {
    String ref = mapValue(row, "reference");
    String title = mapValue(row, "title");
    if (!"-".equals(ref) && !"-".equals(title)) {
      return ref + " - " + title;
    }
    return firstNonBlank(ref, title, mapValue(row, "id"));
  }

  // Formate le nom de l'intervenant/assigne selon le statut de l'incident.
  private String incidentAssignmentPerson(Map<?, ?> row) {
    String person = personName(row.get("assignedTo"));
    if ("-".equals(person)) {
      return "-";
    }
    String status = mapValue(row, "status").toUpperCase(Locale.ROOT);
    if (
      "TREATED".equals(status) ||
      "RESOLVED".equals(status) ||
      "VALIDATED".equals(status) ||
      "CLOSED".equals(status)
    ) {
      return t("report.table.treated_by_prefix") + " " + person;
    }
    return t("report.table.assigned_to_prefix") + " " + person;
  }

  // Formate le type et l'agence d'un incident.

  private String incidentTypeAndAgency(Map<?, ?> row) {
    String type = firstNonBlank(
      nestedValue(row, "type", "displayName"),
      nestedValue(row, "type", "name")
    );
    String agency = nestedValue(row, "agency", "name");
    return joinLines(type, agency);
  }

  // Formate les informations d'affectation d'un incident.

  private String incidentAssignment(Map<?, ?> row) {
    String service = firstNonBlank(
      nestedValue(row, "transferredToService", "name"),
      mapValue(row, "serviceName")
    );
    return joinLines(service, incidentAssignmentPerson(row));
  }

  // Formate le statut et la criticite d'un incident.

  private String incidentStatusAndCriticality(Map<?, ?> row) {
    return joinLines(
      translateDimensionValue("distributionByStatus", mapValue(row, "status")),
      translateDimensionValue(
        "distributionByCriticality",
        mapValue(row, "criticality")
      )
    );
  }

  // Assemble les lignes non vides d'un bloc texte.

  private String joinLines(String first, String second) {
    String left = firstNonBlank(first);
    String right = firstNonBlank(second);
    if ("-".equals(left)) {
      return right;
    }
    if ("-".equals(right)) {
      return left;
    }
    return left + "\n" + right;
  }

  // Construit le nom affichable d'une personne.

  private String personName(Object value) {
    if (value instanceof Map<?, ?> user) {
      String fullName = firstNonBlank(
        joinNames(user.get("firstName"), user.get("lastName")),
        objectToString(user.get("username")),
        objectToString(user.get("email"))
      );
      return firstNonBlank(fullName, objectToString(user.get("id")));
    }
    return objectToString(value);
  }

  // Assemble une liste de noms affichables.

  private String joinNames(Object firstName, Object lastName) {
    String first = objectToString(firstName);
    String last = objectToString(lastName);
    String joined = (
      (!"-".equals(first) ? first : "") +
      " " +
      (!"-".equals(last) ? last : "")
    ).trim();
    return joined.isBlank() ? "-" : joined;
  }

  // Convertit une valeur inconnue en chaine lisible.

  private String objectToString(Object value) {
    return value != null && !value.toString().isBlank()
      ? value.toString()
      : "-";
  }

  // Formate les dates principales d'un incident.

  private String incidentDates(Map<?, ?> row) {
    Map<String, String> dates = new LinkedHashMap<>();
    dates.put(
      t("report.table.created_at"),
      formatDateValue(row.get("createdAt"))
    );
    dates.put(
      t("report.table.incident_date"),
      formatDateValue(row.get("incidentDate"))
    );
    dates.put(t("report.table.due_date"), formatDateValue(row.get("dueDate")));
    dates.put(
      t("report.table.resolved_at"),
      formatDateValue(row.get("resolvedAt"))
    );
    return dates
      .entrySet()
      .stream()
      .filter(entry -> !"-".equals(entry.getValue()))
      .map(entry -> entry.getKey() + ": " + entry.getValue())
      .reduce((left, right) -> left + "\n" + right)
      .orElse("-");
  }

  // Formate les details metier d'un incident.

  private String incidentBusinessDetails(Map<?, ?> row) {
    Map<String, String> details = new LinkedHashMap<>();
    details.put(
      t("report.table.cause"),
      firstNonBlank(mapValue(row, "cause"), mapValue(row, "causeDetail"))
    );
    details.put(t("report.table.description"), mapValue(row, "description"));
    return details
      .entrySet()
      .stream()
      .filter(entry -> !"-".equals(entry.getValue()))
      .map(entry -> entry.getKey() + ": " + entry.getValue())
      .reduce((left, right) -> left + " | " + right)
      .orElse(t("report.incidents.no_details"));
  }

  // Indique si une ligne d'incident possede un detail metier utile a afficher.
  private boolean hasIncidentBusinessDetails(Map<?, ?> row) {
    return !"-".equals(
        firstNonBlank(mapValue(row, "cause"), mapValue(row, "causeDetail"))
      ) || !"-".equals(mapValue(row, "description"));
  }

  // Formate une date heterogene pour le PDF.

  private String formatDateValue(Object value) {
    if (value == null) {
      return "-";
    }
    String text = value.toString();
    if (text.isBlank()) {
      return "-";
    }
    try {
      return LocalDateTime.parse(text).format(DATE_FMT);
    } catch (DateTimeParseException ignored) {
      // Essaie les formats suivants (offset, instant, date seule).
    }
    try {
      return OffsetDateTime.parse(text).toLocalDateTime().format(DATE_FMT);
    } catch (DateTimeParseException ignored) {
      // format suivant
    }
    try {
      return Instant.parse(text)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DATE_FMT);
    } catch (DateTimeParseException ignored) {
      // format suivant
    }
    try {
      return LocalDate.parse(text).format(
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
      );
    } catch (DateTimeParseException ignored) {
      return text;
    }
  }

  // Une duree se lit dans l'unite de son ordre de grandeur : « 0,5 h » et « 172,4 h »
  // sont aussi illisibles l'un que l'autre. Sous l'heure on descend en minutes, au-dela
  // de deux jours on monte en jours.
  private static final double DAY_THRESHOLD_HOURS = 48.0;
  private static final double MINUTES_PER_HOUR = 60.0;

  // Formate une duree dans l'unite adaptee a son ordre de grandeur.
  private String formatHours(Object value) {
    if (!(value instanceof Number number)) {
      return "-";
    }
    double hours = Math.max(number.doubleValue(), 0.0);

    if (hours < 1.0) {
      long minutes = Math.round(hours * MINUTES_PER_HOUR);
      // 0,999 h arrondi a 60 min deviendrait « 60 min » : on passe a l'heure.
      return minutes == 60
        ? t("report.format.duration_hours", 1L)
        : t("report.format.duration_minutes", minutes);
    }

    if (hours < DAY_THRESHOLD_HOURS) {
      long wholeHours = (long) Math.floor(hours);
      long minutes = Math.round((hours - wholeHours) * MINUTES_PER_HOUR);
      // 1,999 h arrondi a 60 min deviendrait « 1h60 » : on reporte sur l'heure.
      if (minutes == 60) {
        wholeHours += 1;
        minutes = 0;
      }
      return minutes == 0
        ? t("report.format.duration_hours", wholeHours)
        : t(
          "report.format.duration_hours_minutes",
          wholeHours,
          String.format("%02d", minutes)
        );
    }

    long days = (long) Math.floor(hours / 24.0);
    long remainder = Math.round(hours - days * 24.0);
    // 71,8 h arrondi a 24 h de reste deviendrait « 2j 24h » : on reporte sur le jour.
    if (remainder == 24) {
      days += 1;
      remainder = 0;
    }
    return remainder == 0
      ? t("report.format.duration_days", days)
      : t("report.format.duration_days_hours", days, remainder);
  }

  // Construit un tableau PDF a partir de lignes simples.

  private PdfPTable simpleTable(String[] headers) {
    PdfPTable table = new PdfPTable(headers.length);
    table.setWidthPercentage(100);
    table.setHeaderRows(1);
    table.setSplitLate(false);
    Font headerFont = FontFactory.getFont(
      FontFactory.HELVETICA_BOLD,
      9,
      Color.WHITE
    );
    for (String header : headers) {
      PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
      cell.setBackgroundColor(NAVY);
      cell.setPadding(6);
      cell.setHorizontalAlignment(Element.ALIGN_CENTER);
      cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
      table.addCell(cell);
    }
    return table;
  }

  // Cree un element du domaine rapport document generator apres validation metier.

  private void addCells(PdfPTable table, String... values) {
    Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, DARK);
    for (String value : values) {
      PdfPCell cell = new PdfPCell(
        new Phrase(value != null ? value : "-", valueFont)
      );
      cell.setPadding(6);
      cell.setHorizontalAlignment(Element.ALIGN_CENTER);
      cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
      table.addCell(cell);
    }
  }

  // Realise l'intention metier metric label.

  private String metricLabel(String key) {
    String messageKey = "report.metric." + key;
    String label = t(messageKey);
    return messageKey.equals(label) ? key : label;
  }

  // Les metriques de flux entrant (incidents crees/entres dans la periode)
  // portent le libelle precis de la periode (aujourd'hui / cette semaine / ce
  // mois...) ; les autres gardent leur libelle generique.
  private String flowMetricLabel(String key, String createdLabel) {
    boolean isInflow = "inflow".equals(key) || "totalIncidents".equals(key);
    return isInflow && createdLabel != null ? createdLabel : metricLabel(key);
  }

  // Traduit le libelle d'un filtre de rapport.

  private String filterLabel(String key) {
    String messageKey = "report.filter." + key;
    String label = t(messageKey);
    return messageKey.equals(label) ? key : label;
  }

  // Formate metric value.

  // Formate une metrique en tenant compte de son unite et de son denominateur.
  private String formatMetricValue(
    String key,
    Object value,
    Map<String, Object> metrics
  ) {
    if (value == null) {
      return "-";
    }
    if (HOUR_METRICS.contains(key)) {
      return formatHours(value);
    }
    if (PERCENTAGE_METRICS.contains(key) && value instanceof Number number) {
      String rate = String.format(
        LocaleContextHolder.getLocale(),
        "%.1f %%",
        number.doubleValue()
      );
      double denominator = numberValue(metrics.get(denominatorKey(key)));
      if (denominator > 0) {
        long numerator = Math.round(
          number.doubleValue() * denominator / 100.0
        );
        return t(
          "report.metric.rate_detail",
          rate,
          numerator,
          Math.round(denominator)
        );
      }
      return rate;
    }
    if ("netBacklog".equals(key) && value instanceof Number number) {
      return String.format(LocaleContextHolder.getLocale(), "%+d", number.longValue());
    }
    if (value instanceof Number number) {
      double numericValue = number.doubleValue();
      if (Math.rint(numericValue) == numericValue) {
        return String.format(LocaleContextHolder.getLocale(), "%.0f", numericValue);
      }
      return String.format(LocaleContextHolder.getLocale(), "%.1f", numericValue);
    }
    return formatValue(value);
  }

  // Associe chaque taux au denominateur metier retourne par le dashboard.
  private String denominatorKey(String metricKey) {
    return switch (metricKey) {
      case "transferRate" -> "transferDenominator";
      case "slaComplianceRate" -> "slaDenominator";
      case "resolutionReopenRate" -> "reopenDenominator";
      default -> "";
    };
  }

  // Formate une valeur heterogene pour l'affichage.

  private String formatValue(Object value) {
    if (value == null) {
      return "-";
    }
    if (value instanceof Collection<?> collection) {
      if (collection.isEmpty()) {
        return "-";
      }
      return collection
        .stream()
        .map(Object::toString)
        .reduce((left, right) -> left + ", " + right)
        .orElse("-");
    }
    return value.toString();
  }

  // Prepare une repartition traduite, sans categorie vide et triee par volume.
  private Map<String, Number> normalizedDistribution(
    String metricKey,
    Map<?, ?> values
  ) {
    Map<String, Number> normalized = new LinkedHashMap<>();
    values
      .entrySet()
      .stream()
      .filter(entry -> entry.getValue() instanceof Number)
      .map(entry ->
        Map.entry(
          translateDimensionValue(metricKey, String.valueOf(entry.getKey())),
          (Number) entry.getValue()
        )
      )
      .filter(entry -> entry.getValue().doubleValue() > 0)
      .sorted(
        Map.Entry.<String, Number>comparingByValue(
          Comparator.comparingDouble(Number::doubleValue)
        ).reversed()
      )
      .forEach(entry -> normalized.put(entry.getKey(), entry.getValue()));
    return normalized;
  }

  // Traduit les codes de statut, criticite, anciennete et cohorte.
  private String translateDimensionValue(String metricKey, String rawValue) {
    if (rawValue == null || rawValue.isBlank() || "-".equals(rawValue)) {
      return "-";
    }
    String prefix = switch (metricKey) {
      case "distributionByStatus" -> "report.enum.status.";
      case "distributionByCriticality" -> "report.enum.criticality.";
      case "cohortOutcome" -> "report.enum.cohort.";
      case "ageDistribution" -> "report.enum.age.";
      default -> null;
    };
    if (prefix == null) {
      return rawValue;
    }
    String key = prefix + rawValue;
    String translated = t(key);
    return key.equals(translated) ? rawValue : translated;
  }

  // Traduit le statut technique du cycle de generation du rapport.
  private String reportStatusLabel(String status) {
    if (status == null || status.isBlank()) {
      return "-";
    }
    String key = "report.status." + status.toLowerCase(Locale.ROOT);
    String translated = t(key);
    return key.equals(translated) ? status : translated;
  }

  // Prepare l'enregistrement de la ressource selon les regles metier.

  private Image createBarChart(
    String categoryAxisLabel,
    String valueAxisLabel,
    Collection<?> data
  ) {
    if (data == null || data.isEmpty()) return null;
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    for (Object row : data) {
      if (row instanceof Map<?, ?> entry) {
        Object monthRaw = entry.get("month");
        int month = monthRaw instanceof Number n ? n.intValue() : 0;
        String monthLabel = getMonthLabel(month);
        Object v = entry.get("value");
        Number value = v instanceof Number n ? n : 0;
        dataset.addValue(value, valueAxisLabel, monthLabel);
      }
    }
    JFreeChart chart = ChartFactory.createBarChart(
      null,
      categoryAxisLabel,
      valueAxisLabel,
      dataset,
      PlotOrientation.VERTICAL,
      false,
      false,
      false
    );
    CategoryPlot plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(Color.WHITE);
    plot.setOutlineVisible(false);
    plot.setRangeGridlinePaint(new Color(230, 230, 230));
    NumberAxis axis = (NumberAxis) plot.getRangeAxis();
    if (t("report.table.incidents").equals(valueAxisLabel)) {
      configureIntegerAxis(axis);
    }
    BarRenderer renderer = new BarRenderer() {
      @Override
      public Paint getItemPaint(int row, int column) {
        return column == 0
          ? GOLD
          : CATEGORICAL[(column - 1) % CATEGORICAL.length];
      }
    };
    plot.setRenderer(renderer);
    renderer.setBarPainter(new StandardBarPainter());
    renderer.setShadowVisible(false);
    renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
    renderer.setDefaultItemLabelsVisible(true);
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ChartUtils.writeChartAsPNG(out, chart, 450, 250);
      return Image.getInstance(out.toByteArray());
    } catch (Exception e) {
      log.error("Erreur lors de la création du BarChart", e);
      return null;
    }
  }

  // Decoupe les types en graphiques successifs afin de conserver des barres lisibles.
  private List<Image> createTypeDistributionCharts(Collection<?> sections) {
    if (sections == null || sections.isEmpty()) return List.of();
    List<Map.Entry<String, Number>> values = sections
      .stream()
      .filter(Map.class::isInstance)
      .map(Map.class::cast)
      .map(section ->
        Map.entry(
          mapValue(section, "typeName"),
          (Number) Double.valueOf(numberValue(section.get("count")))
        )
      )
      .sorted(
        Map.Entry.<String, Number>comparingByValue(
          Comparator.comparingDouble(Number::doubleValue)
        ).reversed()
      )
      .toList();

    List<Image> charts = new ArrayList<>();
    int pageSize = 12;
    for (int start = 0; start < values.size(); start += pageSize) {
      int end = Math.min(start + pageSize, values.size());
      Map<String, Number> page = new LinkedHashMap<>();
      values.subList(start, end).forEach(entry -> page.put(entry.getKey(), entry.getValue()));
      Image chart = createHorizontalBarChart(page);
      if (chart != null) {
        charts.add(chart);
      }
    }
    return charts;
  }

  // Cree un graphique horizontal avec valeurs entieres affichees sur les barres.
  private Image createDistributionBarChart(Map<String, Number> values) {
    return createHorizontalBarChart(values);
  }

  // Produit un graphique categoriel dense, trie et adapte aux libelles longs.
  private Image createHorizontalBarChart(Map<String, Number> values) {
    if (values == null || values.isEmpty()) return null;
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    values.forEach((label, value) ->
      dataset.addValue(value, t("report.table.incidents"), label)
    );
    JFreeChart chart = ChartFactory.createBarChart(
      null,
      "",
      t("report.table.incidents"),
      dataset,
      PlotOrientation.HORIZONTAL,
      false,
      false,
      false
    );
    CategoryPlot plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(Color.WHITE);
    plot.setOutlineVisible(false);
    plot.setRangeGridlinePaint(new Color(230, 230, 230));
    configureIntegerAxis((NumberAxis) plot.getRangeAxis());
    BarRenderer renderer = new BarRenderer() {
      @Override
      public Paint getItemPaint(int row, int column) {
        return column == 0
          ? GOLD
          : CATEGORICAL[(column - 1) % CATEGORICAL.length];
      }
    };
    plot.setRenderer(renderer);
    renderer.setBarPainter(new StandardBarPainter());
    renderer.setShadowVisible(false);
    renderer.setMaximumBarWidth(0.09);
    renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
    renderer.setDefaultItemLabelsVisible(true);
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      int height = Math.max(220, Math.min(560, values.size() * 44 + 100));
      ChartUtils.writeChartAsPNG(out, chart, 520, height);
      return Image.getInstance(out.toByteArray());
    } catch (Exception e) {
      log.error("Erreur lors de la creation du graphique par type", e);
      return null;
    }
  }

  // Impose des graduations entieres pour les volumes d'incidents.
  private void configureIntegerAxis(NumberAxis axis) {
    axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    axis.setAutoRangeIncludesZero(true);
    axis.setLowerBound(0);
    axis.setUpperMargin(0.15);
  }

  /** Dessine l'en-tête et le pied de page en étoile FINSTAR sur chaque page. */
  // Modelise la responsabilite applicative liee a rapport.
  private final class FinstarHeaderFooter extends PdfPageEventHelper {

    @Override
    // Ajoute les elements de bas de page lors de la generation PDF.
    public void onEndPage(PdfWriter writer, Document document) {
      PdfContentByte cb = writer.getDirectContentUnder();
      float pageWidth = document.getPageSize().getWidth();
      float pageTop = document.getPageSize().getHeight();
      float starCx = 75;
      float starCy = pageTop - 50;

      boolean rasterLogo = drawRasterLogo(cb, pageTop);
      if (!rasterLogo) {
        drawStar(cb, starCx, starCy, 24, GOLD);
        drawStar(cb, starCx, starCy, 11, Color.WHITE);

        cb.setColorFill(PINK);
        cb.moveTo(starCx - 14, starCy - 30);
        cb.lineTo(starCx + 12, starCy - 30);
        cb.lineTo(starCx - 14, starCy - 14);
        cb.closePath();
        cb.fill();

        Font nameFont = FontFactory.getFont(
          FontFactory.HELVETICA_BOLD,
          16,
          DARK
        );
        Font taglineFont = FontFactory.getFont(
          FontFactory.HELVETICA,
          7,
          Color.GRAY
        );
        ColumnText.showTextAligned(
          cb,
          Element.ALIGN_LEFT,
          new Phrase("FINSTAR-CM S.A.", nameFont),
          110,
          starCy - 3,
          0
        );
        ColumnText.showTextAligned(
          cb,
          Element.ALIGN_LEFT,
          new Phrase(t("report.footer.tagline"), taglineFont),
          110,
          starCy - 17,
          0
        );
      }

      drawFinTrackLogoSvg(cb, pageWidth - 25, pageTop - 34);

      cb.setColorStroke(GOLD);
      cb.setLineWidth(1.5f);
      cb.moveTo(25, pageTop - 78);
      cb.lineTo(pageWidth - 25, pageTop - 78);
      cb.stroke();

      Font footerFont = FontFactory.getFont(
        FontFactory.HELVETICA,
        7,
        Color.GRAY
      );
      ColumnText.showTextAligned(
        cb,
        Element.ALIGN_CENTER,
        new Phrase(
          t("report.footer.app_name") + " · FINSTAR-CM S.A.",
          footerFont
        ),
        pageWidth / 2,
        35,
        0
      );
      ColumnText.showTextAligned(
        cb,
        Element.ALIGN_RIGHT,
        new Phrase(t("report.footer.page", writer.getPageNumber()), footerFont),
        pageWidth - 25,
        35,
        0
      );
    }

    /** Dessine le logo FinTrack (fintrack-logo.svg) en vectoriel natif dans l'en-tete droit. */
    private void drawFinTrackLogoSvg(PdfContentByte cb, float rightX, float topY) {
      float scale = 0.5f;
      float originX = rightX - (320 * scale);
      float originY = topY - (76 * scale);

      cb.saveState();

      // 1. Icone : Rectangle arrondi (56x56, rx=14)
      float iconX = originX;
      float iconY = originY + (10 * scale);
      float iconW = 56 * scale;
      float iconH = 56 * scale;
      float iconRx = 14 * scale;

      cb.setRGBColorFill(0x1E, 0x29, 0x3B);
      cb.roundRectangle(iconX, iconY, iconW, iconH, iconRx);
      cb.fill();

      // 2. Ligne de tendance polyline
      cb.setRGBColorStroke(0xFF, 0xFF, 0xFF);
      cb.setLineWidth(4.5f * scale);
      cb.setLineCap(PdfContentByte.LINE_CAP_ROUND);
      cb.setLineJoin(PdfContentByte.LINE_JOIN_ROUND);

      float p1x = iconX + (12 * scale), p1y = iconY + (56 - 40) * scale;
      float p2x = iconX + (24 * scale), p2y = iconY + (56 - 29) * scale;
      float p3x = iconX + (35 * scale), p3y = iconY + (56 - 34) * scale;
      float p4x = iconX + (46 * scale), p4y = iconY + (56 - 18) * scale;

      cb.moveTo(p1x, p1y);
      cb.lineTo(p2x, p2y);
      cb.lineTo(p3x, p3y);
      cb.lineTo(p4x, p4y);
      cb.stroke();

      // 3. Noeuds blancs (r=3.5)
      cb.setRGBColorFill(0xFF, 0xFF, 0xFF);
      float rWhite = 3.5f * scale;
      cb.circle(p1x, p1y, rWhite);
      cb.circle(p2x, p2y, rWhite);
      cb.circle(p3x, p3y, rWhite);
      cb.fill();

      // 4. Noeud rouge final (r=5.0)
      cb.setRGBColorFill(0xE5, 0x39, 0x35);
      cb.circle(p4x, p4y, 5.0f * scale);
      cb.fill();

      cb.restoreState();

      // 5. Texte "FinTrack"
      float textX = originX + (70 * scale);
      float titleY = originY + (76 - 45) * scale;

      Font finFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, new Color(0xE5, 0x39, 0x35));
      Font trackFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, new Color(0x1E, 0x29, 0x3B));
      Phrase titlePhrase = new Phrase();
      titlePhrase.add(new Chunk("Fin", finFont));
      titlePhrase.add(new Chunk("Track", trackFont));

      ColumnText.showTextAligned(
        cb,
        Element.ALIGN_LEFT,
        titlePhrase,
        textX,
        titleY,
        0
      );

      // 6. Sous-titre "Suivi et tracabilite des incidents"
      float subY = originY + (76 - 67) * scale;
      Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(0x64, 0x74, 0x8B));
      ColumnText.showTextAligned(
        cb,
        Element.ALIGN_LEFT,
        new Phrase(t("report.fintrack_subtitle"), subFont),
        textX,
        subY,
        0
      );
    }

    // Dessine le logo raster dans le PDF (gauche).

    private boolean drawRasterLogo(PdfContentByte cb, float pageTop) {
      if (LOGO_BYTES == null) {
        return false;
      }
      try {
        Image logo = Image.getInstance(LOGO_BYTES);
        logo.scaleToFit(170, 52);
        logo.setAbsolutePosition(25, pageTop - 28 - logo.getScaledHeight());
        cb.addImage(logo);
        return true;
      } catch (Exception e) {
        log.warn(
          "Impossible de rendre le logo du rapport, falling back vers vector: {}",
          e.getMessage()
        );
        return false;
      }
    }

    // Dessine une etoile vectorielle dans le PDF.

    private static void drawStar(
      PdfContentByte cb,
      float cx,
      float cy,
      float outerR,
      Color color
    ) {
      float innerR = outerR * 0.382f;
      cb.setColorFill(color);
      for (int i = 0; i < 10; i++) {
        double angle = Math.toRadians(-90 + i * 36);
        float r = i % 2 == 0 ? outerR : innerR;
        float x = cx + (float) (r * Math.cos(angle));
        float y = cy + (float) (r * Math.sin(angle));
        if (i == 0) {
          cb.moveTo(x, y);
        } else {
          cb.lineTo(x, y);
        }
      }
      cb.closePath();
      cb.fill();
    }
  }

  // Excel

  private byte[] generateExcel(GeneratedReport report) {
    try (
      Workbook workbook = new XSSFWorkbook();
      ByteArrayOutputStream out = new ByteArrayOutputStream()
    ) {
      // Define common styles
      CellStyle headerStyle = workbook.createCellStyle();
      var headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerFont.setColor(IndexedColors.WHITE.getIndex());
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(
        IndexedColors.GREY_80_PERCENT.getIndex()
      );
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      CellStyle keyStyle = workbook.createCellStyle();
      var keyFont = workbook.createFont();
      keyFont.setBold(true);
      keyStyle.setFont(keyFont);

      Map<String, Object> filterPayload = readJson(report.getFilters());
      Map<String, Object> metrics = normalizeMilestoneKeys(
        readJson(report.getMetrics())
      );
      String createdLabel = createdIncidentsLabel(report);
      String periodPhrase = statusFlowPeriodPhrase(report);
      String activityTitle = statusActivityTitle(report);

      if (!groupedReports(metrics).isEmpty()) {
        writeGroupedExcelSheets(
          workbook,
          groupedReports(metrics),
          headerStyle,
          keyStyle,
          createdLabel,
          periodPhrase,
          activityTitle
        );
        writeExcelFiltersSheet(workbook, filterPayload, keyStyle);
        workbook.write(out);
        return out.toByteArray();
      }

      if (isIncidentTypeAnalysis(metrics)) {
        writeIncidentTypeExcelSheets(workbook, metrics, headerStyle, keyStyle);
        writeExcelFiltersSheet(workbook, filterPayload, keyStyle);
        workbook.write(out);
        return out.toByteArray();
      }

      if (isIncidentStatusOverview(metrics)) {
        writeIncidentStatusExcelSheets(
          workbook,
          metrics,
          headerStyle,
          keyStyle,
          createdLabel,
          periodPhrase,
          activityTitle
        );
        writeExcelFiltersSheet(workbook, filterPayload, keyStyle);
        workbook.write(out);
        return out.toByteArray();
      }

      // --- Sheet 1: Synthese ---
      Sheet sheet1 = workbook.createSheet(t("report.excel.sheet.summary"));
      sheet1.setColumnWidth(0, 8000);
      sheet1.setColumnWidth(1, 12000);

      CellStyle brandStyle = workbook.createCellStyle();
      var brandFont = workbook.createFont();
      brandFont.setBold(true);
      brandFont.setFontHeightInPoints((short) 14);
      brandFont.setColor(IndexedColors.WHITE.getIndex());
      brandStyle.setFont(brandFont);
      brandStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());
      brandStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      Row brandRow = sheet1.createRow(0);
      Cell brandCell = brandRow.createCell(0);
      brandCell.setCellValue(t("report.excel.brand"));
      brandCell.setCellStyle(brandStyle);
      sheet1.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

      sheet1
        .createRow(2)
        .createCell(0)
        .setCellValue(t("report.excel.report_title", safe(report.getName())));
      sheet1.getRow(2).getCell(0).setCellStyle(keyStyle);

      Map<String, Object> synth = new LinkedHashMap<>();
      synth.put(t("report.field.type"), reportTypeLabel(report));
      synth.put(t("report.field.generation"), generationLabel(report));
      synth.put(t("report.field.format"), reportFormatLabel(report));
      synth.put(t("report.period").replace(" :", ""), formatPeriod(report));
      synth.put(t("report.field.status"), reportStatusLabel(report.getStatus()));
      synth.put(
        t("report.field.generatedAt"),
        report.getCreatedAt() != null
          ? report.getCreatedAt().format(DATE_FMT)
          : "-"
      );
      synth.put(
        t("report.field.createdBy"),
        displayCreator(report, filterPayload)
      );

      int rowIdx = 4;
      for (Map.Entry<String, Object> entry : synth.entrySet()) {
        Row row = sheet1.createRow(rowIdx++);
        Cell keyCell = row.createCell(0);
        keyCell.setCellValue(entry.getKey());
        keyCell.setCellStyle(keyStyle);
        row
          .createCell(1)
          .setCellValue(
            entry.getValue() != null ? entry.getValue().toString() : "-"
          );
      }
      rowIdx++;
      rowIdx = writeExcelMetricGroup(
        sheet1,
        rowIdx,
        t("report.summary.period_activity"),
        metrics,
        periodFlowMetricKeys(metrics),
        headerStyle,
        keyStyle,
        createdLabel
      );
      rowIdx = writeExcelMetricGroup(
        sheet1,
        rowIdx,
        t("report.summary.current_snapshot"),
        metrics,
        CURRENT_SNAPSHOT_METRICS,
        headerStyle,
        keyStyle,
        createdLabel
      );
      rowIdx = writeExcelMetricGroup(
        sheet1,
        rowIdx,
        t("report.summary.performance"),
        metrics,
        PERFORMANCE_METRICS,
        headerStyle,
        keyStyle,
        createdLabel
      );
      writeExcelMetricGroup(
        sheet1,
        rowIdx,
        t("report.summary.personal_activity"),
        metrics,
        PERSONAL_METRICS,
        headerStyle,
        keyStyle,
        createdLabel
      );

      // --- Sheet 2: Repartitions ---
      Sheet sheet2 = workbook.createSheet(
        t("report.excel.sheet.distributions")
      );
      sheet2.setColumnWidth(0, 8000);
      sheet2.setColumnWidth(1, 4000);
      int rIdx2 = 0;

      rIdx2 = writeExcelMapMetric(
        sheet2,
        "distributionByType",
        metrics.get("distributionByType"),
        headerStyle,
        rIdx2
      );
      rIdx2 += 2;
      rIdx2 = writeExcelMapMetric(
        sheet2,
        "distributionByCriticality",
        metrics.get("distributionByCriticality"),
        headerStyle,
        rIdx2
      );
      rIdx2 += 2;
      rIdx2 = writeExcelMapMetric(
        sheet2,
        "distributionByStatus",
        metrics.get("distributionByStatus"),
        headerStyle,
        rIdx2
      );
      rIdx2 += 2;
      rIdx2 = writeExcelMapMetric(
        sheet2,
        "ageDistribution",
        metrics.get("ageDistribution"),
        headerStyle,
        rIdx2
      );
      rIdx2 += 2;
      writeExcelMapMetric(
        sheet2,
        "cohortOutcome",
        metrics.get("cohortOutcome"),
        headerStyle,
        rIdx2
      );

      // --- Sheet 3: Performances ---
      Sheet sheet3 = workbook.createSheet(t("report.excel.sheet.performance"));
      sheet3.setColumnWidth(0, 8000);
      sheet3.setColumnWidth(1, 4000);
      int rIdx3 = 0;

      rIdx3 = writeExcelRanking(
        sheet3,
        t("report.metric.topServices"),
        metrics.get("topServices"),
        "serviceName",
        headerStyle,
        rIdx3
      );
      rIdx3 = writeExcelRanking(
        sheet3,
        t("report.metric.topAgencies"),
        metrics.get("topAgencies"),
        "name",
        headerStyle,
        rIdx3
      );
      rIdx3 = writeExcelRanking(
        sheet3,
        t("report.metric.topResolvers"),
        metrics.get("topResolvers"),
        "name",
        headerStyle,
        rIdx3
      );
      rIdx3 += 2;
      writeExcelTop(
        sheet3,
        t("report.metric.workload"),
        metrics.get("workload"),
        "name",
        headerStyle,
        rIdx3
      );

      // --- Sheet 4: Tendances ---
      Sheet sheet4 = workbook.createSheet(t("report.excel.sheet.trends"));
      sheet4.setColumnWidth(0, 6000);
      sheet4.setColumnWidth(1, 6000);
      int rIdx4 = 0;

      rIdx4 = writeExcelMonthly(
        sheet4,
        t("report.metric.monthlyClosures"),
        metrics.get("monthlyClosures"),
        t("report.table.incidents"),
        headerStyle,
        rIdx4
      );
      rIdx4 += 2;
      writeExcelMonthly(
        sheet4,
        t("report.metric.monthlyAvgClosureHours"),
        metrics.get("monthlyAvgClosureHours"),
        t("report.table.hours"),
        headerStyle,
        rIdx4
      );

      writeExcelIncidents(workbook, metrics.get("incidentsList"), headerStyle);
      writeExcelFiltersSheet(workbook, filterPayload, keyStyle);

      workbook.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      log.error("Échec de generer le rapport Excel {}", report.getId(), e);
      throw new BusinessRuleViolationException(
        ErrorCode.EXPORT_ERROR,
        t("reporting.error.generate_excel")
      );
    }
  }

  // Serialise les donnees du domaine rapport document generator pour stockage ou transmission.

  private int writeExcelMetricGroup(
    Sheet sheet,
    int startRow,
    String title,
    Map<String, Object> metrics,
    Collection<String> keys,
    CellStyle headerStyle,
    CellStyle keyStyle,
    String createdLabel
  ) {
    List<String> availableKeys = keys
      .stream()
      .filter(metrics::containsKey)
      .toList();
    if (availableKeys.isEmpty()) {
      return startRow;
    }
    Row titleRow = sheet.createRow(startRow++);
    titleRow.createCell(0).setCellValue(title);
    titleRow.getCell(0).setCellStyle(headerStyle);
    titleRow.createCell(1).setCellStyle(headerStyle);
    sheet.addMergedRegion(
      new CellRangeAddress(startRow - 1, startRow - 1, 0, 1)
    );
    for (String key : availableKeys) {
      Row row = sheet.createRow(startRow++);
      Cell keyCell = row.createCell(0);
      keyCell.setCellValue(flowMetricLabel(key, createdLabel));
      keyCell.setCellStyle(keyStyle);
      row
        .createCell(1)
        .setCellValue(formatMetricValue(key, metrics.get(key), metrics));
    }
    return startRow + 1;
  }

  // Serialise les repartitions traduites dans une feuille Excel.

  private int writeExcelMapMetric(
    Sheet sheet,
    String metricKey,
    Object data,
    CellStyle headerStyle,
    int startRow
  ) {
    if (!(data instanceof Map<?, ?> map) || map.isEmpty()) return startRow;
    Map<String, Number> values = normalizedDistribution(metricKey, map);
    if (values.isEmpty()) return startRow;

    Row titleRow = sheet.createRow(startRow++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue(metricLabel(metricKey));
    titleCell.setCellStyle(headerStyle);
    titleRow.createCell(1).setCellStyle(headerStyle);
    sheet.addMergedRegion(
      new CellRangeAddress(startRow - 1, startRow - 1, 0, 1)
    );

    for (Map.Entry<String, Number> entry : values.entrySet()) {
      Row row = sheet.createRow(startRow++);
      row.createCell(0).setCellValue(entry.getKey());
      row.createCell(1).setCellValue(entry.getValue().doubleValue());
    }
    return startRow;
  }

  // Serialise les donnees du domaine rapport document generator pour stockage ou transmission.

  private int writeExcelTop(
    Sheet sheet,
    String title,
    Object data,
    String nameKey,
    CellStyle headerStyle,
    int startRow
  ) {
    if (
      !(data instanceof Collection<?> list) || list.isEmpty()
    ) return startRow;

    Row titleRow = sheet.createRow(startRow++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue(title);
    titleCell.setCellStyle(headerStyle);
    titleRow.createCell(1).setCellStyle(headerStyle);
    sheet.addMergedRegion(
      new CellRangeAddress(startRow - 1, startRow - 1, 0, 1)
    );

    for (Object item : list) {
      if (item instanceof Map<?, ?> map) {
        Row row = sheet.createRow(startRow++);
        row.createCell(0).setCellValue(mapValue(map, nameKey));
        row.createCell(1).setCellValue(mapValue(map, "count"));
      }
    }
    return startRow;
  }

  // Ecrit un classement Excel avec rang explicite et ex aequo visibles.
  private int writeExcelRanking(
    Sheet sheet,
    String title,
    Object data,
    String nameKey,
    CellStyle headerStyle,
    int startRow
  ) {
    List<Map<?, ?>> rows = rankingRows(data);
    if (rows.isEmpty()) {
      return startRow;
    }

    Row titleRow = sheet.createRow(startRow++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue(title);
    titleCell.setCellStyle(headerStyle);
    titleRow.createCell(1).setCellStyle(headerStyle);
    titleRow.createCell(2).setCellStyle(headerStyle);
    sheet.addMergedRegion(
      new CellRangeAddress(startRow - 1, startRow - 1, 0, 2)
    );

    Row headers = sheet.createRow(startRow++);
    headers.createCell(0).setCellValue(t("report.table.rank"));
    headers.createCell(1).setCellValue(t("report.table.label"));
    headers.createCell(2).setCellValue(t("report.table.incidents"));
    for (int column = 0; column < 3; column++) {
      headers.getCell(column).setCellStyle(headerStyle);
    }

    for (int index = 0; index < rows.size(); index++) {
      Map<?, ?> item = rows.get(index);
      Row row = sheet.createRow(startRow++);
      row.createCell(0).setCellValue(rankingPosition(rows, index));
      row.createCell(1).setCellValue(mapValue(item, nameKey));
      row.createCell(2).setCellValue(mapValue(item, "count"));
    }
    return startRow;
  }

  // Serialise les donnees du domaine rapport document generator pour stockage ou transmission.

  private int writeExcelMonthly(
    Sheet sheet,
    String title,
    Object data,
    String valHeader,
    CellStyle headerStyle,
    int startRow
  ) {
    if (
      !(data instanceof Collection<?> list) || list.isEmpty()
    ) return startRow;

    Row titleRow = sheet.createRow(startRow++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue(title);
    titleCell.setCellStyle(headerStyle);
    Cell valHeaderCell = titleRow.createCell(1);
    valHeaderCell.setCellValue(valHeader);
    valHeaderCell.setCellStyle(headerStyle);

    for (Object item : list) {
      if (item instanceof Map<?, ?> map) {
        Object monthRaw = map.get("month");
        int month = monthRaw instanceof Number n ? n.intValue() : 0;
        String monthLabel = getMonthLabel(month);
        Object v = map.get("value");
        String valueLabel = formatMonthlyValue(title, v);

        Row row = sheet.createRow(startRow++);
        row.createCell(0).setCellValue(monthLabel);
        row.createCell(1).setCellValue(valueLabel);
      }
    }
    return startRow;
  }

  // Serialise les donnees du domaine rapport document generator pour stockage ou transmission.

  private void writeExcelIncidents(
    Workbook workbook,
    Object data,
    CellStyle headerStyle
  ) {
    if (!(data instanceof Collection<?> incidents) || incidents.isEmpty()) {
      return;
    }
    Sheet sheet = workbook.createSheet(t("report.excel.sheet.incidents"));
    int[] widths = {
      9000,
      5000,
      5000,
      5000,
      5000,
      5000,
      3500,
      3500,
      8000,
      8000,
      16000,
    };
    for (int i = 0; i < widths.length; i++) {
      sheet.setColumnWidth(i, widths[i]);
    }

    Row header = sheet.createRow(0);
    String[] labels = {
      t("report.table.incident"),
      t("report.table.creator"),
      t("report.table.type"),
      t("report.table.agency"),
      t("report.table.service"),
      t("report.table.assignee"),
      t("report.table.status"),
      t("report.table.criticality"),
      t("report.table.key_dates"),
      t("report.table.cause"),
      t("report.table.description"),
    };
    for (int i = 0; i < labels.length; i++) {
      Cell cell = header.createCell(i);
      cell.setCellValue(labels[i]);
      cell.setCellStyle(headerStyle);
    }

    int rowIdx = 1;
    for (Object item : incidents) {
      if (item instanceof Map<?, ?> row) {
        Row excelRow = sheet.createRow(rowIdx++);
        excelRow.createCell(0).setCellValue(incidentTitle(row));
        excelRow.createCell(1).setCellValue(personName(row.get("createdBy")));
        excelRow
          .createCell(2)
          .setCellValue(
            firstNonBlank(
              nestedValue(row, "type", "displayName"),
              nestedValue(row, "type", "name")
            )
          );
        excelRow.createCell(3).setCellValue(nestedValue(row, "agency", "name"));
        excelRow
          .createCell(4)
          .setCellValue(
            firstNonBlank(
              nestedValue(row, "transferredToService", "name"),
              mapValue(row, "serviceName")
            )
          );
        excelRow.createCell(5).setCellValue(personName(row.get("assignedTo")));
        excelRow
          .createCell(6)
          .setCellValue(
            translateDimensionValue(
              "distributionByStatus",
              mapValue(row, "status")
            )
          );
        excelRow
          .createCell(7)
          .setCellValue(
            translateDimensionValue(
              "distributionByCriticality",
              mapValue(row, "criticality")
            )
          );
        excelRow.createCell(8).setCellValue(incidentDates(row));
        excelRow
          .createCell(9)
          .setCellValue(
            firstNonBlank(mapValue(row, "cause"), mapValue(row, "causeDetail"))
          );
        excelRow.createCell(10).setCellValue(mapValue(row, "description"));
      }
    }
  }

  // Ajoute les filtres utiles dans la derniere feuille du classeur.
  private void writeExcelFiltersSheet(
    Workbook workbook,
    Map<String, Object> filterPayload,
    CellStyle keyStyle
  ) {
    Map<String, Object> filters = flattenFilters(filterPayload);
    if (filters.isEmpty()) {
      return;
    }
    Sheet sheet = workbook.createSheet(t("report.excel.sheet.filters"));
    sheet.setColumnWidth(0, 9000);
    sheet.setColumnWidth(1, 14000);
    int rowIndex = 0;
    for (Map.Entry<String, Object> entry : filters.entrySet()) {
      Row row = sheet.createRow(rowIndex++);
      Cell keyCell = row.createCell(0);
      keyCell.setCellValue(entry.getKey());
      keyCell.setCellStyle(keyStyle);
      row.createCell(1).setCellValue(formatValue(entry.getValue()));
    }
  }

  // Cree les deux feuilles exploitables du rapport thematique par type.
  private void writeIncidentTypeExcelSheets(
    Workbook workbook,
    Map<String, Object> metrics,
    CellStyle headerStyle,
    CellStyle keyStyle
  ) {
    Sheet summary = workbook.createSheet(t("report.excel.sheet.typeAnalysis"));
    int[] widths = { 10000, 4000, 4000, 4000, 4000, 4000 };
    for (int index = 0; index < widths.length; index++) {
      summary.setColumnWidth(index, widths[index]);
    }
    Row title = summary.createRow(0);
    title.createCell(0).setCellValue(t("report.typeAnalysis.title"));
    title.getCell(0).setCellStyle(keyStyle);

    Row header = summary.createRow(2);
    String[] labels = {
      t("report.table.type"),
      t("report.table.incidents"),
      t("report.table.share"),
      t("report.metric.activeIncidents"),
      t("report.metric.closedIncidents"),
      t("report.metric.rejectedIncidents"),
    };
    for (int index = 0; index < labels.length; index++) {
      Cell cell = header.createCell(index);
      cell.setCellValue(labels[index]);
      cell.setCellStyle(headerStyle);
    }
    int rowIndex = 3;
    if (metrics.get("typeSections") instanceof Collection<?> sections) {
      for (Object item : sections) {
        if (!(item instanceof Map<?, ?> section)) continue;
        Row row = summary.createRow(rowIndex++);
        row.createCell(0).setCellValue(mapValue(section, "typeName"));
        row.createCell(1).setCellValue(numberValue(section.get("count")));
        Cell shareCell = row.createCell(2);
        shareCell.setCellValue(numberValue(section.get("share")) / 100.0);
        CellStyle percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0%"));
        shareCell.setCellStyle(percentStyle);
        row.createCell(3).setCellValue(numberValue(section.get("active")));
        row.createCell(4).setCellValue(numberValue(section.get("closed")));
        row.createCell(5).setCellValue(numberValue(section.get("rejected")));
      }
    }
    writeExcelIncidents(workbook, metrics.get("incidentsList"), headerStyle);
  }

  // Ecrit le corps "situation par statut" d'une feuille : activite de la periode
  // (creations + flux, puis familles) et situation globale (backlog + familles).
  private int writeStatusSummaryBody(
    Workbook workbook,
    Sheet sheet,
    int startRow,
    Map<String, Object> metrics,
    CellStyle headerStyle,
    CellStyle labelStyle,
    String createdLabel,
    String periodPhrase,
    String activityTitle
  ) {
    Map<String, Object> situation = globalSituation(metrics);
    int r = startRow;
    // --- Activite de la periode ---
    r = excelLabelRow(sheet, r, activityTitle, labelStyle);
    r = excelKvRow(sheet, r, createdLabel, numberValue(metrics.get("totalIncidents")));
    r = excelKvRow(
      sheet,
      r,
      t("report.statusOverview.flow.treated", periodPhrase),
      numberValue(situation.get("treatedInPeriod"))
    );
    r = excelKvRow(
      sheet,
      r,
      t("report.statusOverview.flow.resolved", periodPhrase),
      numberValue(situation.get("resolvedInPeriod"))
    );
    r = excelKvRow(
      sheet,
      r,
      t("report.statusOverview.flow.closed", periodPhrase),
      numberValue(situation.get("closedInPeriod"))
    );
    // La ventilation par famille appartient a la situation globale ci-dessous (statut
    // courant), pas au flux de la periode : chaque compteur du flux a sa feuille dediee.
    // --- Situation globale (backlog courant) ---
    r++;
    r = excelLabelRow(
      sheet,
      r,
      t("report.statusOverview.global.title"),
      labelStyle
    );
    r = excelKvRow(
      sheet,
      r,
      t("report.statusOverview.global.untreated"),
      numberValue(situation.get("untreatedBacklog"))
    );
    r = excelKvRow(
      sheet,
      r,
      t("report.statusOverview.global.overdue"),
      numberValue(situation.get("overdueBacklog"))
    );
    r = writeExcelCategoryTable(
      workbook,
      sheet,
      r + 1,
      situation.get("categorySections"),
      headerStyle
    );
    return r;
  }

  private int excelLabelRow(
    Sheet sheet,
    int rowIndex,
    String label,
    CellStyle labelStyle
  ) {
    Row row = sheet.createRow(rowIndex);
    Cell cell = row.createCell(0);
    cell.setCellValue(label);
    if (labelStyle != null) {
      cell.setCellStyle(labelStyle);
    }
    return rowIndex + 1;
  }

  private int excelKvRow(
    Sheet sheet,
    int rowIndex,
    String label,
    double value
  ) {
    Row row = sheet.createRow(rowIndex);
    row.createCell(0).setCellValue(label);
    row.createCell(1).setCellValue(value);
    return rowIndex + 1;
  }

  // Tableau "famille / incidents / part" pour une population donnee.
  private int writeExcelCategoryTable(
    Workbook workbook,
    Sheet sheet,
    int startRow,
    Object sectionsObj,
    CellStyle headerStyle
  ) {
    int r = startRow;
    Row header = sheet.createRow(r++);
    String[] labels = {
      t("report.statusOverview.table.category"),
      t("report.table.incidents"),
      t("report.table.share"),
    };
    for (int index = 0; index < labels.length; index++) {
      Cell cell = header.createCell(index);
      cell.setCellValue(labels[index]);
      cell.setCellStyle(headerStyle);
    }
    if (sectionsObj instanceof Collection<?> sections) {
      for (Object item : sections) {
        if (!(item instanceof Map<?, ?> section)) continue;
        Row row = sheet.createRow(r++);
        row.createCell(0).setCellValue(categoryLabel(mapValue(section, "key")));
        row.createCell(1).setCellValue(numberValue(section.get("count")));
        Cell shareCell = row.createCell(2);
        shareCell.setCellValue(numberValue(section.get("share")) / 100.0);
        CellStyle percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(
          workbook.createDataFormat().getFormat("0.0%")
        );
        shareCell.setCellStyle(percentStyle);
      }
    }
    return r;
  }

  // Classeur "situation par statut" : feuille de synthese (activite periode +
  // situation globale), feuille "En retard", feuilles par famille, et une feuille
  // "Reste a traiter" listant le backlog courant.
  private void writeIncidentStatusExcelSheets(
    Workbook workbook,
    Map<String, Object> metrics,
    CellStyle headerStyle,
    CellStyle keyStyle,
    String createdLabel,
    String periodPhrase,
    String activityTitle
  ) {
    Sheet summary = workbook.createSheet(t("report.excel.sheet.statusOverview"));
    summary.setColumnWidth(0, 12000);
    summary.setColumnWidth(1, 4000);
    summary.setColumnWidth(2, 4000);

    Row title = summary.createRow(0);
    title.createCell(0).setCellValue(t("report.statusOverview.title"));
    title.getCell(0).setCellStyle(keyStyle);

    writeStatusSummaryBody(
      workbook,
      summary,
      1,
      metrics,
      headerStyle,
      keyStyle,
      createdLabel,
      periodPhrase,
      activityTitle
    );

    Map<String, Object> situation = globalSituation(metrics);

    // --- Activite de la periode : une feuille plate par compteur (crees + 3 flux),
    // meme format, dans le meme ordre que le PDF ; feuilles vides omises.
    writeFlowListSheet(workbook, metrics.get("incidentsList"), "report.statusOverview.flow.created.sheet", headerStyle);
    writeFlowListSheet(workbook, situation.get("treatedList"), "report.statusOverview.flow.treated.sheet", headerStyle);
    writeFlowListSheet(workbook, situation.get("resolvedList"), "report.statusOverview.flow.resolved.sheet", headerStyle);
    writeFlowListSheet(workbook, situation.get("closedList"), "report.statusOverview.flow.closed.sheet", headerStyle);

    // --- Situation globale : le backlog courant, et ses retards (adosses au KPI "En retard").
    if (situation.get("incidentsList") instanceof Collection<?> backlog) {
      writeStatusIncidentSheet(
        workbook,
        t("report.statusOverview.global.untreated"),
        backlog,
        headerStyle
      );
    }
    writeStatusIncidentSheet(
      workbook,
      t("report.excel.sheet.overdue"),
      overdueIncidents(situation),
      headerStyle
    );
  }

  // Feuille adossant un compteur de la periode (crees/traites/resolus/clotures) a sa
  // population. Ne cree rien si la liste est absente ou vide.
  private void writeFlowListSheet(
    Workbook workbook,
    Object data,
    String sheetNameKey,
    CellStyle headerStyle
  ) {
    if (data instanceof Collection<?> incidents) {
      writeStatusIncidentSheet(
        workbook,
        t(sheetNameKey),
        incidents,
        headerStyle
      );
    }
  }

  // Ecrit une feuille d'incidents par statut (reference, titre, type, agence, signalant, traiteur/assigne, date, retard). Ne cree rien si la liste est vide.
  private void writeStatusIncidentSheet(
    Workbook workbook,
    String rawName,
    Collection<?> incidents,
    CellStyle headerStyle
  ) {
    if (incidents == null || incidents.isEmpty()) return;
    Sheet sheet = workbook.createSheet(
      WorkbookUtil.createSafeSheetName(rawName)
    );
    int[] widths = { 4000, 10000, 5000, 5000, 5000, 5000, 4500, 3000, 16000 };
    for (int i = 0; i < widths.length; i++) {
      sheet.setColumnWidth(i, widths[i]);
    }
    Row header = sheet.createRow(0);
    String[] labels = {
      t("report.table.reference"),
      t("report.table.title"),
      t("report.table.type"),
      t("report.table.agency"),
      t("report.table.creator"),
      t("report.table.assignee"),
      t("report.statusOverview.statusAndCreated"),
      t("report.statusOverview.overdueColumn"),
      t("report.table.description"),
    };
    for (int i = 0; i < labels.length; i++) {
      Cell cell = header.createCell(i);
      cell.setCellValue(labels[i]);
      cell.setCellStyle(headerStyle);
    }
    // Cellule "Statut / Cree le" sur deux lignes : renvoi a la ligne active.
    CellStyle wrapStyle = workbook.createCellStyle();
    wrapStyle.setWrapText(true);
    int rowIndex = 1;
    for (Object item : incidents) {
      if (!(item instanceof Map<?, ?> row)) continue;
      Row r = sheet.createRow(rowIndex++);
      r.createCell(0).setCellValue(dash(mapValue(row, "reference")));
      r.createCell(1).setCellValue(dash(mapValue(row, "title")));
      r
        .createCell(2)
        .setCellValue(
          dash(
            firstNonBlank(
              nestedValue(row, "type", "displayName"),
              nestedValue(row, "type", "name")
            )
          )
        );
      r.createCell(3).setCellValue(dash(nestedValue(row, "agency", "name")));
      r.createCell(4).setCellValue(dash(personName(row.get("createdBy"))));
      r.createCell(5).setCellValue(dash(incidentAssignmentPerson(row)));
      Cell statusCreatedCell = r.createCell(6);
      statusCreatedCell.setCellValue(
        translateDimensionValue("distributionByStatus", mapValue(row, "status")) +
        "\n" +
        formatDateValue(row.get("createdAt"))
      );
      statusCreatedCell.setCellStyle(wrapStyle);
      r
        .createCell(7)
        .setCellValue(
          Boolean.TRUE.equals(row.get("overdue"))
            ? t("report.statusOverview.overdue.yes")
            : t("report.statusOverview.overdue.no")
        );
      r.createCell(8).setCellValue(dash(mapValue(row, "description")));
    }
  }

  // Convertit une metrique brute en nombre exploitable dans Excel.
  private double numberValue(Object value) {
    return value instanceof Number number ? number.doubleValue() : 0.0;
  }

  // Cree une feuille Excel autonome pour chaque entite du regroupement.
  private void writeGroupedExcelSheets(
    Workbook workbook,
    Collection<?> groups,
    CellStyle headerStyle,
    CellStyle keyStyle,
    String createdLabel,
    String periodPhrase,
    String activityTitle
  ) {
    int groupIndex = 1;
    for (Object item : groups) {
      if (!(item instanceof Map<?, ?> group)) continue;
      String label =
        group.get("label") != null ? String.valueOf(group.get("label")) : "-";
      Sheet sheet = workbook.createSheet(safeSheetName(groupIndex++, label));
      sheet.setColumnWidth(0, 9000);
      sheet.setColumnWidth(1, 6000);
      Map<String, Object> metrics = groupMetrics(group);

      Row titleRow = sheet.createRow(0);
      Cell titleCell = titleRow.createCell(0);
      titleCell.setCellValue(label);
      titleCell.setCellStyle(headerStyle);
      titleRow.createCell(1).setCellStyle(headerStyle);
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

      int rowIndex = 2;
      if (isIncidentTypeAnalysis(metrics)) {
        writeGroupedTypeAnalysis(sheet, metrics, headerStyle, rowIndex);
        continue;
      }
      if (isIncidentStatusOverview(metrics)) {
        writeGroupedStatusOverview(
          sheet,
          metrics,
          headerStyle,
          rowIndex,
          createdLabel,
          periodPhrase,
          activityTitle
        );
        continue;
      }
      rowIndex = writeExcelMetricGroup(
        sheet,
        rowIndex,
        t("report.summary.period_activity"),
        metrics,
        periodFlowMetricKeys(metrics),
        headerStyle,
        keyStyle,
        createdLabel
      );
      rowIndex = writeExcelMetricGroup(
        sheet,
        rowIndex,
        t("report.summary.current_snapshot"),
        metrics,
        CURRENT_SNAPSHOT_METRICS,
        headerStyle,
        keyStyle,
        createdLabel
      );
      rowIndex = writeExcelMetricGroup(
        sheet,
        rowIndex,
        t("report.summary.performance"),
        metrics,
        PERFORMANCE_METRICS,
        headerStyle,
        keyStyle,
        createdLabel
      );

      rowIndex += 2;
      rowIndex =
        writeExcelMapMetric(
          sheet,
          "distributionByType",
          metrics.get("distributionByType"),
          headerStyle,
          rowIndex
        ) + 1;
      rowIndex =
        writeExcelMapMetric(
          sheet,
          "distributionByCriticality",
          metrics.get("distributionByCriticality"),
          headerStyle,
          rowIndex
        ) + 1;
      rowIndex =
        writeExcelMapMetric(
          sheet,
          "distributionByStatus",
          metrics.get("distributionByStatus"),
          headerStyle,
          rowIndex
        ) + 2;
      writeGroupedExcelIncidents(
        sheet,
        metrics.get("incidentsList"),
        headerStyle,
        rowIndex
      );
    }
  }

  // Ecrit la repartition par type et les incidents dans une feuille de perimetre.
  private void writeGroupedTypeAnalysis(
    Sheet sheet,
    Map<String, Object> metrics,
    CellStyle headerStyle,
    int startRow
  ) {
    Row header = sheet.createRow(startRow++);
    String[] labels = {
      t("report.table.type"),
      t("report.table.incidents"),
      t("report.table.share"),
      t("report.metric.activeIncidents"),
      t("report.metric.closedIncidents"),
      t("report.metric.rejectedIncidents"),
    };
    for (int index = 0; index < labels.length; index++) {
      Cell cell = header.createCell(index);
      cell.setCellValue(labels[index]);
      cell.setCellStyle(headerStyle);
    }
    if (metrics.get("typeSections") instanceof Collection<?> sections) {
      for (Object item : sections) {
        if (!(item instanceof Map<?, ?> section)) continue;
        Row row = sheet.createRow(startRow++);
        row.createCell(0).setCellValue(mapValue(section, "typeName"));
        row.createCell(1).setCellValue(numberValue(section.get("count")));
        row.createCell(2).setCellValue(formatPercentage(section.get("share")));
        row.createCell(3).setCellValue(numberValue(section.get("active")));
        row.createCell(4).setCellValue(numberValue(section.get("closed")));
        row.createCell(5).setCellValue(numberValue(section.get("rejected")));
      }
    }
    writeGroupedExcelIncidents(
      sheet,
      metrics.get("incidentsList"),
      headerStyle,
      startRow + 2
    );
  }

  // Variante groupee (rapport automatique) de la "situation par statut".
  private void writeGroupedStatusOverview(
    Sheet sheet,
    Map<String, Object> metrics,
    CellStyle headerStyle,
    int startRow,
    String createdLabel,
    String periodPhrase,
    String activityTitle
  ) {
    int nextRow = writeStatusSummaryBody(
      sheet.getWorkbook(),
      sheet,
      startRow,
      metrics,
      headerStyle,
      null,
      createdLabel,
      periodPhrase,
      activityTitle
    );
    nextRow = writeGroupedExcelIncidents(
      sheet,
      metrics.get("incidentsList"),
      headerStyle,
      nextRow + 2
    );
    // Chaque compteur de flux adosse a sa liste, comme dans le PDF et l'Excel non groupe.
    Map<String, Object> situation = globalSituation(metrics);
    nextRow = writeGroupedFlowList(
      sheet,
      situation,
      "treatedList",
      "report.statusOverview.flow.treated.list",
      periodPhrase,
      headerStyle,
      nextRow + 2
    );
    nextRow = writeGroupedFlowList(
      sheet,
      situation,
      "resolvedList",
      "report.statusOverview.flow.resolved.list",
      periodPhrase,
      headerStyle,
      nextRow + 2
    );
    writeGroupedFlowList(
      sheet,
      situation,
      "closedList",
      "report.statusOverview.flow.closed.list",
      periodPhrase,
      headerStyle,
      nextRow + 2
    );
  }

  // Ajoute les incidents sous les indicateurs de la feuille groupee. Renvoie la
  // premiere ligne libre pour permettre d'empiler d'autres tableaux en dessous.
  private int writeGroupedExcelIncidents(
    Sheet sheet,
    Object data,
    CellStyle headerStyle,
    int startRow
  ) {
    if (!(data instanceof Collection<?> incidents)) return startRow;
    return writeGroupedIncidentTable(
      sheet,
      t("report.incidents.title"),
      incidents,
      headerStyle,
      startRow
    );
  }

  // Liste de flux (traites/resolus/clotures) sous les indicateurs de la feuille
  // groupee : adosse chaque compteur a sa population, omise si vide.
  private int writeGroupedFlowList(
    Sheet sheet,
    Map<String, Object> situation,
    String key,
    String titleKey,
    String periodPhrase,
    CellStyle headerStyle,
    int startRow
  ) {
    if (
      !(situation.get(key) instanceof Collection<?> incidents) ||
      incidents.isEmpty()
    ) {
      return startRow;
    }
    return writeGroupedIncidentTable(
      sheet,
      t(titleKey, periodPhrase, incidents.size()),
      incidents,
      headerStyle,
      startRow
    );
  }

  // Tableau compact (incident, statut) sous un titre fusionne, pour les feuilles groupees.
  private int writeGroupedIncidentTable(
    Sheet sheet,
    String tableTitle,
    Collection<?> incidents,
    CellStyle headerStyle,
    int startRow
  ) {
    Row title = sheet.createRow(startRow++);
    Cell titleCell = title.createCell(0);
    titleCell.setCellValue(tableTitle);
    titleCell.setCellStyle(headerStyle);
    title.createCell(1).setCellStyle(headerStyle);
    sheet.addMergedRegion(
      new CellRangeAddress(startRow - 1, startRow - 1, 0, 1)
    );

    Row header = sheet.createRow(startRow++);
    header.createCell(0).setCellValue(t("report.table.incident"));
    header.createCell(1).setCellValue(t("report.table.status"));
    header.getCell(0).setCellStyle(headerStyle);
    header.getCell(1).setCellStyle(headerStyle);
    for (Object item : incidents) {
      if (!(item instanceof Map<?, ?> row)) continue;
      Row incidentRow = sheet.createRow(startRow++);
      incidentRow.createCell(0).setCellValue(incidentTitle(row));
      incidentRow
        .createCell(1)
        .setCellValue(
          translateDimensionValue(
            "distributionByStatus",
            mapValue(row, "status")
          )
        );
    }
    return startRow;
  }

  // Produit un nom de feuille Excel valide, court et unique.
  private String safeSheetName(int index, String label) {
    String cleaned = label.replaceAll("[\\\\/?*\\[\\]:]", "-").trim();
    String prefixed = index + "-" + (cleaned.isBlank() ? "rapport" : cleaned);
    return prefixed.substring(0, Math.min(31, prefixed.length()));
  }

  // JSON

  private byte[] generateJson(GeneratedReport report) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("organization", "FINSTAR-CM S.A.");
      payload.put("documentTitle", t("report.title"));
      payload.put("name", report.getName());
      payload.put(
        "type",
        report.getType() != null ? report.getType().getName() : null
      );
      payload.put(
        "contentType",
        report.getContentType() != null
          ? report.getContentType().name()
          : "OPERATIONAL"
      );
      payload.put(
        "generationType",
        report.getGenerationType() != null
          ? report.getGenerationType().name()
          : null
      );
      payload.put("period", formatPeriod(report));
      payload.put("status", report.getStatus());
      payload.put("generatedAt", report.getCreatedAt());
      Map<String, Object> filters = readJson(report.getFilters());
      payload.put("createdBy", displayCreator(report, filters));
      payload.put("metrics", readJson(report.getMetrics()));
      payload.put("filters", filters);
      return objectMapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsBytes(payload);
    } catch (Exception e) {
      log.error("Échec de generer le rapport JSON {}", report.getId(), e);
      throw new BusinessRuleViolationException(
        ErrorCode.EXPORT_ERROR,
        t("reporting.error.generate_json")
      );
    }
  }

  // Helpers

  private Map<String, Object> readJson(String json) {
    if (json == null || json.isBlank()) {
      return new LinkedHashMap<>();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      log.warn(
        "Impossible de analyser le contenu JSON du rapport: {}",
        e.getMessage()
      );
      return new LinkedHashMap<>();
    }
  }

  // Aplatit les filtres du rapport pour les lignes du document.

  private Map<String, Object> flattenFilters(Map<String, Object> payload) {
    Map<String, Object> rows = new LinkedHashMap<>();
    Object filters = payload.get("filters");
    if (filters instanceof Map<?, ?> filterMap) {
      filterMap.forEach((key, value) -> {
        String filterKey = String.valueOf(key);
        if (
          HIDDEN_FILTER_KEYS.contains(filterKey) ||
          !isMeaningfulFilterValue(value)
        ) {
          return;
        }
        rows.put(filterLabel(filterKey), formatFilterValue(filterKey, value));
      });
    }
    return rows;
  }

  // Ecarte les valeurs de filtre absentes ou uniquement techniques.
  private boolean isMeaningfulFilterValue(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof Collection<?> collection) {
      return collection.stream().anyMatch(this::isMeaningfulFilterValue);
    }
    String text = value.toString().trim();
    return !text.isEmpty() && !"-".equals(text) && !"null".equalsIgnoreCase(text);
  }

  // Formate les valeurs de filtre en traduisant les enumerations fonctionnelles.
  private String formatFilterValue(String filterKey, Object value) {
    if (value instanceof Collection<?> collection) {
      return collection
        .stream()
        .filter(this::isMeaningfulFilterValue)
        .map(item -> formatFilterValue(filterKey, item))
        .reduce((left, right) -> left + ", " + right)
        .orElse("-");
    }
    String rawValue = value.toString();
    return switch (filterKey) {
      case "statuses" -> translateDimensionValue("distributionByStatus", rawValue);
      case "criticalities" ->
        translateDimensionValue("distributionByCriticality", rawValue);
      default -> rawValue;
    };
  }

  // Retrouve la langue figee lors de la demande de generation du rapport.
  private Locale resolveDocumentLocale(
    GeneratedReport report,
    Locale fallbackLocale
  ) {
    Map<String, Object> filters = readJson(report.getFilters());
    Object language = filters.get("documentLanguage");
    if (language instanceof String code && !code.isBlank()) {
      return Locale.forLanguageTag(code);
    }
    return Locale.FRENCH;
  }

  // Formate une valeur mensuelle de rapport.

  private String formatMonthlyValue(String title, Object value) {
    if (!(value instanceof Number n)) {
      return "-";
    }
    String avgTitle = t("report.metric.monthlyAvgClosureHours");
    return String.format(
      LocaleContextHolder.getLocale(),
      avgTitle.equals(title) ? "%.1f" : "%.0f",
      n.doubleValue()
    );
  }

  // Formate une periode de rapport.

  private String formatPeriod(GeneratedReport report) {
    String start =
      report.getPeriodStart() != null
        ? report.getPeriodStart().format(DATE_FMT)
        : null;
    String end =
      report.getPeriodEnd() != null
        ? report.getPeriodEnd().format(DATE_FMT)
        : null;
    if (start == null && end == null) {
      return t("report.period.unspecified");
    }
    return t(
      "report.period.range",
      start != null ? start : "-",
      end != null ? end : "-"
    );
  }

  // Fournit une lecture tolerante pour les donnees du domaine rapport document generator.

  private String safe(String value) {
    return value != null ? value : "-";
  }
}
