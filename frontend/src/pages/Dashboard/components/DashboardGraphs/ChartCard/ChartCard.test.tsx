// Tests frontend : verifie le comportement de chart card.test.

import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import ChartCard from "./ChartCard";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

describe("ChartCard", () => {
  it("renders local CSV and PNG export actions when rows are available", () => {
    renderWithProviders(
      <ChartCard
        title="Distribution"
        filename="distribution"
        rows={[{ label: "Open", value: 4, color: "#2563EB" }]}
      >
        <svg aria-label="chart" />
      </ChartCard>,
    );

    expect(screen.getByText("Distribution")).toBeInTheDocument();
    expect(
      screen.getByLabelText("dashboard.graphs.export_chart_csv"),
    ).toBeEnabled();
    expect(
      screen.getByLabelText("dashboard.graphs.export_chart_png"),
    ).toBeEnabled();
  });

  it("disables export actions when the chart has no export rows", () => {
    renderWithProviders(
      <ChartCard title="Empty chart" filename="empty-chart" rows={[]}>
        <div>Aucune donnee</div>
      </ChartCard>,
    );

    expect(
      screen.getByLabelText("dashboard.graphs.export_chart_csv"),
    ).toBeDisabled();
    expect(
      screen.getByLabelText("dashboard.graphs.export_chart_png"),
    ).toBeDisabled();
  });

  it("renders an optional toolbar before export actions", () => {
    renderWithProviders(
      <ChartCard
        title="Monthly"
        filename="monthly"
        rows={[{ label: "June", value: 2 }]}
        toolbar={<button type="button">2026</button>}
      >
        <svg aria-label="chart" />
      </ChartCard>,
    );

    expect(screen.getByRole("button", { name: "2026" })).toBeInTheDocument();
  });
});
