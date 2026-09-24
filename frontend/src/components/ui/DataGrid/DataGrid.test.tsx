// Tests frontend : verifie le comportement de data grid.test.

import { type ComponentProps, type CSSProperties, type ReactNode } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import userEvent from "@testing-library/user-event";
import { ConfigProvider, App as AntdApp } from "antd";
import type { ColumnsType } from "antd/es/table";
import DataGrid from "./DataGrid";
import EmptyState from "../EmptyState/EmptyState";

vi.mock("react-window", () => ({
  FixedSizeList: ({
    children,
    itemCount,
    itemSize,
  }: {
    children: (params: { index: number; style: CSSProperties }) => ReactNode;
    itemCount: number;
    itemSize: number;
  }) => (
    <div data-testid="virtualized-list">
      {Array.from({ length: Math.min(itemCount, 5) }, (_, i) =>
        children({ index: i, style: { height: itemSize, top: i * itemSize } }),
      )}
    </div>
  ),
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

// Type une ligne de test pour la grille.
interface TestRecord {
  id: string;
  name: string;
  status: string;
}

// Fabrique une fixture de test pour data grid.test.
const makeRecord = (overrides: Partial<TestRecord> = {}): TestRecord => ({
  id: "rec-1",
  name: "Test Record",
  status: "ACTIVE",
  ...overrides,
});

const TEST_COLUMNS: ColumnsType<TestRecord> = [
  { title: "Name", dataIndex: "name", key: "name" },
  { title: "Status", dataIndex: "status", key: "status" },
];

const TEST_DATA: TestRecord[] = [
  makeRecord({ id: "rec-1", name: "Alice", status: "ACTIVE" }),
  makeRecord({ id: "rec-2", name: "Bob", status: "INACTIVE" }),
  makeRecord({ id: "rec-3", name: "Charlie", status: "ACTIVE" }),
];

// Prepare l'affichage lisible de data grid.test.
const renderGrid = (
  props: Partial<ComponentProps<typeof DataGrid<TestRecord>>> = {},
) =>
  render(
    <ConfigProvider>
      <AntdApp>
        <DataGrid<TestRecord>
          data={TEST_DATA}
          columns={TEST_COLUMNS}
          rowKey="id"
          {...props}
        />
      </AntdApp>
    </ConfigProvider>,
  );

describe("DataGrid", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("basic rendering", () => {
    it("renders a table with data", () => {
      renderGrid();
      expect(screen.getByRole("table")).toBeInTheDocument();
    });

    it("renders all data rows", () => {
      renderGrid();
      expect(screen.getByText("Alice")).toBeInTheDocument();
      expect(screen.getByText("Bob")).toBeInTheDocument();
      expect(screen.getByText("Charlie")).toBeInTheDocument();
    });

    it("renders column headers", () => {
      renderGrid();
      expect(screen.getAllByText("Name").length).toBeGreaterThan(0);
      expect(screen.getAllByText("Status").length).toBeGreaterThan(0);
    });
  });

  describe("empty state rendering", () => {
    it("renders the custom emptyState when data is empty", () => {
      renderGrid({
        data: [],
        emptyState: <EmptyState title="No records found" />,
      });
      expect(screen.getByText("No records found")).toBeInTheDocument();
    });

    it("renders the custom emptyState with description", () => {
      renderGrid({
        data: [],
        emptyState: (
          <EmptyState
            title="No records found"
            description="Try adjusting your filters."
          />
        ),
      });
      expect(screen.getByText("No records found")).toBeInTheDocument();
      expect(
        screen.getByText("Try adjusting your filters."),
      ).toBeInTheDocument();
    });

    it("renders default empty state when data is empty and no emptyState prop", () => {
      renderGrid({ data: [] });
      expect(screen.getByRole("table")).toBeInTheDocument();

      expect(screen.queryByText("Alice")).not.toBeInTheDocument();
    });

    it("does not render emptyState when data is not empty", () => {
      renderGrid({
        emptyState: <EmptyState title="No records found" />,
      });
      expect(screen.queryByText("No records found")).not.toBeInTheDocument();
      expect(screen.getByText("Alice")).toBeInTheDocument();
    });
  });

  describe("row click behavior", () => {
    it("calls onRowClick with the correct record when a row is clicked", async () => {
      const onRowClick = vi.fn();
      renderGrid({ onRowClick });

      await userEvent.click(screen.getByRole("row", { name: /Alice/i }));

      expect(onRowClick).toHaveBeenCalledWith(
        expect.objectContaining({ id: "rec-1", name: "Alice" }),
        expect.any(Number),
      );
    });

    it("calls onRowClick with the correct record for each row", async () => {
      const onRowClick = vi.fn();
      renderGrid({ onRowClick });

      await userEvent.click(screen.getByRole("row", { name: /Bob/i }));

      expect(onRowClick).toHaveBeenCalledWith(
        expect.objectContaining({ id: "rec-2", name: "Bob" }),
        expect.any(Number),
      );
    });

    it("does not throw when onRowClick is not provided", async () => {
      renderGrid();
      await expect(
        userEvent.click(screen.getByRole("row", { name: /Alice/i })),
      ).resolves.not.toThrow();
    });

    it("applies pointer cursor style when onRowClick is provided", () => {
      const onRowClick = vi.fn();
      renderGrid({ onRowClick });

      const row = screen.getByRole("row", { name: /Alice/i });

      expect(row).toBeInTheDocument();
    });
  });

  describe("loading state", () => {
    it("renders in loading state without crashing", () => {
      renderGrid({ loading: true });
      expect(screen.getByRole("table")).toBeInTheDocument();
    });
  });

  describe("auto-virtualization", () => {
    it("uses VirtualizedTable when data exceeds threshold", () => {
      const largeData = Array.from({ length: 101 }, (_, i) =>
        makeRecord({ id: `rec-${i}`, name: `Record ${i}`, status: "ACTIVE" }),
      );
      renderGrid({ data: largeData, virtualizationThreshold: 100 });

      expect(screen.getByTestId("virtualized-list")).toBeInTheDocument();
    });

    it("uses regular Table when data is at or below threshold", () => {
      const smallData = Array.from({ length: 10 }, (_, i) =>
        makeRecord({ id: `rec-${i}`, name: `Record ${i}`, status: "ACTIVE" }),
      );
      renderGrid({ data: smallData, virtualizationThreshold: 100 });
      expect(screen.getByRole("table")).toBeInTheDocument();
      expect(screen.queryByTestId("virtualized-list")).not.toBeInTheDocument();
    });
  });

  describe("keyboard navigation (AC-16.4)", () => {
    it("Enter key on a focused row triggers onRowClick", async () => {
      const onRowClick = vi.fn();
      renderGrid({ onRowClick });

      const aliceRow = screen.getByRole("row", { name: /Alice/i });

      expect(aliceRow).toHaveAttribute("tabindex", "0");

      fireEvent.keyDown(aliceRow, { key: "Enter", code: "Enter" });

      expect(onRowClick).toHaveBeenCalledWith(
        expect.objectContaining({ id: "rec-1", name: "Alice" }),
        expect.any(Number),
      );
    });

    it("Enter key on a different row triggers onRowClick with correct record", async () => {
      const onRowClick = vi.fn();
      renderGrid({ onRowClick });

      const bobRow = screen.getByRole("row", { name: /Bob/i });
      fireEvent.keyDown(bobRow, { key: "Enter", code: "Enter" });

      expect(onRowClick).toHaveBeenCalledWith(
        expect.objectContaining({ id: "rec-2", name: "Bob" }),
        expect.any(Number),
      );
    });

    it("Tab key moves focus between rows", async () => {
      renderGrid({ onRowClick: vi.fn() });

      const aliceRow = screen.getByRole("row", { name: /Alice/i });
      const bobRow = screen.getByRole("row", { name: /Bob/i });

      expect(aliceRow).toHaveAttribute("tabindex", "0");
      expect(bobRow).toHaveAttribute("tabindex", "0");

      aliceRow.focus();
      expect(document.activeElement).toBe(aliceRow);

      await userEvent.tab();

      expect(document.activeElement).toBe(bobRow);
    });

    it("rows with onRowClick have tabIndex=0 for keyboard accessibility", () => {
      const onRowClick = vi.fn();
      renderGrid({ onRowClick });

      const aliceRow = screen.getByRole("row", { name: /Alice/i });
      const bobRow = screen.getByRole("row", { name: /Bob/i });
      const charlieRow = screen.getByRole("row", { name: /Charlie/i });

      expect(aliceRow).toHaveAttribute("tabindex", "0");
      expect(bobRow).toHaveAttribute("tabindex", "0");
      expect(charlieRow).toHaveAttribute("tabindex", "0");
    });

    it("rows without onRowClick do not have tabIndex", () => {
      renderGrid();

      const aliceRow = screen.getByRole("row", { name: /Alice/i });
      expect(aliceRow).not.toHaveAttribute("tabindex");
    });

    it("non-Enter keys do not trigger onRowClick", () => {
      const onRowClick = vi.fn();
      renderGrid({ onRowClick });

      const aliceRow = screen.getByRole("row", { name: /Alice/i });
      fireEvent.keyDown(aliceRow, { key: " ", code: "Space" });
      fireEvent.keyDown(aliceRow, { key: "ArrowDown", code: "ArrowDown" });

      expect(onRowClick).not.toHaveBeenCalled();
    });
  });
});
