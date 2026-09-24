// Tests frontend : verifie le comportement de password management.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { App } from "antd";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import PasswordManagement from "./PasswordManagement";
import { useRegeneratePassword } from "../../../../hooks/user/useUsers";
import { useReauth } from "../../../../hooks/auth/useAuth";
import { makeUser } from "../../../../mocks";

vi.mock("../../../../hooks/user/useUsers", () => ({
  useRegeneratePassword: vi.fn(),
}));

vi.mock("../../../../hooks/auth/useAuth", () => ({
  useReauth: vi.fn(),
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

const mockConfirm = vi.fn();
const mockMessageSuccess = vi.fn();
const mockMessageError = vi.fn();

// Fabrique une fixture de test pour password management.test.
const testUser = makeUser({ id: "user-1", username: "jdoe", isActive: true });
// Fabrique une fixture de test pour password management.test.
const inactiveUser = makeUser({ id: "user-1", isActive: false });
// Fabrique une fixture de test pour password management.test.
const currentUser = { username: "admin" };

describe("PasswordManagement", () => {
  const mockResetPassword = vi.fn();
  const mockReauth = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    vi.spyOn(App, "useApp").mockReturnValue({
      modal: { confirm: mockConfirm } as Partial<
        ReturnType<typeof App.useApp>["modal"]
      > as ReturnType<typeof App.useApp>["modal"],
      message: {
        success: mockMessageSuccess,
        error: mockMessageError,
      } as Partial<ReturnType<typeof App.useApp>["message"]> as ReturnType<
        typeof App.useApp
      >["message"],
      notification: {} as ReturnType<typeof App.useApp>["notification"],
    });

    vi.mocked(useRegeneratePassword).mockReturnValue({
      mutate: mockResetPassword,
      isPending: false,
    } as unknown as ReturnType<typeof useRegeneratePassword>);

    vi.mocked(useReauth).mockReturnValue({
      mutate: mockReauth,
      isPending: false,
    } as unknown as ReturnType<typeof useReauth>);
  });

  it("should render the reset password button when the user is active", () => {
    renderWithProviders(
      <PasswordManagement user={testUser} currentUser={currentUser} />,
    );
    expect(
      screen.getByRole("button", { name: "users.form.buttons.reset_password" }),
    ).toBeEnabled();
  });

  it("should disable the reset password button and show a warning when the user is inactive", () => {
    renderWithProviders(
      <PasswordManagement user={inactiveUser} currentUser={currentUser} />,
    );
    expect(
      screen.getByRole("button", { name: "users.form.buttons.reset_password" }),
    ).toBeDisabled();
    expect(
      screen.getByText("users.form.messages.user_inactive"),
    ).toBeInTheDocument();
  });

  it("should open the reauth modal when the reset password button is clicked", async () => {
    renderWithProviders(
      <PasswordManagement user={testUser} currentUser={currentUser} />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: "users.form.buttons.reset_password" }),
    );
    expect(screen.getByText("auth.reauth_title")).toBeInTheDocument();
    expect(screen.getByRole("dialog")).toBeInTheDocument();
  });

  it("should close the reauth modal when the cancel button is clicked", async () => {
    renderWithProviders(
      <PasswordManagement user={testUser} currentUser={currentUser} />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: "users.form.buttons.reset_password" }),
    );
    expect(screen.getByText("auth.reauth_title")).toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", { name: "common.cancel" }),
    );

    await waitFor(() => {
      const dialog = screen.getByRole("dialog", { hidden: true });
      expect(dialog.className).toMatch("leave");
    });
  });

  it("should call reauth with the entered credentials when the confirm button is clicked", async () => {
    renderWithProviders(
      <PasswordManagement user={testUser} currentUser={currentUser} />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: "users.form.buttons.reset_password" }),
    );

    await userEvent.type(
      screen.getByLabelText("auth.labels.password"),
      "password123",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "common.confirm" }),
    );

    await waitFor(() => {
      expect(mockReauth).toHaveBeenCalledWith(
        { username: "admin", password: "password123" },
        expect.any(Object),
      );
    });
  });

  it("should show a success message and open the confirm modal when reauthentication succeeds", async () => {
    mockReauth.mockImplementation(
      (_: unknown, options: { onSuccess: () => void }) => {
        options.onSuccess();
      },
    );

    renderWithProviders(
      <PasswordManagement user={testUser} currentUser={currentUser} />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: "users.form.buttons.reset_password" }),
    );
    await userEvent.type(
      screen.getByLabelText("auth.labels.password"),
      "password123",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "common.confirm" }),
    );

    await waitFor(() => {
      expect(mockMessageSuccess).toHaveBeenCalledWith("auth.reauth_success");
      expect(mockConfirm).toHaveBeenCalled();
    });

    const confirmCall = mockConfirm.mock.calls[0][0] as { onOk: () => void };
    confirmCall.onOk();

    expect(mockResetPassword).toHaveBeenCalledWith("user-1");
  });

  it("should show an error message when reauthentication fails", async () => {
    mockReauth.mockImplementation(
      (_: unknown, options: { onError: () => void }) => {
        options.onError();
      },
    );

    renderWithProviders(
      <PasswordManagement user={testUser} currentUser={currentUser} />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: "users.form.buttons.reset_password" }),
    );
    await userEvent.type(
      screen.getByLabelText("auth.labels.password"),
      "password123",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "common.confirm" }),
    );

    await waitFor(() => {
      expect(mockMessageError).toHaveBeenCalledWith("auth.reauth_failed");
    });
  });
});
