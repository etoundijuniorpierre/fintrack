// Tests frontend : verifie le comportement de kpi card.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import KpiCard from "./KpiCard";
import type { KpiCardProps } from "./KpiCard";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

const defaultProps: KpiCardProps = {
  title: "Total Incidents",
  value: 42,
  icon: <span>icon</span>,
};

// Prepare l'affichage lisible de kpi card.test.
const renderKpiCard = (props: Partial<KpiCardProps> = {}) =>
  renderWithProviders(<KpiCard {...defaultProps} {...props} />);

describe("KpiCard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("value display", () => {
    it("renders the numeric value when value prop is a number", () => {
      renderKpiCard({ value: 99 });
      expect(screen.getByText("99")).toBeInTheDocument();
    });

    it("renders the string value when value prop is a string", () => {
      renderKpiCard({ value: "1,234" });
      expect(screen.getByText("1,234")).toBeInTheDocument();
    });
  });

  describe("subtitle", () => {
    it("renders the subtitle when provided", () => {
      renderKpiCard({ subtitle: "over 48 incidents" });
      expect(screen.getByText("over 48 incidents")).toBeInTheDocument();
    });

    it("does not render a subtitle when absent", () => {
      renderKpiCard({ subtitle: undefined });
      expect(screen.queryByText("over 48 incidents")).not.toBeInTheDocument();
    });
  });

  describe("details", () => {
    it("renders one line per detail below the subtitle", () => {
      renderKpiCard({
        subtitle: "the headline base",
        details: ["30 reported", "18 closed"],
      });
      expect(screen.getByText("the headline base")).toBeInTheDocument();
      expect(screen.getByText("30 reported")).toBeInTheDocument();
      expect(screen.getByText("18 closed")).toBeInTheDocument();
    });

    it("renders nothing extra when details are absent", () => {
      renderKpiCard({ details: undefined });
      expect(screen.queryByText("30 reported")).not.toBeInTheDocument();
    });
  });

  describe("aria-label", () => {
    it("combines title and value in the aria-label", () => {
      renderKpiCard({ title: "Open Incidents", value: 7 });
      expect(screen.getByLabelText("Open Incidents: 7")).toBeInTheDocument();
    });
  });

  describe("trend display", () => {
    it("does not render a trend element when trend prop is undefined", () => {
      renderKpiCard({ trend: undefined });
      expect(screen.queryByText(/[+-]\s*\d+%/)).not.toBeInTheDocument();
    });

    it("renders a positive trend when trend.isUp is true", () => {
      renderKpiCard({ trend: { value: 12, isUp: true } });
      expect(screen.getByText("+ 12%")).toBeInTheDocument();
    });

    it("renders a negative trend when trend.isUp is false", () => {
      renderKpiCard({ trend: { value: 5, isUp: false } });
      expect(screen.getByText("- 5%")).toBeInTheDocument();
    });

    it("renders the comparison label from i18n when trend is present", () => {
      renderKpiCard({ trend: { value: 8, isUp: false } });
      expect(screen.getByText("dashboard.kpi.comparison")).toBeInTheDocument();
    });

    it("does not render the comparison label when trend is absent", () => {
      renderKpiCard({ trend: undefined });
      expect(
        screen.queryByText("dashboard.kpi.comparison"),
      ).not.toBeInTheDocument();
    });
  });
});
