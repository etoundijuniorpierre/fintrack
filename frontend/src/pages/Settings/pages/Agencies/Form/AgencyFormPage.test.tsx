// Tests frontend : verifie le comportement de agence formulaire page.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import AgencyFormPage from "./AgencyFormPage";
import type { AgencyResponse } from "../../../../../api/settings/types";

const mockNavigate = vi.fn();
const mockCreateAgency = vi.fn();
const mockUpdateAgency = vi.fn();
const mockUseParams = vi.fn<() => { id?: string }>();
let agencyData: AgencyResponse | undefined;

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
  useCreateAgency: vi.fn(() => ({
    mutate: mockCreateAgency,
    isPending: false,
  })),
  useUpdateAgency: vi.fn(() => ({
    mutate: mockUpdateAgency,
    isPending: false,
  })),
  useAgency: vi.fn(() => ({ data: agencyData, isLoading: false })),
}));

vi.mock("../../../../../hooks/user", () => ({
  useUsers: vi.fn(() => ({
    data: [
      {
        id: "u1",
        firstName: "John",
        lastName: "Doe",
        username: "jdoe",
        roles: [{ name: "CHEF_AGENCE" }],
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

describe("AgencyFormPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseParams.mockReturnValue({});
    agencyData = undefined;
  });

  it("should render create title when route has no id", () => {
    renderWithProviders(<AgencyFormPage />);
    expect(
      screen.getByText("settings.agencies.page.createTitle"),
    ).toBeInTheDocument();
  });

  it("should render edit title when route has id", () => {
    mockUseParams.mockReturnValue({ id: "agency-1" });
    agencyData = {
      id: "agency-1",
      code: "AG-1",
      name: "Agency",
      isActive: true,
      members: [],
      createdAt: "",
      updatedAt: "",
    };

    renderWithProviders(<AgencyFormPage />);
    expect(
      screen.getByText("settings.agencies.page.editTitle"),
    ).toBeInTheDocument();
  });

  it("should navigate back when cancel button is clicked", () => {
    renderWithProviders(<AgencyFormPage />);
    const cancel = screen
      .getByText("settings.buttons.cancel")
      .closest("button");
    expect(cancel).not.toBeNull();
    fireEvent.click(cancel!);
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  it("should call create mutation when save is clicked with valid values", async () => {
    renderWithProviders(<AgencyFormPage />);

    fireEvent.change(
      screen.getByPlaceholderText("settings.agencies.form.placeholders.city"),
      {
        target: { value: "Yaoundé" },
      },
    );
    fireEvent.change(
      screen.getByPlaceholderText("settings.agencies.form.placeholders.name"),
      {
        target: { value: "Agency One" },
      },
    );
    const save = screen.getByText("settings.buttons.save").closest("button");
    expect(save).not.toBeNull();
    fireEvent.click(save!);

    await waitFor(() => {
      expect(mockCreateAgency).toHaveBeenCalled();
    });
  });

  it("should render headOfAgency selection field", () => {
    renderWithProviders(<AgencyFormPage />);
    expect(
      screen.getByText("settings.agencies.form.labels.headOfAgency"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("settings.agencies.form.placeholders.headOfAgency"),
    ).toBeInTheDocument();
  });
});
