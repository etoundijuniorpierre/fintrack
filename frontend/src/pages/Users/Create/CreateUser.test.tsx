// Tests frontend : verifie le comportement de create user.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import { useNavigate } from "react-router-dom";
import CreateUser from "./CreateUser";
import { makeUser } from "../../../mocks";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import type { User } from "../../../api/user/types";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (k: string) => k }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: vi.fn(),
  };
});

vi.mock("../components", () => ({
  UserForm: ({
    onSuccess,
    onCancel,
  }: {
    onSuccess: (user: User) => void;
    onCancel: () => void;
  }) => (
    <div data-testid="user-form">
      <button onClick={() => onSuccess(makeUser({ id: "user-1" }))}>
        Submit
      </button>
      <button onClick={onCancel}>Cancel</button>
    </div>
  ),
}));

describe("CreateUser Page", () => {
  const mockNavigate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useNavigate).mockReturnValue(mockNavigate);
  });

  it("should render the page title and user form on initial load", () => {
    renderWithProviders(<CreateUser />);
    expect(
      screen.getAllByText("users.form.titles.create").length,
    ).toBeGreaterThanOrEqual(1);
    expect(screen.getByTestId("user-form")).toBeInTheDocument();
  });

  it("should render a back button on the page", () => {
    renderWithProviders(<CreateUser />);
    expect(screen.getByRole("button", { name: "Return" })).toBeInTheDocument();
  });

  it("should navigate to user detail when form submission succeeds", () => {
    renderWithProviders(<CreateUser />);
    screen.getByText("Submit").click();
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/users/user-1");
  });

  it("should navigate back to users list when cancel is clicked", () => {
    renderWithProviders(<CreateUser />);
    screen.getByText("Cancel").click();
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/users");
  });
});
