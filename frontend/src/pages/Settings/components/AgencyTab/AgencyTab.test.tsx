// Tests frontend : verifie le comportement de agence tab.test.

import { screen, fireEvent, within } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import AgencyTab from "./AgencyTab";
import { useAuth } from "../../../../hooks/auth/useAuth";
import type { AgencyResponse } from "../../../../api/settings/types";
import { makeSettingsAgency } from "../../../../mocks";
import { APP_ROUTES } from "../../../../utils/constants";

const mockNavigate = vi.fn();
let mockSearchParams = new URLSearchParams();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useSearchParams: () => [mockSearchParams],
  };
});

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(() => ({ hasPermission: () => true })),
}));

const { mockDeleteAgency } = vi.hoisted(() => ({
  mockDeleteAgency: vi.fn(),
}));

vi.mock("../../../../hooks/settings", () => ({
  useAgencies: () => ({
    data: AGENCIES,
    isLoading: false,
  }),
  useDeleteAgency: () => ({ mutate: mockDeleteAgency, isPending: false }),
}));

const AGENCIES: AgencyResponse[] = [
  makeSettingsAgency({
    id: "agency-1",
    name: "Agence Yaoundé",
    code: "PAR",
    isActive: true,
    headOfAgency: {
      id: "u1",
      username: "boss",
      firstName: "Chef",
      lastName: "Agence",
    },
  }),
  makeSettingsAgency({
    id: "agency-2",
    name: "Agence Douala",
    code: "DLA",
    isActive: true,
    headOfAgency: undefined,
  }),
];

// Prepare l'affichage lisible de agence tab.test.
const renderTab = () => renderWithProviders(<AgencyTab />);

describe("AgencyTab", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockSearchParams = new URLSearchParams();
    vi.mocked(useAuth).mockReturnValue({
      hasPermission: () => true,
      user: null,
      isAuthenticated: true,
      hasRole: () => false,
    } as ReturnType<typeof useAuth>);
  });

  it("renders the table and display data", () => {
    renderTab();
    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(screen.getByText("Agence Yaoundé")).toBeInTheDocument();
  });

  it("navigates to create page when add button is clicked", () => {
    renderTab();
    const addButton = screen.getByText("settings.agencies.addButton");
    fireEvent.click(addButton);
    expect(mockNavigate).toHaveBeenCalledWith(
      APP_ROUTES.SETTINGS_AGENCIES_CREATE,
    );
  });

  it("navigates to edit page when edit icon is clicked", () => {
    renderTab();
    const row = screen.getByText("Agence Yaoundé").closest("tr");
    expect(row).not.toBeNull();
    fireEvent.click(within(row!).getByLabelText("settings.buttons.edit"));
    expect(mockNavigate).toHaveBeenCalledWith(
      APP_ROUTES.SETTINGS_AGENCIES_EDIT("agency-1"),
    );
  });

  it("shows delete confirm and calls deleteAgency", async () => {
    renderTab();
    const row = screen.getByText("Agence Yaoundé").closest("tr");
    expect(row).not.toBeNull();
    fireEvent.click(within(row!).getByLabelText("settings.buttons.delete"));
    expect(
      screen.getByText("settings.agencies.messages.delete_confirm"),
    ).toBeInTheDocument();

    const confirmBtn = screen.getByText("settings.buttons.confirm");
    fireEvent.click(confirmBtn);
    expect(mockDeleteAgency).toHaveBeenCalledWith("agency-1");
  });

  it("filters to agencies without head when missingHead param is set", () => {
    mockSearchParams = new URLSearchParams("missingHead=true");
    renderTab();
    expect(screen.queryByText("Agence Yaoundé")).not.toBeInTheDocument();
    expect(screen.getByText("Agence Douala")).toBeInTheDocument();
  });

  it("shows all agencies when missingHead param is absent", () => {
    renderTab();
    expect(screen.getByText("Agence Yaoundé")).toBeInTheDocument();
    expect(screen.getByText("Agence Douala")).toBeInTheDocument();
  });
});
