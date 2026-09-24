// Tests frontend : verifie le comportement de virtualized table.test.

import { screen } from "@testing-library/react";
import { vi, describe, it, expect, beforeEach } from "vitest";
import React from "react";
import VirtualizedTable from "./VirtualizedTable";
import type { ColumnsType } from "antd/es/table";
import { renderWithProviders } from "../../test-utils/renderWithProviders";

vi.mock("react-window", () => ({
  FixedSizeList: ({
    children,
    itemCount,
    itemSize,
    height,
  }: {
    children: (props: {
      index: number;
      style: React.CSSProperties;
    }) => React.ReactNode;
    itemCount: number;
    itemSize: number;
    height: number;
  }) => (
    <div
      data-testid="virtualized-list"
      data-item-count={itemCount}
      data-item-size={itemSize}
      data-height={height}
    >
      {Array.from({ length: Math.min(itemCount, 5) }, (_, index) => (
        <React.Fragment key={index}>
          {children({
            index,
            style: { height: itemSize, top: index * itemSize },
          })}
        </React.Fragment>
      ))}
    </div>
  ),
}));

vi.mock("antd", async (importOriginal) => {
  const actual = await importOriginal<typeof import("antd")>();
  return {
    ...actual,
    Table: ({
      dataSource,
      columns,
      loading,
      pagination,
      showHeader,
      size,
    }: {
      dataSource?: Record<string, unknown>[];
      columns?: ColumnsType<Record<string, unknown>>;
      loading?: boolean;
      pagination?: boolean | object;
      showHeader?: boolean;
      size?: string;
    }) => (
      <div
        data-testid="ant-table"
        data-loading={loading}
        data-pagination={String(pagination ?? "")}
        data-show-header={showHeader}
        data-size={size}
        data-row-count={dataSource?.length ?? 0}
      >
        {showHeader && <div data-testid="table-header">Header</div>}
        {dataSource?.map((item, index) => (
          <div key={String(item.id ?? index)} data-testid="table-row">
            {columns?.map((col, colIndex) => {
              const column = col as {
                dataIndex?: string;
                render?: (
                  val: unknown,
                  record: Record<string, unknown>,
                ) => React.ReactNode;
              };
              const val = column.dataIndex ? item[column.dataIndex] : undefined;
              return (
                <span key={colIndex} data-testid="table-cell">
                  {column.render ? column.render(val, item) : String(val ?? "")}
                </span>
              );
            })}
          </div>
        ))}
      </div>
    ),
  };
});

describe("VirtualizedTable", () => {
  const mockColumns: ColumnsType<Record<string, unknown>> = [
    { title: "Name", dataIndex: "name", key: "name" },
    { title: "Email", dataIndex: "email", key: "email" },
  ];

  const mockData: Record<string, unknown>[] = Array.from(
    { length: 100 },
    (_, index) => ({
      id: `user-${index}`,
      name: `User ${index}`,
      email: `user${index}@test.com`,
    }),
  );

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render loading state when loading prop is true", () => {
    renderWithProviders(
      <VirtualizedTable data={[]} columns={mockColumns} loading={true} />,
    );

    const table = screen.getByTestId("ant-table");
    expect(table).toHaveAttribute("data-loading", "true");
    expect(table).toHaveAttribute("data-row-count", "0");
  });

  it("should render empty table when data is empty", () => {
    renderWithProviders(
      <VirtualizedTable data={[]} columns={mockColumns} loading={false} />,
    );

    const table = screen.getByTestId("ant-table");
    expect(table).toBeInTheDocument();
    expect(table).toHaveAttribute("data-row-count", "0");
    expect(table).toHaveAttribute("data-pagination", "false");
  });

  it("should render virtualized list when data is provided", () => {
    renderWithProviders(
      <VirtualizedTable
        data={mockData}
        columns={mockColumns}
        height={400}
        itemHeight={54}
      />,
    );

    const virtualizedList = screen.getByTestId("virtualized-list");
    expect(virtualizedList).toHaveAttribute("data-item-count", "100");
    expect(virtualizedList).toHaveAttribute("data-item-size", "54");
    expect(virtualizedList).toHaveAttribute("data-height", "400");

    const headerTable = screen.getAllByTestId("ant-table")[0];
    expect(headerTable).toHaveAttribute("data-show-header", "true");
    expect(headerTable).toHaveAttribute("data-row-count", "0");
  });

  it("should use default height and itemHeight when not specified", () => {
    renderWithProviders(
      <VirtualizedTable data={mockData} columns={mockColumns} />,
    );

    const virtualizedList = screen.getByTestId("virtualized-list");
    expect(virtualizedList).toHaveAttribute("data-height", "400");
    expect(virtualizedList).toHaveAttribute("data-item-size", "54");
  });

  it("should render row tables without header when data is provided", () => {
    renderWithProviders(
      <VirtualizedTable data={mockData.slice(0, 5)} columns={mockColumns} />,
    );

    const tables = screen.getAllByTestId("ant-table");
    expect(tables.length).toBeGreaterThan(1);

    const rowTables = tables.slice(1);
    rowTables.forEach((table) => {
      expect(table).toHaveAttribute("data-show-header", "false");
      expect(table).toHaveAttribute("data-size", "small");
      expect(table).toHaveAttribute("data-pagination", "false");
    });
  });

  it("should render efficiently when dataset is large", () => {
    const largeData: Record<string, unknown>[] = Array.from(
      { length: 10000 },
      (_, index) => ({
        id: `user-${index}`,
        name: `User ${index}`,
        email: `user${index}@test.com`,
      }),
    );

    renderWithProviders(
      <VirtualizedTable data={largeData} columns={mockColumns} />,
    );

    const virtualizedList = screen.getByTestId("virtualized-list");
    expect(virtualizedList).toHaveAttribute("data-item-count", "10000");
    expect(screen.getAllByTestId("table-row")).toHaveLength(5);
  });

  it("should update item count when data changes", () => {
    const { rerender } = renderWithProviders(
      <VirtualizedTable data={mockData.slice(0, 50)} columns={mockColumns} />,
    );

    let virtualizedList = screen.getByTestId("virtualized-list");
    expect(virtualizedList).toHaveAttribute("data-item-count", "50");

    rerender(<VirtualizedTable data={mockData} columns={mockColumns} />);

    virtualizedList = screen.getByTestId("virtualized-list");
    expect(virtualizedList).toHaveAttribute("data-item-count", "100");
  });

  it("should apply custom height and itemHeight when specified", () => {
    renderWithProviders(
      <VirtualizedTable
        data={mockData}
        columns={mockColumns}
        height={600}
        itemHeight={80}
      />,
    );

    const virtualizedList = screen.getByTestId("virtualized-list");
    expect(virtualizedList).toHaveAttribute("data-height", "600");
    expect(virtualizedList).toHaveAttribute("data-item-size", "80");
  });
});
