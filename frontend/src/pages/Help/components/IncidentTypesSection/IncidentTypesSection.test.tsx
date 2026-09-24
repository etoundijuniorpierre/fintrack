import { screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { IncidentTypesSection } from "./IncidentTypesSection";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import { incidentApi } from "../../../../api/incident/incidentApi/incidentApi";
import type { IncidentTypeConfigResponse } from "../../../../api/incident/types";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, opts?: { defaultValue?: string }) =>
      opts?.defaultValue ?? key,
  }),
}));

vi.mock("../../../../api/incident/incidentApi/incidentApi", () => ({
  incidentApi: {
    getIncidentTypes: vi.fn(),
  },
}));

describe("IncidentTypesSection", () => {
  it("renders loading spinner initially", () => {
    vi.mocked(incidentApi.getIncidentTypes).mockReturnValue(
      new Promise(() => {}),
    );
    renderWithProviders(<IncidentTypesSection />);
    expect(screen.getByTestId("loading-spinner")).toBeInTheDocument();
  });

  it("renders error alert on failure", async () => {
    vi.mocked(incidentApi.getIncidentTypes).mockRejectedValue(
      new Error("Failed"),
    );
    renderWithProviders(<IncidentTypesSection />);
    expect(await screen.findByText("common.error")).toBeInTheDocument();
  });

  it("renders the list of active incident types", async () => {
    const mockTypes: Partial<IncidentTypeConfigResponse>[] = [
      {
        id: "1",
        name: "HARDWARE",
        displayName: "Matériel",
        description: "Problème matériel",
        isActive: true,
      },
      {
        id: "2",
        name: "SOFTWARE",
        displayName: "Logiciel",
        description: "Problème logiciel",
        isActive: true,
      },
      {
        id: "3",
        name: "OLD",
        displayName: "Vieux Type",
        description: "Ancien type",
        isActive: false,
      },
    ];
    vi.mocked(incidentApi.getIncidentTypes).mockResolvedValue(
      mockTypes as IncidentTypeConfigResponse[],
    );

    renderWithProviders(<IncidentTypesSection />);
    expect(
      await screen.findByText("help.sections.types.intro"),
    ).toBeInTheDocument();
    expect(screen.getByText("Matériel")).toBeInTheDocument();
    expect(screen.getByText("Problème matériel")).toBeInTheDocument();
    expect(screen.getByText("Logiciel")).toBeInTheDocument();
    expect(screen.queryByText("Vieux Type")).not.toBeInTheDocument();
  });
});
