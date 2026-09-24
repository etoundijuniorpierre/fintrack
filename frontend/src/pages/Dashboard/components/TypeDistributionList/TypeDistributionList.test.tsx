// Tests frontend : verifie le comportement de type distribution list.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import TypeDistributionList from "./TypeDistributionList";
import type { TypeDistributionItem } from "../../../../types/dashboard";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

// Fabrique une fixture de test pour type distribution list.test.
const makeItem = (
  overrides: Partial<TypeDistributionItem> = {},
): TypeDistributionItem => ({
  typeId: "type-1",
  typeName: "Infrastructure",
  count: 10,
  ...overrides,
});

describe("TypeDistributionList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should show skeleton when isLoading is true", () => {
    const { container } = renderWithProviders(
      <TypeDistributionList
        distribution={[]}
        isLoading={true}
        isError={false}
      />,
    );
    expect(container.querySelector('[aria-busy="true"]')).toBeInTheDocument();
  });

  it("should show empty state when isError is true", () => {
    renderWithProviders(
      <TypeDistributionList
        distribution={[]}
        isLoading={false}
        isError={true}
      />,
    );
    expect(
      screen.getByText("dashboard.typeDistribution.empty"),
    ).toBeInTheDocument();
  });

  it("should show empty state when distribution is empty", () => {
    renderWithProviders(
      <TypeDistributionList
        distribution={[]}
        isLoading={false}
        isError={false}
      />,
    );
    expect(
      screen.getByText("dashboard.typeDistribution.empty"),
    ).toBeInTheDocument();
  });

  it("should render type names and counts when distribution has items", () => {
    const distribution = [
      makeItem({ typeId: "t1", typeName: "Infrastructure", count: 15 }),
      makeItem({ typeId: "t2", typeName: "Sécurité", count: 8 }),
    ];
    renderWithProviders(
      <TypeDistributionList
        distribution={distribution}
        isLoading={false}
        isError={false}
      />,
    );
    expect(screen.getByText("Infrastructure")).toBeInTheDocument();
    expect(screen.getByText("Sécurité")).toBeInTheDocument();
    expect(screen.getByText("15")).toBeInTheDocument();
    expect(screen.getByText("8")).toBeInTheDocument();
  });

  it("should render items sorted by count descending when distribution is unsorted", () => {
    const distribution = [
      makeItem({ typeId: "t1", typeName: "Faible", count: 2 }),
      makeItem({ typeId: "t2", typeName: "Élevé", count: 20 }),
      makeItem({ typeId: "t3", typeName: "Moyen", count: 10 }),
    ];
    renderWithProviders(
      <TypeDistributionList
        distribution={distribution}
        isLoading={false}
        isError={false}
      />,
    );
    const items = screen.getAllByRole("listitem");
    expect(items[0]).toHaveTextContent("Élevé");
    expect(items[1]).toHaveTextContent("Moyen");
    expect(items[2]).toHaveTextContent("Faible");
  });

  it("should render a progress bar for each item when distribution has multiple items", () => {
    const distribution = [
      makeItem({ typeId: "t1", typeName: "Type A", count: 30 }),
      makeItem({ typeId: "t2", typeName: "Type B", count: 70 }),
    ];
    const { container } = renderWithProviders(
      <TypeDistributionList
        distribution={distribution}
        isLoading={false}
        isError={false}
      />,
    );
    const progressBars = container.querySelectorAll(".ant-progress");
    expect(progressBars).toHaveLength(2);
  });

  it("should render without crashing when distribution has a single item", () => {
    renderWithProviders(
      <TypeDistributionList
        distribution={[makeItem({ typeName: "Unique", count: 1 })]}
        isLoading={false}
        isError={false}
      />,
    );
    expect(screen.getByText("Unique")).toBeInTheDocument();
    expect(screen.getByText("1")).toBeInTheDocument();
  });
});
