package com.fintrack.reporting.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.context.support.ResourceBundleMessageSource;
import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.constant.ReportGenerationType;
import com.fintrack.reporting.model.constant.ReportType;
import com.fintrack.reporting.model.entity.GeneratedReport;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import tools.jackson.databind.json.JsonMapper;

class ReportDocumentGeneratorTest {

  private static final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
  static {
    messageSource.setBasenames("i18n/messages", "i18n/notifications");
    messageSource.setDefaultEncoding("UTF-8");
  }

  private final ReportDocumentGenerator generator = new ReportDocumentGenerator(
    new JsonMapper(),
    messageSource
  );

  private GeneratedReport sampleReport(ReportFormat format) {
    GeneratedReport report = new GeneratedReport();
    report.setId(UUID.randomUUID());
    report.setName("Rapport mensuel mai");
    report.setType(ReportType.MONTHLY);
    report.setFormat(format);
    report.setGenerationType(ReportGenerationType.MANUAL);
    report.setStatus("AVAILABLE");
    report.setCreatedBy(UUID.randomUUID());
    report.setCreatedAt(LocalDateTime.now());
    report.setPeriodStart(LocalDateTime.now().minusDays(30));
    report.setPeriodEnd(LocalDateTime.now());
    report.setMetrics("{\"Incidents totaux\":42,\"Incidents résolus\":37}");
    return report;
  }

  @Test
  void shouldGenerateValidPdf() {
    byte[] pdf = generator.generate(sampleReport(ReportFormat.PDF));

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
  }

  @Test
  void shouldGenerateValidExcel() {
    byte[] xlsx = generator.generate(sampleReport(ReportFormat.EXCEL));

    assertThat(xlsx).isNotEmpty();
    // Verifie la signature ZIP attendue au debut du conteneur XLSX.
    assertThat(xlsx[0]).isEqualTo((byte) 'P');
    assertThat(xlsx[1]).isEqualTo((byte) 'K');
  }

