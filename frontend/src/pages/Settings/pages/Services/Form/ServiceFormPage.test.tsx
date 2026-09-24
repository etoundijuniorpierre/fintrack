// Tests frontend : verifie le comportement de service formulaire page.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import ServiceFormPage from "./ServiceFormPage";
import type { ServiceResponse } from "../../../../../api/settings/types";

const mockNavigate = vi.fn();
const mockCreateService = vi.fn();
const mockUpdateService = vi.fn();
const mockUseParams = vi.fn<() => { id?: string }>();
let serviceData: ServiceResponse | undefined;

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
  useCreateDepartment: vi.fn(() => ({
    mutate: mockCreateService,
    isPending: false,
  })),
  useUpdateDepartment: vi.fn(() => ({
    mutate: mockUpdateService,
    isPending: false,
  })),
  useDepartment: vi.fn(() => ({ data: serviceData, isLoading: false })),
}));

vi.mock("../../../../../hooks/user", () => ({
  useUsers: vi.fn(() => ({
    data: [
      {
        id: "u1",
        firstName: "John",
        lastName: "Doe",
        username: "jdoe",
        roles: [{ name: "CHEF_SERVICE" }],
      },
      {
        id: "u2",
        firstName: "Jane",
        lastName: "Smith",
        username: "jsmith",
        roles: [{ name: "AGENT" }],
      },
    ],
    isLoading: false,
  })),
}));

describe("ServiceFormPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseParams.mockReturnValue({});
    serviceData = undefined;
  });

  it("should render create title when route has no id", () => {
    renderWithProviders(<ServiceFormPage />);
    expect(
      screen.getByText("settings.services.page.createTitle"),
    ).toBeInTheDocument();
  });

  it("should render edit title when route has id", () => {
    mockUseParams.mockReturnValue({ id: "service-1" });
    serviceData = {
      id: "service-1",
      name: "IT",
      isActive: true,
      members: [],
      createdAt: "",
      updatedAt: "",
    };

    renderWithProviders(<ServiceFormPage />);
    expect(
      screen.getByText("settings.services.page.editTitle"),
    ).toBeInTheDocument();
  });

  it("should navigate back when cancel button is clicked", () => {
    renderWithProviders(<ServiceFormPage />);
    const cancel = screen
      .getByText("settings.buttons.cancel")
      .closest("button");
    expect(cancel).not.toBeNull();
    fireEvent.click(cancel!);
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  it("should render headOfService selection field", () => {
    renderWithProviders(<ServiceFormPage />);
    expect(
      screen.getByText("settings.services.form.labels.headOfService"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("settings.services.form.placeholders.headOfService"),
    ).toBeInTheDocument();
  });
});
