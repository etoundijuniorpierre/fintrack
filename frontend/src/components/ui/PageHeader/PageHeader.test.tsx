// Tests frontend : verifie le comportement de page header.test.

import { type ComponentProps } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import PageHeader from "./PageHeader";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

// Prepare l'affichage lisible de page header.test.
const renderHeader = (props: ComponentProps<typeof PageHeader>) =>
  render(<PageHeader {...props} />);

describe("PageHeader", () => {
  describe("title rendering", () => {
    it("renders the title", () => {
      renderHeader({ title: "Incidents" });
      expect(screen.getByText("Incidents")).toBeInTheDocument();
    });

    it("renders the subtitle when provided", () => {
      renderHeader({ title: "Incidents", subtitle: "Manage all incidents" });
      expect(screen.getByText("Manage all incidents")).toBeInTheDocument();
    });

    it("does not render subtitle when not provided", () => {
      renderHeader({ title: "Incidents" });
      expect(
        screen.queryByText("Manage all incidents"),
      ).not.toBeInTheDocument();
    });
  });

  describe("onBack button", () => {
    it("does not render a back button when onBack is not provided", () => {
      renderHeader({ title: "Dashboard" });
      expect(
        screen.queryByRole("button", { name: "Return" }),
      ).not.toBeInTheDocument();
    });

    it('renders a back button with aria-label "Return" when onBack is provided', () => {
      const onBack = vi.fn();
      renderHeader({ title: "Incidents", onBack });
      const backBtn = screen.getByRole("button", { name: "Return" });
      expect(backBtn).toBeInTheDocument();
    });

    it("calls onBack when back button is clicked", () => {
      const onBack = vi.fn();
      renderHeader({ title: "Incidents", onBack });
      fireEvent.click(screen.getByRole("button", { name: "Return" }));
      expect(onBack).toHaveBeenCalledTimes(1);
    });

    it("uses backLabel as aria-label when provided", () => {
      const onBack = vi.fn();
      renderHeader({ title: "Incidents", onBack, backLabel: "Go back" });
      expect(
        screen.getByRole("button", { name: "Go back" }),
      ).toBeInTheDocument();
    });

    it("does not render visible text for the back button (icon only)", () => {
      const onBack = vi.fn();
      renderHeader({ title: "Incidents", onBack });
      const backBtn = screen.getByRole("button", { name: "Return" });
      // Verifie que le bouton ne contient pas le texte visible "Return".
      expect(backBtn.textContent).toBe("");
    });

    it("renders the back button inline before the title in DOM order", () => {
      const onBack = vi.fn();
      renderHeader({ title: "Incidents", onBack });
      const backBtn = screen.getByRole("button", { name: "Return" });
      const title = screen.getByText("Incidents");
      // Back button and title share the same parent row (inline, same line)
      expect(backBtn.parentElement).toBe(title.parentElement);
      // Back button precedes the title in DOM order
      expect(
        backBtn.compareDocumentPosition(title) &
          Node.DOCUMENT_POSITION_FOLLOWING,
      ).toBeTruthy();
    });
  });

  describe("titleTag slot", () => {
    it("renders the titleTag node when provided", () => {
      renderHeader({
        title: "Incidents",
        titleTag: <span data-testid="title-tag">OPEN</span>,
      });
      expect(screen.getByTestId("title-tag")).toBeInTheDocument();
    });

    it("does not render a titleTag wrapper when titleTag is not provided", () => {
      renderHeader({ title: "Incidents" });
      expect(screen.queryByTestId("title-tag")).not.toBeInTheDocument();
    });
  });

  describe("actions slot — P-1.C", () => {
    it("does not render actions slot when actions prop is absent", () => {
      renderHeader({ title: "Dashboard" });
      expect(
        screen.queryByTestId("page-header-actions"),
      ).not.toBeInTheDocument();
    });

    it("does not render actions slot when actions is an empty array", () => {
      renderHeader({ title: "Dashboard", actions: [] });
      expect(
        screen.queryByTestId("page-header-actions"),
      ).not.toBeInTheDocument();
    });

    it("renders actions slot when actions has one element", () => {
      renderHeader({
        title: "Incidents",
        actions: [<button key="create">Create</button>],
      });
      expect(screen.getByTestId("page-header-actions")).toBeInTheDocument();
      expect(screen.getByText("Create")).toBeInTheDocument();
    });

    it("renders all actions when multiple actions are provided", () => {
      renderHeader({
        title: "Incidents",
        actions: [
          <button key="create">Create</button>,
          <button key="export">Export</button>,
        ],
      });
      expect(screen.getByText("Create")).toBeInTheDocument();
      expect(screen.getByText("Export")).toBeInTheDocument();
    });

    it("actions slot is absent from DOM when actions is empty — P-1.C", () => {
      const { container } = renderHeader({ title: "Settings", actions: [] });

      const actionsContainer = container.querySelector(
        '[data-testid="page-header-actions"]',
      );
      expect(actionsContainer).toBeNull();
    });

    it("actions slot is absent from DOM when actions is undefined — P-1.C", () => {
      const { container } = renderHeader({ title: "Settings" });
      const actionsContainer = container.querySelector(
        '[data-testid="page-header-actions"]',
      );
      expect(actionsContainer).toBeNull();
    });
  });
});