  @Test
  void shouldUsePreciseCreatedLabelInOperationalReport() throws Exception {
    LocaleContextHolder.setLocale(Locale.FRENCH);
    try {
      GeneratedReport report = sampleReport(ReportFormat.EXCEL);
      report.setType(ReportType.WEEKLY);
      report.setContentType(ReportContentType.OPERATIONAL);
      report.setMetrics("{\"inflow\":5,\"outflow\":3,\"activeIncidents\":2}");

      Set<String> cells = readAllStrings(generator.generate(report));

      // Le flux entrant porte le libelle precis de la periode (hebdomadaire),
      // plus le libelle generique.
      assertThat(cells).contains("Incidents créés cette semaine");
      assertThat(cells).doesNotContain("Nouveaux incidents");
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  // Collecte toutes les valeurs texte d'un classeur xlsx.
  private Set<String> readAllStrings(byte[] xlsx) throws Exception {
    Set<String> texts = new HashSet<>();
    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      for (int s = 0; s < wb.getNumberOfSheets(); s++) {
        Sheet sheet = wb.getSheetAt(s);
        for (Row row : sheet) {
          for (Cell cell : row) {
            if (cell.getCellType() == CellType.STRING) {
              texts.add(cell.getStringCellValue());
            }
          }
        }
      }
    }
    return texts;
  }

  // Metriques "situation par statut" avec deux familles et un incident en retard.
  private String statusOverviewMetrics() {
    return (
      "{\"reportContentType\":\"INCIDENT_STATUS_OVERVIEW\",\"totalIncidents\":3,\"overdueCount\":1," +
      "\"categorySections\":[" +
      "{\"key\":\"IN_PROGRESS\",\"count\":2,\"share\":66.7,\"incidents\":[" +
      "{\"reference\":\"FT-I-2026-0001\",\"title\":\"A\",\"agency\":{\"name\":\"Alpha\"},\"createdAt\":\"2026-08-03T10:00:00\",\"overdue\":false}," +
      "{\"reference\":\"FT-I-2026-0002\",\"title\":\"B\",\"agency\":{\"name\":\"Alpha\"},\"createdAt\":\"2026-08-04T10:00:00\",\"overdue\":true}]}," +
      "{\"key\":\"BLOCKED\",\"count\":1,\"share\":33.3,\"incidents\":[" +
      "{\"reference\":\"FT-I-2026-0003\",\"title\":\"C\",\"agency\":{\"name\":\"Beta\"},\"createdAt\":\"2026-08-05T10:00:00\",\"overdue\":false}]}]," +
      "\"incidentsList\":[" +
      "{\"reference\":\"FT-I-2026-0001\",\"title\":\"A\",\"agency\":{\"name\":\"Alpha\"},\"createdAt\":\"2026-08-03T10:00:00\",\"overdue\":false}," +
      "{\"reference\":\"FT-I-2026-0002\",\"title\":\"B\",\"agency\":{\"name\":\"Alpha\"},\"createdAt\":\"2026-08-04T10:00:00\",\"overdue\":true}," +
      "{\"reference\":\"FT-I-2026-0003\",\"title\":\"C\",\"agency\":{\"name\":\"Beta\"},\"createdAt\":\"2026-08-05T10:00:00\",\"overdue\":false}]}"
    );
  }

  @Test
  void shouldGenerateStatusOverviewPdfWithoutError() {
    GeneratedReport report = sampleReport(ReportFormat.PDF);
    report.setType(ReportType.WEEKLY);
    report.setContentType(ReportContentType.INCIDENT_STATUS_OVERVIEW);
    report.setMetrics(statusOverviewMetrics());

    byte[] pdf = generator.generate(report);

    // Le rendu (blocs familles insecables + section retards dediee) aboutit.
    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
  }

  @Test
  void shouldGenerateStatusOverviewPdfWithGlobalSituation() {
    GeneratedReport report = sampleReport(ReportFormat.PDF);
    report.setType(ReportType.DAILY);
    report.setContentType(ReportContentType.INCIDENT_STATUS_OVERVIEW);
    // Metriques enrichies du bloc "situation globale" (flux periode + backlog + retard).
    report.setMetrics(
      statusOverviewMetrics().replaceFirst(
        "\\}$",
        ",\"globalSituation\":{\"treatedInPeriod\":4,\"resolvedInPeriod\":2," +
        "\"closedInPeriod\":1,\"untreatedBacklog\":12,\"overdueBacklog\":5}}"
      )
    );

    byte[] pdf = generator.generate(report);

    // Le rendu du bloc situation globale (KPI flux + backlog) aboutit.
    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
  }

  @Test
  void shouldGenerateStatusOverviewExcelWithSummaryAndCreatedSheet()
    throws Exception {
    LocaleContextHolder.setLocale(Locale.FRENCH);
    try {
      GeneratedReport report = sampleReport(ReportFormat.EXCEL);
      report.setType(ReportType.WEEKLY);
      report.setContentType(ReportContentType.INCIDENT_STATUS_OVERVIEW);
      report.setMetrics(
        statusOverviewMetrics().replaceFirst(
          "\\}$",
          ",\"globalSituation\":{\"treatedInPeriod\":4,\"resolvedInPeriod\":2," +
          "\"closedInPeriod\":1,\"untreatedBacklog\":12,\"overdueBacklog\":5}}"
        )
      );

      byte[] xlsx = generator.generate(report);

      try (
        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))
      ) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
          names.add(wb.getSheetName(i));
        }
        // Une feuille de synthese + la liste plate des crees. Plus de feuilles par
        // famille dans l'activite : la famille appartient a la situation globale.
        assertThat(names).contains("Situation par statut", "Créés période");
        assertThat(names).doesNotContain("En cours", "Bloqués");
        Sheet status = wb.getSheet("Situation par statut");
        // Titre de section date (hebdomadaire) + libelles/valeurs reperes par scan.
        Map<String, Double> kv = statusSummaryKeyValues(status);
        assertThat(status.getRow(1).getCell(0).getStringCellValue()).isEqualTo(
          "Activité de la semaine"
        );
        assertThat(kv).containsEntry("Incidents créés cette semaine", 3.0);
        assertThat(kv).containsEntry("Traités cette semaine", 4.0);
        assertThat(kv).containsEntry("Encore ouverts", 12.0);
        assertThat(kv).containsEntry("En retard", 5.0);
      }
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  @Test
  void shouldRenderFlowListSheetsBackingTheCounters() throws Exception {
    LocaleContextHolder.setLocale(Locale.FRENCH);
    try {
      GeneratedReport report = sampleReport(ReportFormat.EXCEL);
      report.setType(ReportType.DAILY);
      report.setContentType(ReportContentType.INCIDENT_STATUS_OVERVIEW);
      // Situation globale portant la liste reelle adossant le compteur "traités".
      report.setMetrics(
        statusOverviewMetrics().replaceFirst(
          "\\}$",
          ",\"globalSituation\":{\"treatedInPeriod\":1,\"resolvedInPeriod\":0," +
          "\"closedInPeriod\":0,\"untreatedBacklog\":12,\"overdueBacklog\":5," +
          "\"treatedList\":[{\"reference\":\"FT-I-2026-0043\",\"title\":\"Ecran\"," +
          "\"agency\":{\"name\":\"Nkolbisson\"},\"status\":\"TREATED\"," +
          "\"createdAt\":\"2026-08-13T10:56:00\",\"overdue\":false}]," +
          "\"resolvedList\":[],\"closedList\":[]}}"
        )
      );

      byte[] xlsx = generator.generate(report);

      try (
        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))
      ) {
        // Le compteur "Traités" est adosse a sa feuille ; les listes vides sont omises.
        assertThat(wb.getSheet("Traités période")).isNotNull();
        assertThat(wb.getSheet("Résolus période")).isNull();
        assertThat(wb.getSheet("Clôturés période")).isNull();
        Sheet treated = wb.getSheet("Traités période");
        assertThat(
          treated.getRow(1).getCell(0).getStringCellValue()
        ).isEqualTo("FT-I-2026-0043");
      }
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  // Collecte les couples (libelle texte, valeur numerique) de la feuille de synthese,
  // pour reperer les KPI sans dependre de leur ligne exacte.
  private static Map<String, Double> statusSummaryKeyValues(Sheet sheet) {
    Map<String, Double> values = new java.util.HashMap<>();
    for (Row row : sheet) {
      Cell key = row.getCell(0);
      Cell value = row.getCell(1);
      if (
        key != null &&
        key.getCellType() == CellType.STRING &&
        value != null &&
        value.getCellType() == CellType.NUMERIC
      ) {
        values.putIfAbsent(key.getStringCellValue(), value.getNumericCellValue());
      }
    }
    return values;
  }

  @Test
  void shouldGenerateJsonWithOrganization() {
    byte[] json = generator.generate(sampleReport(ReportFormat.JSON));

    assertThat(new String(json))
      .contains("FINSTAR-CM S.A.")
      .contains("Incidents totaux");
  }

  @Test
  void shouldGenerateIncidentTypeAnalysisPdf() {
    GeneratedReport report = incidentTypeReport(ReportFormat.PDF);

    byte[] pdf = generator.generate(report);

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
  }

  @Test
  void shouldGenerateIncidentTypeAnalysisWorkbookWithSummaryAndIncidents()
    throws Exception {
    GeneratedReport report = incidentTypeReport(ReportFormat.EXCEL);

    byte[] xlsx = generator.generate(report);

    try (
      XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))
    ) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
      assertThat(workbook.getSheetName(0)).contains("type");
      assertThat(workbook.getSheetName(1)).containsIgnoringCase("incident");
      assertThat(workbook.getSheetAt(0).getRow(3).getCell(0).getStringCellValue())
        .isEqualTo("Incident de caisse");
    }
  }

  // Cree un rapport thematique representatif des donnees produites par l'agregateur.
  private GeneratedReport incidentTypeReport(ReportFormat format) {
    GeneratedReport report = sampleReport(format);
    report.setContentType(ReportContentType.INCIDENT_TYPE_ANALYSIS);
    report.setMetrics(
      "{" +
        "\"reportContentType\":\"INCIDENT_TYPE_ANALYSIS\"," +
        "\"totalIncidents\":1," +
        "\"representedTypeCount\":1," +
        "\"dominantType\":\"Incident de caisse\"," +
        "\"dominantTypeShare\":100.0," +
        "\"typeSections\":[{" +
        "\"typeId\":\"type-1\",\"typeName\":\"Incident de caisse\"," +
        "\"count\":1,\"share\":100.0,\"active\":1,\"closed\":0,\"rejected\":0," +
        "\"incidents\":[{\"title\":\"Ecart de caisse\",\"status\":\"OPEN\",\"type\":{\"displayName\":\"Incident de caisse\"}}]}]," +
        "\"incidentsList\":[{\"title\":\"Ecart de caisse\",\"status\":\"OPEN\",\"type\":{\"displayName\":\"Incident de caisse\"}}]}"
    );
    return report;
  }

  private GeneratedReport reportWithNewMetrics(ReportFormat format) {
    GeneratedReport report = sampleReport(format);
    report.setMetrics(
      "{" +
        "\"inflow\":3,\"outflow\":2,\"rejectedIncidents\":0,\"netBacklog\":1," +
        "\"activeIncidents\":8,\"blockedIncidents\":0," +
        "\"avgClosureHours\":7.0," +
        "\"medianClosureHours\":5.0," +
        "\"p90ClosureHours\":12.0," +
        "\"avgResolutionHours\":6.0," +
        "\"avgTimeToFirstResponse\":2.5," +
        "\"slaComplianceRate\":100.0,\"slaDenominator\":2," +
        "\"transferRate\":50.0,\"transferDenominator\":2," +
        "\"slaBreachNow\":3," +
        "\"distributionByStatus\":{\"OPEN\":2,\"CLOSED\":5}," +
        "\"distributionByCriticality\":{\"LOW\":1,\"MEDIUM\":2,\"HIGH\":3,\"CRITICAL\":4}," +
        "\"ageDistribution\":{\"0-3\":1,\">30\":2}," +
        "\"cohortOutcome\":{\"closedOnTime\":4,\"openLate\":1}," +
        "\"workload\":[{\"name\":\"Alice\",\"count\":6}]," +
        "\"efficiencyScorecard\":{\"delayScore\":80.0,\"qualityScore\":90.0,\"throughputScore\":70.0,\"compositeScore\":80.0}," +
        "\"incidentsList\":[" +
        "{\"title\":\"Ecart de caisse\",\"status\":\"CLOSED\",\"criticality\":\"HIGH\",\"description\":\"Ecart confirme apres controle\"}," +
        "{\"title\":\"Materiel indisponible\",\"status\":\"ASSIGNED\",\"criticality\":\"MEDIUM\"}," +
        "{\"title\":\"Acces applicatif\",\"status\":\"PENDING_VALIDATION\",\"criticality\":\"LOW\"}]" +
        "}"
    );
    return report;
  }

  @Test
  void shouldRenderNewMetricsInPdf() {
    byte[] pdf = generator.generate(reportWithNewMetrics(ReportFormat.PDF));

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
  }

  @Test
  // Garantit que le score composite est présenté avec des libellés métier explicites.
  void shouldUseExplicitOperationalPerformanceLabels() {
    assertThat(
      messageSource.getMessage(
        "report.metric.efficiencyScorecard",
        null,
        Locale.FRENCH
      )
    ).isEqualTo("Score de performance opérationnelle");
    assertThat(
      messageSource.getMessage(
        "report.metric.scorecard.throughput",
        null,
        Locale.FRENCH
      )
    ).isEqualTo("Capacité de clôture");
  }

  @Test
  // Genere les deux documents de reference utilises pour le controle visuel.
  void shouldGeneratePdfPreviewsForVisualInspection() throws Exception {
    Path previewDirectory = Path.of("target", "report-previews");
    Files.createDirectories(previewDirectory);
    GeneratedReport operationalReport = reportWithNewMetrics(ReportFormat.PDF);
    operationalReport.setFilters(
      "{\"documentLanguage\":\"fr\",\"scopeLabel\":\"Global\",\"filters\":{" +
      "\"view\":\"all\",\"criticalities\":[\"HIGH\"],\"statuses\":[]," +
      "\"services\":[],\"subjectUserId\":null}}"
    );

    Files.write(
      previewDirectory.resolve("operational-report.pdf"),
      generator.generate(operationalReport)
    );
    Files.write(
      previewDirectory.resolve("incident-type-report.pdf"),
      generator.generate(incidentTypeReport(ReportFormat.PDF))
    );

    assertThat(previewDirectory.resolve("operational-report.pdf")).exists();
    assertThat(previewDirectory.resolve("incident-type-report.pdf")).exists();
  }

  @Test
  // La description tenait dans une cellule fourre-tout « Cause: x | Description: y »,
  // inexploitable dans un tableur ou l'on trie et filtre colonne par colonne.
  void shouldGiveTheIncidentDescriptionItsOwnExcelColumn() throws Exception {
    LocaleContextHolder.setLocale(Locale.FRENCH);
    try {
      GeneratedReport report = sampleReport(ReportFormat.EXCEL);
      report.setMetrics(
        "{\"incidentsList\":[{\"reference\":\"FT-I-2026-0009\"," +
        "\"title\":\"Ecart de caisse\",\"cause\":\"Erreur de saisie\"," +
        "\"description\":\"Le solde affiche 12 000 XAF de moins que le journal.\"}]}"
      );

      try (
        XSSFWorkbook workbook = new XSSFWorkbook(
          new ByteArrayInputStream(generator.generate(report))
        )
      ) {
        Sheet incidents = workbook.getSheet(
          messageSource.getMessage(
            "report.excel.sheet.incidents",
            null,
            Locale.FRENCH
          )
        );

        Row header = incidents.getRow(0);
        assertThat(header.getCell(9).getStringCellValue()).isEqualTo("Cause");
        assertThat(header.getCell(10).getStringCellValue())
          .isEqualTo("Description");

        Row first = incidents.getRow(1);
        assertThat(first.getCell(9).getStringCellValue())
          .isEqualTo("Erreur de saisie");
        assertThat(first.getCell(10).getStringCellValue())
          .isEqualTo("Le solde affiche 12 000 XAF de moins que le journal.");
      }
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  @Test
  void shouldRenderNewMetricsInExcel() {
    byte[] xlsx = generator.generate(reportWithNewMetrics(ReportFormat.EXCEL));

    assertThat(xlsx).isNotEmpty();
    assertThat(xlsx[0]).isEqualTo((byte) 'P');
    assertThat(xlsx[1]).isEqualTo((byte) 'K');
  }

  @Test
  void shouldDisplayTiedRanksInExcel() throws Exception {
    LocaleContextHolder.setLocale(Locale.FRENCH);
    try {
      GeneratedReport report = sampleReport(ReportFormat.EXCEL);
      report.setMetrics(
        "{\"topResolvers\":[" +
        "{\"name\":\"Alice\",\"count\":3}," +
        "{\"name\":\"Bob\",\"count\":2}," +
        "{\"name\":\"Claire\",\"count\":2}," +
        "{\"name\":\"David\",\"count\":1}]}"
      );

      try (
        XSSFWorkbook workbook = new XSSFWorkbook(
          new ByteArrayInputStream(generator.generate(report))
        )
      ) {
        var ranking = workbook.getSheet(
          messageSource.getMessage(
            "report.excel.sheet.performance",
            null,
            Locale.FRENCH
          )
        );

        assertThat(ranking.getRow(2).getCell(0).getStringCellValue())
          .isEqualTo("1");
        assertThat(ranking.getRow(3).getCell(0).getStringCellValue())
          .isEqualTo("2 ex æquo");
        assertThat(ranking.getRow(4).getCell(0).getStringCellValue())
          .isEqualTo("2 ex æquo");
        assertThat(ranking.getRow(5).getCell(0).getStringCellValue())
          .isEqualTo("4");
      }
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  @Test
  // Verifie la langue du classeur et l'annexe des seuls filtres actifs.
  void shouldLocalizeExcelValuesAndAppendOnlyAppliedFilters() throws Exception {
    GeneratedReport report = reportWithNewMetrics(ReportFormat.EXCEL);
    report.setFilters(
      "{\"documentLanguage\":\"fr\",\"filters\":{" +
      "\"view\":\"all\",\"criticalities\":[\"HIGH\"]," +
      "\"statuses\":[],\"services\":[],\"subjectUserId\":null}}"
    );

    try (
      XSSFWorkbook workbook = new XSSFWorkbook(
        new ByteArrayInputStream(generator.generate(report))
      )
    ) {
      assertThat(workbook.getSheet("Incidents").getRow(1).getCell(6).getStringCellValue())
        .isEqualTo("Clôturé");
      assertThat(workbook.getSheet("Incidents").getRow(1).getCell(7).getStringCellValue())
        .isEqualTo("Élevée");

      var filters = workbook.getSheetAt(workbook.getNumberOfSheets() - 1);
      assertThat(filters.getSheetName()).isEqualTo("Filtres appliqués");
      assertThat(filters.getPhysicalNumberOfRows()).isEqualTo(1);
      assertThat(filters.getRow(0).getCell(0).getStringCellValue())
        .isEqualTo("Criticités");
      assertThat(filters.getRow(0).getCell(1).getStringCellValue())
        .isEqualTo("Élevée");
    }
  }

  @Test
  void shouldRenderEachGroupedEntityInPdf() {
    GeneratedReport report = sampleReport(ReportFormat.PDF);
    report.setMetrics(groupedMetrics());

    byte[] pdf = generator.generate(report);

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
  }

  @Test
  void shouldCreateOneExcelSheetPerGroupedEntity() throws Exception {
    GeneratedReport report = sampleReport(ReportFormat.EXCEL);
    report.setMetrics(groupedMetrics());

    byte[] xlsx = generator.generate(report);

    try (
      XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))
    ) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
      assertThat(workbook.getSheetName(0)).contains("Agence Centre");
      assertThat(workbook.getSheetName(1)).contains("Agence Nord");
    }
  }

  private String groupedMetrics() {
    return (
      "{\"groupBy\":\"agency\",\"groupCount\":2,\"groupedReports\":[" +
      "{\"id\":\"1\",\"label\":\"Agence Centre\",\"metrics\":{\"totalIncidents\":1,\"activeIncidents\":1,\"incidentsList\":[{\"title\":\"Incident caisse\",\"status\":\"OPEN\"}]}}," +
      "{\"id\":\"2\",\"label\":\"Agence Nord\",\"metrics\":{\"totalIncidents\":0,\"activeIncidents\":0,\"incidentsList\":[]}}]}"
    );
  }

  @Test
  void shouldGeneratePdfTemplateWhenNoMetrics() {
    GeneratedReport report = sampleReport(ReportFormat.PDF);
    report.setMetrics(null);

    byte[] pdf = generator.generate(report);

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
  }

  @Test
  void shouldUseProfessionalFallbacksForAutomaticEmptyJsonReport() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);
    try {
      GeneratedReport report = sampleReport(ReportFormat.JSON);
      UUID technicalCreatorId = report.getCreatedBy();
      report.setGenerationType(ReportGenerationType.AUTOMATIC);
      report.setPeriodStart(null);
      report.setPeriodEnd(null);
      report.setMetrics("{}");
      report.setFilters("{\"documentLanguage\":\"en\"}");

      String json = new String(generator.generate(report));

      assertThat(json)
        .contains("Not specified")
        .contains("System generation")
        .doesNotContain(technicalCreatorId.toString());
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }
  @Test
  // Un rapport sans documentLanguage dans ses filtres doit etre genere en francais.
  void shouldDefaultToFrenchWhenDocumentLanguageIsMissing() throws Exception {
    LocaleContextHolder.setLocale(Locale.ENGLISH);
    try {
      GeneratedReport report = reportWithNewMetrics(ReportFormat.EXCEL);
      // Pas de documentLanguage dans les filtres (cas des anciens rapports ou scheduler).
      report.setFilters("{\"filters\":{\"view\":\"all\"}}");

      try (
        XSSFWorkbook workbook = new XSSFWorkbook(
          new ByteArrayInputStream(generator.generate(report))
        )
      ) {
        // Les noms de feuilles doivent etre en francais malgre le LocaleContextHolder en anglais.
        assertThat(workbook.getSheetName(0)).isEqualTo(
          messageSource.getMessage("report.excel.sheet.summary", null, Locale.FRENCH)
        );
      }
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }
}
