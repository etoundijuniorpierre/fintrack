// Tests frontend : verifie le comportement de incident type details page.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import IncidentTypeDetailsPage from "./IncidentTypeDetailsPage";

const mockNavigate = vi.fn();
const mockDeleteIncidentType = vi.fn();
const mockUpdateIncidentType = vi.fn();

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: "incident-type-1" }),
  };
});

vi.mock("../../../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(() => ({
    hasPermission: () => true,
    user: null,
    isAuthenticated: true,
  })),
}));

vi.mock("../../../../../hooks/user", () => ({
  useUsers: vi.fn(() => ({
    data: [],
    isLoading: false,
  })),
  useDirectionValidators: vi.fn(() => ({
    data: [],
    isLoading: false,
  })),
}));

vi.mock("../../../../../hooks/settings", () => ({
  useIncidentType: vi.fn(() => ({
    data: {
      id: "incident-type-1",
      name: "TECHNICAL_ERROR",
      displayName: "Technical Error",
      description: "Technical issue description",
      isActive: true,
      slaHours: 4,
      defaultTargetService: { id: "service-1", name: "IT Support" },
      defaultTargetUser: {
        id: "user-1",
        firstName: "Default",
        lastName: "Assignee",
        username: "default.assignee",
      },
      requiresValidation: true,
      validatorScope: "TARGET_SERVICE_MANAGER",
      requiresDirectionValidation: true,
      directionValidators: [
        {
          id: "validator-1",
          firstName: "Direction",
          lastName: "Validator",
          username: "direction.validator",
        },
      ],
      requiresCauseAnalysis: true,
      emailNotificationsEnabled: false,
      closerType: "ASSIGNEE",
      resolverType: "SOURCE_AGENCY_MANAGER",
      treaterType: "ASSIGNEE",
      createdAt: "2024-01-15T10:00:00Z",
      updatedAt: "2024-01-16T12:00:00Z",
      modifiedBy: "admin",
    },
    isLoading: false,
  })),
  useDeleteIncidentType: vi.fn(() => ({
    mutate: mockDeleteIncidentType,
  })),
  useUpdateIncidentType: vi.fn(() => ({
    mutate: mockUpdateIncidentType,
    isPending: false,
  })),
  useDepartments: vi.fn(() => ({
    data: [{ id: "service-1", name: "IT Support" }],
    isLoading: false,
  })),
}));

describe("IncidentTypeDetailsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render incident type details", () => {
    renderWithProviders(<IncidentTypeDetailsPage />);

    expect(screen.getAllByText("Technical Error")[0]).toBeInTheDocument();
    expect(screen.getByText("TECHNICAL_ERROR")).toBeInTheDocument();
    expect(screen.getByText("Technical issue description")).toBeInTheDocument();
    expect(screen.getByText("IT Support")).toBeInTheDocument();
  });

  it("should navigate to the canonical edit page when edit is clicked", () => {
    renderWithProviders(<IncidentTypeDetailsPage />);

    fireEvent.click(screen.getByText("settings.buttons.edit"));

    expect(mockNavigate).toHaveBeenCalledWith(
      "/dashboard/settings/incident-types/edit/incident-type-1",
    );
  });

  it("should call delete mutation after confirmation", () => {
    renderWithProviders(<IncidentTypeDetailsPage />);

    fireEvent.click(screen.getByText("settings.buttons.delete"));

    const confirmButton = screen.getByRole("button", { name: "Confirm" });
    fireEvent.click(confirmButton);

    expect(mockDeleteIncidentType).toHaveBeenCalledWith(
      "incident-type-1",
      expect.any(Object),
    );
  });

  it("should navigate back when back button is clicked", () => {
    renderWithProviders(<IncidentTypeDetailsPage />);

    fireEvent.click(screen.getByLabelText("Return"));

    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  it("should call update mutation when deactivate button is clicked", () => {
    renderWithProviders(<IncidentTypeDetailsPage />);

    fireEvent.click(screen.getByText("common.deactivate"));

    expect(mockUpdateIncidentType).toHaveBeenCalledWith({
      id: "incident-type-1",
      data: expect.objectContaining({
        isActive: false,
      }),
    });
  });
});
