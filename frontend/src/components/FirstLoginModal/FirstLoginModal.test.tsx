// Tests frontend : verifie le comportement de first connexion modal.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import FirstLoginModal from "./FirstLoginModal";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { authApi } from "../../api/user/auth/authApi";
import { App } from "antd";

vi.mock("../../api/user/auth/authApi", () => ({
  authApi: {
    changePassword: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
  },
}));

const mockStoreLogin = vi.fn();

vi.mock("../../store/authStore/authStore", () => ({
  useAuthStore: () => ({
    user: { id: "user-1", username: "jdoe" },
    login: mockStoreLogin,
  }),
}));

vi.mock("../../hooks/auth/useAuth", () => ({
  useLogout: () => ({ mutate: mockLogout }),
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

vi.mock("../../utils/apiMessages/apiMessages", () => ({
  getApiErrorMessage: () => "Error message",
}));

const mockLogout = vi.fn();

// Couvre les comportements du module teste.
const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

// Prepare l'affichage lisible de first connexion modal.test.
const renderFirstLoginModal = (isOpen = true) => {
  const queryClient = createTestQueryClient();
  const onClose = vi.fn();

  return {
    onClose,
    ...render(
      <App>
        <QueryClientProvider client={queryClient}>
          <FirstLoginModal open={isOpen} onClose={onClose} />
        </QueryClientProvider>
      </App>,
    ),
  };
};

describe("FirstLoginModal", () => {
  const mockChangePassword = vi.mocked(authApi.changePassword);
  const mockLogin = vi.mocked(authApi.login);

  beforeEach(() => {
    vi.clearAllMocks();
    mockLogout.mockReset();
    mockLogin.mockResolvedValue({
      token: "full-access-token",
      id: "user-1",
      username: "jdoe",
      roles: ["AGENT"],
      permissions: ["INCIDENT_VIEW_OWN"],
      isActive: true,
      isFirstLogin: false,
    });
  });

  it("renders when open is true", () => {
    renderFirstLoginModal(true);
    expect(screen.getByText("first_login.title")).toBeInTheDocument();
    expect(screen.getByText("first_login.welcome_message")).toBeInTheDocument();
  });

  it("does not render when open is false", () => {
    renderFirstLoginModal(false);
    expect(screen.queryByText("first_login.title")).not.toBeInTheDocument();
  });

  it("renders password input fields", () => {
    renderFirstLoginModal(true);
    expect(
      screen.getByLabelText("first_login.new_password_label"),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("first_login.confirm_password_label"),
    ).toBeInTheDocument();
  });

  it("validates required fields", async () => {
    renderFirstLoginModal(true);
    fireEvent.click(
      screen.getByRole("button", {
        name: "first_login.change_password_button",
      }),
    );

    await waitFor(() => {
      expect(
        screen.getByText("first_login.password_required"),
      ).toBeInTheDocument();
    });
  });

  it("validates minimum password length", async () => {
    renderFirstLoginModal(true);
    fireEvent.change(screen.getByLabelText("first_login.new_password_label"), {
      target: { value: "weak" },
    });
    fireEvent.change(
      screen.getByLabelText("first_login.confirm_password_label"),
      {
        target: { value: "weak" },
      },
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: "first_login.change_password_button",
      }),
    );

    await waitFor(() => {
      expect(
        screen.getByText("first_login.password_min_length"),
      ).toBeInTheDocument();
    });
  });

  it("validates password confirmation match", async () => {
    renderFirstLoginModal(true);
    fireEvent.change(screen.getByLabelText("first_login.new_password_label"), {
      target: { value: "ComplexPass123!" },
    });
    fireEvent.change(
      screen.getByLabelText("first_login.confirm_password_label"),
      {
        target: { value: "DifferentPass123!" },
      },
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: "first_login.change_password_button",
      }),
    );

    await waitFor(() => {
      expect(
        screen.getByText("first_login.passwords_not_match"),
      ).toBeInTheDocument();
    });
  });

  it("submits without currentPassword and calls onClose on success", async () => {
    mockChangePassword.mockResolvedValue(undefined);
    const { onClose } = renderFirstLoginModal(true);

    fireEvent.change(screen.getByLabelText("first_login.new_password_label"), {
      target: { value: "ComplexPass123!" },
    });
    fireEvent.change(
      screen.getByLabelText("first_login.confirm_password_label"),
      {
        target: { value: "ComplexPass123!" },
      },
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: "first_login.change_password_button",
      }),
    );

    await waitFor(() => {
      expect(mockChangePassword).toHaveBeenCalledWith("user-1", {
        currentPassword: "",
        newPassword: "ComplexPass123!",
      });
      expect(mockLogin).toHaveBeenCalledWith({
        username: "jdoe",
        password: "ComplexPass123!",
      });
      expect(mockStoreLogin).toHaveBeenCalledWith(
        "full-access-token",
        expect.objectContaining({
          id: "user-1",
          isFirstLogin: false,
          isActive: true,
        }),
      );
    });

    await waitFor(() => {
      expect(onClose).toHaveBeenCalled();
    });
  });

  it("does not call onClose on API error", async () => {
    mockChangePassword.mockRejectedValue(new Error("API Error"));
    const { onClose } = renderFirstLoginModal(true);

    fireEvent.change(screen.getByLabelText("first_login.new_password_label"), {
      target: { value: "ComplexPass123!" },
    });
    fireEvent.change(
      screen.getByLabelText("first_login.confirm_password_label"),
      {
        target: { value: "ComplexPass123!" },
      },
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: "first_login.change_password_button",
      }),
    );

    await waitFor(() => {
      expect(mockChangePassword).toHaveBeenCalled();
    });

    expect(onClose).not.toHaveBeenCalled();
  });

  it("cancel button calls onClose and logout", () => {
    const { onClose } = renderFirstLoginModal(true);

    fireEvent.click(
      screen.getByRole("button", { name: "first_login.cancel_and_logout" }),
    );

    expect(onClose).toHaveBeenCalled();
    expect(mockLogout).toHaveBeenCalled();
  });

  it("X button calls onClose and logout", () => {
    const { onClose } = renderFirstLoginModal(true);

    const closeBtn = screen.getByRole("button", { name: "Close" });
    fireEvent.click(closeBtn);

    expect(onClose).toHaveBeenCalled();
    expect(mockLogout).toHaveBeenCalled();
  });
});
