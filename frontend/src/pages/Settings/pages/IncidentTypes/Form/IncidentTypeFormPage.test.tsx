// Tests frontend : verifie le comportement de incident type formulaire page.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import IncidentTypeFormPage from "./IncidentTypeFormPage";
import { useIncidentType } from "../../../../../hooks/settings";
import {
  useAssignableUsers,
  useDirectionValidators,
} from "../../../../../hooks/user";
import type { IncidentTypeConfigResponse } from "../../../../../api/settings/types";

const mockNavigate = vi.fn();
const mockUseParams = vi.fn<() => { id?: string }>();
const mockUpdateIncidentType = vi.fn();

const completeIncidentType: IncidentTypeConfigResponse = {
  id: "incident-type-1",
  name: "TECHNICAL_ERROR",
  displayName: "Technical Error",
  description: "Complete configuration",
  slaHours: 48,
  defaultCriticality: "CRITICAL",
  defaultTargetService: { id: "service-1", name: "IT Support" },
  defaultTargetUser: {
    id: "user-1",
    username: "assignee",
    email: "assignee@fintrack.com",
    firstName: "Default",
    lastName: "Assignee",
  },
  requiresValidation: true,
  validatorScope: "TARGET_SERVICE_MANAGER",
  requiresDirectionValidation: true,
  directionValidators: [
    {
      id: "validator-1",
      username: "validator",
      email: "validator@fintrack.com",
      firstName: "Direction",
      lastName: "Validator",
    },
  ],
  requiresCauseAnalysis: true,
  emailNotificationsEnabled: true,
  closerRoles: ["CREATOR", "ASSIGNEE"],
  treaterRoles: ["CHEF_SERVICE"],
  resolverRoles: ["CREATOR"],
  reopenerRoles: ["SOURCE_AGENCY_MANAGER"],
  isActive: false,
  createdAt: "2026-07-20",
  updatedAt: "2026-07-28",
};

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => mockUseParams(),
  };
});

vi.mock("../../../../../hooks/settings", () => ({
  useCreateIncidentType: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useUpdateIncidentType: vi.fn(() => ({
    mutate: mockUpdateIncidentType,
    isPending: false,
  })),
  useIncidentType: vi.fn(() => ({ data: undefined, isLoading: false })),
  useDepartments: vi.fn(() => ({ data: [], isLoading: false })),
}));

vi.mock("../../../../../hooks/user", () => ({
  useUsers: vi.fn(() => ({ data: [], isLoading: false })),
  useAssignableUsers: vi.fn(() => ({ data: [], isLoading: false })),
  useDirectionValidators: vi.fn(() => ({ data: [], isLoading: false })),
}));

