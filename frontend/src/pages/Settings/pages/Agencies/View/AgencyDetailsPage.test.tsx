// Tests frontend : verifie le comportement de agence details page.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import AgencyDetailsPage from "./AgencyDetailsPage";

const mockNavigate = vi.fn();
const mockDeleteAgency = vi.fn();
const mockUpdateAgency = vi.fn();

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, fallback?: unknown) => {
      if (
        typeof fallback === "object" &&
        fallback !== null &&
        "code" in fallback
      )
        return key;
      return key;
    },
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: "agency-1" }),
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
  useAgency: vi.fn(() => ({
    data: {
      id: "agency-1",
      name: "Main Agency",
      code: "AG-MAIN",
      city: "Yaoundé",
      address: "123 Test St",
      isActive: true,
      members: [],
      headOfAgency: null,
    },
    isLoading: false,
  })),
  useDeleteAgency: vi.fn(() => ({ mutate: mockDeleteAgency })),
  useUpdateAgency: vi.fn(() => ({
    mutate: mockUpdateAgency,
    isPending: false,
  })),
}));

describe("AgencyDetailsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render agency details", () => {
    renderWithProviders(<AgencyDetailsPage />);
    expect(screen.getAllByText("Main Agency")[0]).toBeInTheDocument();
    expect(screen.getByText("AG-MAIN")).toBeInTheDocument();
    expect(screen.getByText("123 Test St")).toBeInTheDocument();
  });

  it("should navigate to the canonical edit page when edit is clicked", () => {
    renderWithProviders(<AgencyDetailsPage />);
    const edit = screen.getByText("settings.buttons.edit").closest("button");
    expect(edit).not.toBeNull();
    fireEvent.click(edit!);
    expect(mockNavigate).toHaveBeenCalledWith(
      "/dashboard/settings/agencies/edit/agency-1",
    );
  });

  it("should call delete mutation after confirmation", () => {
    renderWithProviders(<AgencyDetailsPage />);
    const deleteBtn = screen
      .getByText("settings.buttons.delete")
      .closest("button");
    expect(deleteBtn).not.toBeNull();
    fireEvent.click(deleteBtn!);
    fireEvent.click(screen.getByText("Confirm"));
    expect(mockDeleteAgency).toHaveBeenCalledWith(
      "agency-1",
      expect.any(Object),
    );
  });

  it("should navigate back when back button is clicked", () => {
    renderWithProviders(<AgencyDetailsPage />);
    const backBtn = screen.getByRole("button", { name: "Return" });
    fireEvent.click(backBtn);
    expect(mockNavigate).toHaveBeenCalled();
  });

  it("should call update mutation when deactivate button is clicked", () => {
    renderWithProviders(<AgencyDetailsPage />);
    const toggleButton = screen
      .getByText("common.deactivate")
      .closest("button")!;
    fireEvent.click(toggleButton);
    expect(mockUpdateAgency).toHaveBeenCalledWith({
      id: "agency-1",
      data: {
        name: "Main Agency",
        city: "Yaoundé",
        address: "123 Test St",
        headUserId: undefined,
        isActive: false,
      },
    });
  });
});
