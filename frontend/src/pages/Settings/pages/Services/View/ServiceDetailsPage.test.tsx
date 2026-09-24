// Tests frontend : verifie le comportement de service details page.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import ServiceDetailsPage from "./ServiceDetailsPage";

const mockNavigate = vi.fn();
const mockDeleteService = vi.fn();
const mockUpdateService = vi.fn();

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: "service-1" }),
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
}));

vi.mock("../../../../../hooks/settings", () => ({
  useDepartment: vi.fn(() => ({
    data: {
      id: "service-1",
      name: "IT Department",
      description: "IT support and development",
      isActive: true,
      members: [],
      headOfService: null,
    },
    isLoading: false,
  })),
  useDeleteDepartment: vi.fn(() => ({ mutate: mockDeleteService })),
  useUpdateDepartment: vi.fn(() => ({
    mutate: mockUpdateService,
    isPending: false,
  })),
}));

describe("ServiceDetailsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render service details", () => {
    renderWithProviders(<ServiceDetailsPage />);
    expect(screen.getAllByText("IT Department")[0]).toBeInTheDocument();
    expect(screen.getByText("IT support and development")).toBeInTheDocument();
  });

  it("should navigate to the canonical edit page when edit is clicked", () => {
    renderWithProviders(<ServiceDetailsPage />);
    const edit = screen.getByText("settings.buttons.edit").closest("button");
    expect(edit).not.toBeNull();
    fireEvent.click(edit!);
    expect(mockNavigate).toHaveBeenCalledWith(
      "/dashboard/settings/services/edit/service-1",
    );
  });

  it("should call delete mutation after confirmation", () => {
    renderWithProviders(<ServiceDetailsPage />);
    const deleteBtn = screen
      .getByText("settings.buttons.delete")
      .closest("button");
    expect(deleteBtn).not.toBeNull();
    fireEvent.click(deleteBtn!);
    fireEvent.click(screen.getByText("Confirm"));
    expect(mockDeleteService).toHaveBeenCalledWith(
      "service-1",
      expect.any(Object),
    );
  });

  it("should navigate back when back button is clicked", () => {
    renderWithProviders(<ServiceDetailsPage />);
    const backBtn = screen.getByRole("button", { name: "Return" });
    fireEvent.click(backBtn);
    expect(mockNavigate).toHaveBeenCalled();
  });

  it("should call update mutation when deactivate button is clicked", () => {
    renderWithProviders(<ServiceDetailsPage />);
    const toggleButton = screen
      .getByText("common.deactivate")
      .closest("button")!;
    fireEvent.click(toggleButton);
    expect(mockUpdateService).toHaveBeenCalledWith({
      id: "service-1",
      data: {
        name: "IT Department",
        description: "IT support and development",
        headUserId: undefined,
        isActive: false,
      },
    });
  });
});