describe("IncidentTypeFormPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseParams.mockReturnValue({});
  });

  it("should render create title when route has no id", () => {
    renderWithProviders(<IncidentTypeFormPage />);
    expect(
      screen.getByText("settings.incidentTypes.page.createTitle"),
    ).toBeInTheDocument();
  });

  it("should render edit title when route has id", () => {
    mockUseParams.mockReturnValue({ id: "incident-type-1" });
    renderWithProviders(<IncidentTypeFormPage />);
    expect(
      screen.getByText("settings.incidentTypes.page.editTitle"),
    ).toBeInTheDocument();
  });

  it("should navigate back when cancel button is clicked", () => {
    renderWithProviders(<IncidentTypeFormPage />);
    const cancel = screen
      .getByText("settings.buttons.cancel")
      .closest("button");
    expect(cancel).not.toBeNull();
    fireEvent.click(cancel!);
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  it("should render direction validator field when requiresDirectionValidation is enabled", async () => {
    mockUseParams.mockReturnValue({ id: "incident-type-1" });
    vi.mocked(useIncidentType).mockReturnValue({
      data: {
        id: "incident-type-1",
        name: "test-type",
        displayName: "Test Type",
        description: "Desc",
        slaHours: 24,
        requiresValidation: false,
        requiresDirectionValidation: true,
        directionValidators: [{ id: "val-1", username: "val1", email: "val1@fintrack.com", firstName: "Val", lastName: "One" }],
        isActive: true,
        closerRoles: ["ASSIGNEE"],
        validatorScope: "AGENCY_MANAGER",
        requiresCauseAnalysis: false,
        emailNotificationsEnabled: false,
        createdAt: "2026-07-26",
        updatedAt: "2026-07-26",
      },
      isLoading: false,
    } as unknown as ReturnType<typeof useIncidentType>);

    vi.mocked(useDirectionValidators).mockReturnValue({
      data: [
        { id: "val-1", username: "val1", email: "val1@fintrack.com", firstName: "Val", lastName: "One" },
      ],
      isLoading: false,
    } as unknown as ReturnType<typeof useDirectionValidators>);

    renderWithProviders(<IncidentTypeFormPage />);

    await waitFor(() => {
      expect(
        screen.getByText("settings.incidentTypes.form.labels.directionValidator"),
      ).toBeInTheDocument();
    });
  });

  it("should preserve every API field when a complete incident type is edited", async () => {
    mockUseParams.mockReturnValue({ id: "incident-type-1" });
    vi.mocked(useIncidentType).mockReturnValue({
      data: completeIncidentType,
      isLoading: false,
    } as unknown as ReturnType<typeof useIncidentType>);
    vi.mocked(useAssignableUsers).mockReturnValue({
      data: [
        {
          id: "user-1",
          username: "assignee",
          email: "assignee@fintrack.com",
          firstName: "Default",
          lastName: "Assignee",
        },
      ],
      isLoading: false,
      isSuccess: true,
    } as unknown as ReturnType<typeof useAssignableUsers>);
    vi.mocked(useDirectionValidators).mockReturnValue({
      data: [
        {
          id: "validator-1",
          username: "validator",
          email: "validator@fintrack.com",
          firstName: "Direction",
          lastName: "Validator",
        },
      ],
      isLoading: false,
    } as unknown as ReturnType<typeof useDirectionValidators>);

    renderWithProviders(<IncidentTypeFormPage />);

    await waitFor(() =>
      expect(screen.getByDisplayValue("TECHNICAL_ERROR")).toBeInTheDocument(),
    );
    fireEvent.submit(document.querySelector("form")!);

    await waitFor(() =>
      expect(mockUpdateIncidentType).toHaveBeenCalledWith(
        {
          id: "incident-type-1",
          data: {
            name: "TECHNICAL_ERROR",
            displayName: "Technical Error",
            description: "Complete configuration",
            slaHours: 48,
            defaultCriticality: "CRITICAL",
            defaultTargetServiceId: "service-1",
            defaultTargetUserId: "user-1",
            requiresValidation: true,
            validatorScope: "TARGET_SERVICE_MANAGER",
            requiresDirectionValidation: true,
            directionValidatorIds: ["validator-1"],
            requiresCauseAnalysis: true,
            emailNotificationsEnabled: true,
            closerRoles: ["CREATOR", "ASSIGNEE"],
            treaterRoles: ["CHEF_SERVICE"],
            resolverRoles: ["CREATOR"],
            reopenerRoles: ["SOURCE_AGENCY_MANAGER"],
            isActive: false,
          },
        },
        expect.objectContaining({ onSuccess: expect.any(Function) }),
      ),
    );
  });

  it("should keep the configured target user while eligible users are loading", async () => {
    mockUseParams.mockReturnValue({ id: "incident-type-1" });
    vi.mocked(useIncidentType).mockReturnValue({
      data: completeIncidentType,
      isLoading: false,
    } as unknown as ReturnType<typeof useIncidentType>);
    vi.mocked(useAssignableUsers).mockReturnValue({
      data: [],
      isLoading: true,
      isSuccess: false,
    } as unknown as ReturnType<typeof useAssignableUsers>);
    vi.mocked(useDirectionValidators).mockReturnValue({
      data: completeIncidentType.directionValidators,
      isLoading: false,
    } as unknown as ReturnType<typeof useDirectionValidators>);

    renderWithProviders(<IncidentTypeFormPage />);

    await waitFor(() =>
      expect(screen.getByDisplayValue("TECHNICAL_ERROR")).toBeInTheDocument(),
    );
    fireEvent.submit(document.querySelector("form")!);

    await waitFor(() =>
      expect(mockUpdateIncidentType).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            defaultTargetUserId: "user-1",
          }),
        }),
        expect.any(Object),
      ),
    );
  });
});
