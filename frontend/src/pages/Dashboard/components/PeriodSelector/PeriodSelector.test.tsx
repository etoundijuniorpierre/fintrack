// Tests frontend : verifie le comportement de periode selector.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import PeriodSelector from "./PeriodSelector";
import { PeriodType, type PeriodFilter } from "../../../../types/dashboard";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

const defaultFilter: PeriodFilter = { period: PeriodType.LAST_30_DAYS };

// Prepare l'affichage lisible de periode selector.test.
const renderPeriodSelector = (
  value: PeriodFilter = defaultFilter,
  onChange = vi.fn(),
) => {
  return renderWithProviders(
    <PeriodSelector value={value} onChange={onChange} />,
  );
};

describe("PeriodSelector", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("date fields visibility", () => {
    it("should not display date fields when period is LAST_30_DAYS", () => {
      renderPeriodSelector({ period: PeriodType.LAST_30_DAYS });

      expect(
        screen.queryByText("dashboard.period.dateFrom"),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByText("dashboard.period.dateTo"),
      ).not.toBeInTheDocument();
    });

    it("should not display date fields when period is TODAY", () => {
      renderPeriodSelector({ period: PeriodType.TODAY });

      expect(
        screen.queryByText("dashboard.period.dateFrom"),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByText("dashboard.period.dateTo"),
      ).not.toBeInTheDocument();
    });

    it("should not display date fields when period is ALL", () => {
      renderPeriodSelector({ period: PeriodType.ALL });

      expect(
        screen.queryByText("dashboard.period.dateFrom"),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByText("dashboard.period.dateTo"),
      ).not.toBeInTheDocument();
    });

    it("should display date fields when period is CUSTOM", () => {
      renderPeriodSelector({ period: PeriodType.CUSTOM });

      // DatePickers use placeholder instead of visible label text
      expect(
        screen.getByPlaceholderText("dashboard.period.dateFrom"),
      ).toBeInTheDocument();
      expect(
        screen.getByPlaceholderText("dashboard.period.dateTo"),
      ).toBeInTheDocument();
    });

    it("should display two DatePicker inputs when period is CUSTOM", () => {
      renderPeriodSelector({ period: PeriodType.CUSTOM });

      const inputs = screen.getAllByRole("textbox");
      expect(inputs.length).toBeGreaterThanOrEqual(2);
    });
  });

  describe("period select", () => {
    it("should render the Select component", () => {
      renderPeriodSelector();

      expect(screen.getByRole("combobox")).toBeInTheDocument();
    });

    it("should call onChange with the selected period when a non-CUSTOM period is chosen", async () => {
      const onChange = vi.fn();
      renderPeriodSelector(defaultFilter, onChange);

      const select = screen.getByRole("combobox");
      fireEvent.mouseDown(select);

      await waitFor(() => {
        const option = screen.getByText("dashboard.period.today");
        fireEvent.click(option);
      });

      expect(onChange).toHaveBeenCalledWith({ period: PeriodType.TODAY });
    });

    it("should call onChange with CUSTOM period when CUSTOM is selected", async () => {
      const onChange = vi.fn();
      renderPeriodSelector(defaultFilter, onChange);

      const select = screen.getByRole("combobox");
      fireEvent.mouseDown(select);

      await waitFor(() => {
        const option = screen.getByText("dashboard.period.custom");
        fireEvent.click(option);
      });

      expect(onChange).toHaveBeenCalledWith({ period: PeriodType.CUSTOM });
    });
  });

  describe("CUSTOM mode validation", () => {
    it("should not show an error message initially when period is CUSTOM", () => {
      renderPeriodSelector({ period: PeriodType.CUSTOM });

      expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    });

    it("should not call onChange with both dateFrom and dateTo when only one date is provided", () => {
      const onChange = vi.fn();
      renderPeriodSelector({ period: PeriodType.CUSTOM }, onChange);

      const callsWithDates = onChange.mock.calls.filter(
        (call) => call[0]?.dateFrom && call[0]?.dateTo,
      );
      expect(callsWithDates).toHaveLength(0);
    });
  });

  describe("onChange with correct PeriodFilter", () => {
    it("should call onChange with LAST_7_DAYS when LAST_7_DAYS is selected", async () => {
      const onChange = vi.fn();
      renderPeriodSelector(defaultFilter, onChange);

      const select = screen.getByRole("combobox");
      fireEvent.mouseDown(select);

      await waitFor(() => {
        const option = screen.getByText("dashboard.period.last7Days");
        fireEvent.click(option);
      });

      expect(onChange).toHaveBeenCalledWith({ period: PeriodType.LAST_7_DAYS });
    });

    it("should call onChange with ALL when ALL is selected", async () => {
      const onChange = vi.fn();
      renderPeriodSelector(defaultFilter, onChange);

      const select = screen.getByRole("combobox");
      fireEvent.mouseDown(select);

      await waitFor(() => {
        const option = screen.getByText("dashboard.period.all");
        fireEvent.click(option);
      });

      expect(onChange).toHaveBeenCalledWith({ period: PeriodType.ALL });
    });

    it("should call onChange with THIS_MONTH when this month is selected", async () => {
      const onChange = vi.fn();
      renderPeriodSelector(defaultFilter, onChange);

      fireEvent.mouseDown(screen.getByRole("combobox"));
      await waitFor(() =>
        fireEvent.click(screen.getByText("dashboard.period.thisMonth")),
      );

      expect(onChange).toHaveBeenCalledWith({ period: PeriodType.THIS_MONTH });
    });

    it("should call onChange with LAST_30_DAYS when LAST_30_DAYS is selected", async () => {
      const onChange = vi.fn();
      renderPeriodSelector({ period: PeriodType.TODAY }, onChange);

      const select = screen.getByRole("combobox");
      fireEvent.mouseDown(select);

      await waitFor(() => {
        const option = screen.getByText("dashboard.period.last30Days");
        fireEvent.click(option);
      });

      expect(onChange).toHaveBeenCalledWith({
        period: PeriodType.LAST_30_DAYS,
      });
    });
  });

  describe("error display", () => {
    it("should not show any error message initially in CUSTOM mode", () => {
      renderPeriodSelector({ period: PeriodType.CUSTOM });

      expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    });

    it("should not show any error message in non-CUSTOM mode", () => {
      renderPeriodSelector({ period: PeriodType.LAST_30_DAYS });

      expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    });
  });
});
