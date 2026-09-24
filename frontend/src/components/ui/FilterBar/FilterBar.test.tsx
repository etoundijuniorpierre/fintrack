// Tests frontend : verifie le comportement de filter bar.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import FilterBar from "./FilterBar";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

// Prepare l'affichage lisible de filter bar.test.
const renderFilterBar = (activeCount?: number) =>
  renderWithProviders(
    <FilterBar activeCount={activeCount}>
      <input data-testid="filter-input-1" placeholder="Search..." />
      <select data-testid="filter-select-1" aria-label="Status filter">
        <option value="">All</option>
        <option value="active">Active</option>
      </select>
      <button data-testid="filter-button-1">Apply</button>
    </FilterBar>,
  );

describe("FilterBar — keyboard navigation (AC-16.4)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("filter controls are focusable", () => {
    it("input filter control is focusable", () => {
      renderFilterBar();
      const input = screen.getByTestId("filter-input-1");
      input.focus();
      expect(document.activeElement).toBe(input);
    });

    it("select filter control is focusable", () => {
      renderFilterBar();
      const select = screen.getByTestId("filter-select-1");
      select.focus();
      expect(document.activeElement).toBe(select);
    });

    it("button filter control is focusable", () => {
      renderFilterBar();
      const button = screen.getByTestId("filter-button-1");
      button.focus();
      expect(document.activeElement).toBe(button);
    });
  });

  describe("Tab key navigates through filter controls", () => {
    it("Tab moves focus from first to second filter control", async () => {
      renderFilterBar();

      const input = screen.getByTestId("filter-input-1");
      input.focus();
      expect(document.activeElement).toBe(input);

      await userEvent.tab();

      expect(document.activeElement).toBe(
        screen.getByTestId("filter-select-1"),
      );
    });

    it("Tab moves focus from second to third filter control", async () => {
      renderFilterBar();

      const select = screen.getByTestId("filter-select-1");
      select.focus();
      expect(document.activeElement).toBe(select);

      await userEvent.tab();

      expect(document.activeElement).toBe(
        screen.getByTestId("filter-button-1"),
      );
    });

    it("Shift+Tab moves focus backwards through filter controls", async () => {
      renderFilterBar();

      const button = screen.getByTestId("filter-button-1");
      button.focus();
      expect(document.activeElement).toBe(button);

      await userEvent.tab({ shift: true });

      expect(document.activeElement).toBe(
        screen.getByTestId("filter-select-1"),
      );
    });
  });

  describe("active count badge", () => {
    it("renders badge when activeCount > 0", () => {
      renderFilterBar(3);

      expect(screen.getByLabelText("3 active filters")).toBeInTheDocument();
    });

    it("does not render badge when activeCount is 0", () => {
      renderFilterBar(0);
      expect(screen.queryByLabelText(/active filter/)).not.toBeInTheDocument();
    });

    it("does not render badge when activeCount is undefined", () => {
      renderFilterBar(undefined);
      expect(screen.queryByLabelText(/active filter/)).not.toBeInTheDocument();
    });
  });

  describe("accessibility", () => {
    it('has role="search" on the container', () => {
      renderFilterBar();
      expect(screen.getByRole("search")).toBeInTheDocument();
    });

    it('has aria-label="Filters" on the container', () => {
      renderFilterBar();
      expect(
        screen.getByRole("search", { name: "Filters" }),
      ).toBeInTheDocument();
    });
  });
});
