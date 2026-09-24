// Tests frontend : verifie le comportement de tableau de bord graphs.test.

import { describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import DashboardGraphs from "./DashboardGraphs";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

// Prepare graph column pour tableau de bord graphs.test.
const graphColumn = (title: string, isMultiple = false) => {
  const elem = isMultiple
    ? screen.getAllByText(title)[0]
    : screen.getByText(title);
  const column = elem.closest(".ant-col");
  expect(column).not.toBeNull();
  return column as HTMLElement;
};

describe("DashboardGraphs", () => {
  it("renders only graph cards enabled by dashboard personalization", () => {
    renderWithProviders(
      <DashboardGraphs
        showTypeDistribution={false}
        showCriticalityDistribution
        showStatusDistribution={false}
        showTopServices={false}
        typeDistribution={{}}
        criticalityDistribution={{ high: 2 }}
        topServices={[]}
      />,
    );

    expect(
      screen.queryByText("dashboard.graphs.type_distribution"),
    ).not.toBeInTheDocument();
    expect(
      screen.getByText("dashboard.graphs.criticality_distribution"),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("dashboard.graphs.top_services"),
    ).not.toBeInTheDocument();
  });

  it("renders local export controls for each visible graph card", () => {
    renderWithProviders(
      <DashboardGraphs
        showTypeDistribution
        showCriticalityDistribution
        showStatusDistribution={false}
        showTopServices={false}
        typeDistribution={{ Fraud: 3 }}
        criticalityDistribution={{ HIGH: 2 }}
        topServices={[]}
      />,
    );

    expect(
      screen.queryByText("dashboard.graphs.export_csv"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("dashboard.graphs.export_png"),
    ).not.toBeInTheDocument();
    expect(
      screen.getAllByLabelText("dashboard.graphs.export_chart_csv"),
    ).toHaveLength(2);
    expect(
      screen.getAllByLabelText("dashboard.graphs.export_chart_png"),
    ).toHaveLength(2);
  });

  it("lays standard graph cards two per row and expands a lonely last graph", () => {
    renderWithProviders(
      <DashboardGraphs
        showTypeDistribution
        showCriticalityDistribution
        showStatusDistribution
        showTopServices={false}
        typeDistribution={{ Fraud: 3 }}
        criticalityDistribution={{ HIGH: 2 }}
        statusDistribution={{ OPEN: 1 }}
        topServices={[]}
      />,
    );

    expect(graphColumn("dashboard.graphs.type_distribution")).toHaveClass(
      "ant-col-lg-12",
    );
    expect(
      graphColumn("dashboard.graphs.criticality_distribution"),
    ).toHaveClass("ant-col-lg-12");
    expect(graphColumn("dashboard.graphs.status_distribution")).toHaveClass(
      "ant-col-lg-24",
    );
  });

  it("shows the graph empty state when all graph widgets are disabled", () => {
    renderWithProviders(
      <DashboardGraphs
        showTypeDistribution={false}
        showCriticalityDistribution={false}
        showStatusDistribution={false}
        showTopServices={false}
        typeDistribution={{}}
        criticalityDistribution={{}}
        topServices={[]}
      />,
    );

    expect(screen.getByText("dashboard.graphs.empty")).toBeInTheDocument();
  });

  it("renders the director graphs (aging, cohort, workload) when enabled", () => {
    renderWithProviders(
      <DashboardGraphs
        showTypeDistribution={false}
        showCriticalityDistribution={false}
        showStatusDistribution={false}
        showTopServices={false}
        showAging
        showCohort
        showWorkload
        typeDistribution={{}}
        criticalityDistribution={{}}
        ageDistribution={{ "0-3": 2, ">30": 1 }}
        cohortOutcome={{ closedOnTime: 3, openLate: 1 }}
        workload={[{ name: "Alice", count: 4 }]}
      />,
    );

    expect(screen.getByText("dashboard.graphs.aging")).toBeInTheDocument();
    expect(
      screen.getAllByText("dashboard.graphs.cohort.title")[0],
    ).toBeInTheDocument();
    expect(screen.getByText("dashboard.graphs.workload")).toBeInTheDocument();
    expect(graphColumn("dashboard.graphs.cohort.title", true)).toHaveClass(
      "ant-col-xs-24",
    );
    expect(graphColumn("dashboard.graphs.cohort.title", true)).not.toHaveClass(
      "ant-col-lg-12",
    );
    expect(
      screen.queryByText("dashboard.graphs.empty"),
    ).not.toBeInTheDocument();
  });

  it("renders monthly graph cards even when distribution and ranking widgets are disabled", () => {
    renderWithProviders(
      <DashboardGraphs
        showTypeDistribution={false}
        showCriticalityDistribution={false}
        showStatusDistribution={false}
        showTopServices={false}
        showMonthlyClosures
        showMonthlyAvgClosure
        typeDistribution={{}}
        criticalityDistribution={{}}
        monthlyClosures={[{ month: 5, value: 3 }]}
        monthlyAvgClosureHours={[{ month: 5, value: 4.5 }]}
        year={2026}
      />,
    );

    expect(
      screen.getByText("dashboard.graphs.monthlyClosures"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("dashboard.graphs.monthly_avg_closure"),
    ).toBeInTheDocument();
    expect(graphColumn("dashboard.graphs.monthlyClosures")).toHaveClass(
      "ant-col-lg-12",
    );
    expect(graphColumn("dashboard.graphs.monthly_avg_closure")).toHaveClass(
      "ant-col-lg-12",
    );
    expect(
      screen.queryByText("dashboard.graphs.empty"),
    ).not.toBeInTheDocument();
  });
});
