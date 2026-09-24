// Tests : modale de detail d'un rapport (infos generales, metriques, incidents).

import { screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import type { ReportResponse } from "../../../../api/reporting/types";
import ReportDetailModal from "./ReportDetailModal";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

const RECORD = {
  id: "rep-1",
  name: "Rapport mensuel mai",
  type: "INCIDENT",
  format: "PDF",
  status: "AVAILABLE",
  period: "2024-05",
  generatedAt: "2024-06-01T08:00:00Z",
  scope: "agency",
  recipients: ["ops@finstar.com"],
  metrics: {
    totalIncidents: 5,
    avgResolutionHours: 12.34,
    incidentsList: [
      {
        id: "i1",
        title: "Incident A",
        status: "CLOSED",
        closedAt: "2024-05-20T10:00:00Z",
        resolutionDescription: "Corrige",
      },
    ],
  },
} as unknown as ReportResponse;

describe("ReportDetailModal", () => {
  it("should not render the content when open is false", () => {
    renderWithProviders(
      <ReportDetailModal open={false} record={RECORD} onClose={vi.fn()} />,
    );
    expect(screen.queryByText("Rapport mensuel mai")).not.toBeInTheDocument();
  });

  it("should render the report name and general info", () => {
    renderWithProviders(
      <ReportDetailModal open record={RECORD} onClose={vi.fn()} />,
    );
    expect(screen.getByText("reports.detail.title")).toBeInTheDocument();
    expect(screen.getByText("Rapport mensuel mai")).toBeInTheDocument();
    expect(
      screen.getByText("reports.contentType.OPERATIONAL"),
    ).toBeInTheDocument();
  });

  it("should display the incidents count derived from incidentsList", () => {
    renderWithProviders(
      <ReportDetailModal open record={RECORD} onClose={vi.fn()} />,
    );
    const countItem = screen
      .getByText("reports.detail.incidentsCount")
      .closest("tr");
    expect(countItem).toHaveTextContent("1");
  });

  it("should read a legacy report and render its closure delay in hours", () => {
    renderWithProviders(
      <ReportDetailModal open record={RECORD} onClose={vi.fn()} />,
    );
    expect(
      screen.getByText("reports.metrics.totalIncidents"),
    ).toBeInTheDocument();
    expect(screen.getByText("5")).toBeInTheDocument();
    expect(
      screen.getByText("reports.metrics.avgClosureHours"),
    ).toBeInTheDocument();
    expect(screen.getByText("12h20")).toBeInTheDocument();
  });

  it("should list the recipients when they exist", () => {
    renderWithProviders(
      <ReportDetailModal open record={RECORD} onClose={vi.fn()} />,
    );
    expect(screen.getByText("ops@finstar.com")).toBeInTheDocument();
  });

  it("should render the table of incidents included in the report", () => {
    renderWithProviders(
      <ReportDetailModal open record={RECORD} onClose={vi.fn()} />,
    );
    expect(screen.getByText("Incident A")).toBeInTheDocument();
  });

  it("should render no metrics when the report carries none", () => {
    const noMetrics = {
      ...RECORD,
      metrics: undefined,
    } as unknown as ReportResponse;
    renderWithProviders(
      <ReportDetailModal open record={noMetrics} onClose={vi.fn()} />,
    );
    expect(
      screen.queryByText("reports.metrics.totalIncidents"),
    ).not.toBeInTheDocument();
  });
});
