// Tests frontend : verifie le comportement de connexion form.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import LoginForm from "./LoginForm";
import { useLogin } from "../../../../../hooks/auth/useAuth";
import type { UseMutationResult } from "@tanstack/react-query";
import type {
  AuthResponse,
  LoginCredentials,
} from "../../../../../api/user/types";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (
      key: string,
      opts?: { count?: number; max?: number; defaultValue?: string },
    ) => {
      if (key === "login.errors.failed_attempts" && opts) {
        return opts.defaultValue ?? key;
      }
      return key;
    },
  }),
  initReactI18next: {
    type: "3rdParty",
    init: () => {},
  },
}));

vi.mock("../../../../../hooks/auth/useAuth");

vi.mock("../../../../../utils/apiMessages/apiMessages", () => ({
  getApiErrorMessage: (error: unknown) => {
    const e = error as {
      response?: { status?: number; data?: { code?: string } };
    };
    if (
      e.response?.status === 403 &&
      e.response?.data?.code === "ACCOUNT_LOCKED"
    ) {
      return "Account locked message from API";
    }
    if (e.response?.status === 401) return "login.errors.unauthorized";
    return "login.errors.generic";
  },
  getApiErrorCode: (error: unknown) => {
    const e = error as { response?: { data?: { code?: string } } };
    return e.response?.data?.code || null;
  },
  getFailedLoginAttempts: (error: unknown) => {
    const e = error as {
      response?: { data?: { details?: { failed_login_attempts?: number } } };
    };
    return e.response?.data?.details?.failed_login_attempts ?? null;
  },
  getMaxFailedAttempts: (error: unknown) => {
    const e = error as {
      response?: { data?: { details?: { max_failed_attempts?: number } } };
    };
    return e.response?.data?.details?.max_failed_attempts ?? null;
  },
}));

// Type la portion de mutation de connexion utilisee par le test.
type LoginMutationResult = Pick<
  UseMutationResult<AuthResponse, Error, LoginCredentials>,
  "mutate" | "isPending" | "isError" | "error"
>;

// Definit les donnees de test test data.
const TEST_DATA = {
  USERNAME: "admin",
  PASSWORD: "password",
} as const;

describe("LoginForm", () => {
  const mockMutate = vi.fn();

  const setupLoginMock = (overrides: Partial<LoginMutationResult> = {}) => {
    vi.mocked(useLogin).mockReturnValue({
      mutate: mockMutate,
      isPending: false,
      isError: false,
      error: null,
      ...overrides,
    } as ReturnType<typeof useLogin>);
  };

  beforeEach(() => {
    vi.clearAllMocks();
    setupLoginMock();
  });

  it("should render all form elements when the component mounts", () => {
    renderWithProviders(<LoginForm />);

    expect(screen.getByText("login.title")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("login.username_placeholder"),
    ).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("login.password_placeholder"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "login.submit_button" }),
    ).toBeInTheDocument();
  });

  it("should call login mutation with entered credentials when form is submitted", async () => {
    renderWithProviders(<LoginForm />);

    fireEvent.change(
      screen.getByPlaceholderText("login.username_placeholder"),
      { target: { value: TEST_DATA.USERNAME } },
    );
    fireEvent.change(
      screen.getByPlaceholderText("login.password_placeholder"),
      { target: { value: TEST_DATA.PASSWORD } },
    );
    fireEvent.click(
      screen.getByRole("button", { name: "login.submit_button" }),
    );

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledWith({
        username: TEST_DATA.USERNAME,
        password: TEST_DATA.PASSWORD,
      });
    });
  });

  it("should trim username boundaries without altering internal spaces", async () => {
    renderWithProviders(<LoginForm />);

    fireEvent.change(
      screen.getByPlaceholderText("login.username_placeholder"),
      { target: { value: "  jean pierre  " } },
    );
    fireEvent.change(
      screen.getByPlaceholderText("login.password_placeholder"),
      { target: { value: TEST_DATA.PASSWORD } },
    );
    fireEvent.click(
      screen.getByRole("button", { name: "login.submit_button" }),
    );

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledWith({
        username: "jean pierre",
        password: TEST_DATA.PASSWORD,
      });
    });
  });

  it("should display submit button in loading state when isPending is true", () => {
    setupLoginMock({ isPending: true });
    renderWithProviders(<LoginForm />);

    // Le bouton personnalise remplace l'icone par <Spin> pendant le chargement sans modifier le libelle accessible.
    // Verifie aussi la desactivation pendant le chargement.
    const button = screen.getByRole("button", { name: "login.submitting" });
    expect(button).toBeDisabled();
  });

  it("should not display error alert when isError is false", () => {
    setupLoginMock({ isError: false, error: null });
    renderWithProviders(<LoginForm />);

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("should display error alert when login fails", () => {
    setupLoginMock({
      isError: true,
      error: { response: { status: 500 } } as Partial<Error> as Error,
    });
    renderWithProviders(<LoginForm />);

    expect(screen.getByRole("alert")).toBeInTheDocument();
  });

  it("should display unauthorized error message when status is 401", () => {
    setupLoginMock({
      isError: true,
      error: { response: { status: 401 } } as Partial<Error> as Error,
    });
    renderWithProviders(<LoginForm />);

    expect(screen.getByRole("alert")).toHaveTextContent(
      "login.errors.unauthorized",
    );
  });

  it("should display generic error message when server returns 500", () => {
    setupLoginMock({
      isError: true,
      error: { response: { status: 500 } } as Partial<Error> as Error,
    });
    renderWithProviders(<LoginForm />);

    expect(screen.getByRole("alert")).toHaveTextContent("login.errors.generic");
  });

  it("should display account locked message when error code is ACCOUNT_LOCKED", () => {
    setupLoginMock({
      isError: true,
      error: {
        response: {
          status: 403,
          data: { code: "ACCOUNT_LOCKED" },
        },
      } as Partial<Error> as Error,
    });
    renderWithProviders(<LoginForm />);

    expect(screen.getByRole("alert")).toHaveTextContent(
      "error.user.account_locked",
    );
  });

  it("should block submission and display validation errors when fields are empty", async () => {
    renderWithProviders(<LoginForm />);

    fireEvent.click(
      screen.getByRole("button", { name: "login.submit_button" }),
    );

    await waitFor(() => {
      expect(
        screen.getByPlaceholderText("login.username_placeholder"),
      ).toHaveAttribute("aria-invalid", "true");
      expect(
        screen.getByPlaceholderText("login.password_placeholder"),
      ).toHaveAttribute("aria-invalid", "true");
    });

    expect(mockMutate).not.toHaveBeenCalled();
  });

  it("should display failed attempts counter when error details include failed_login_attempts", () => {
    setupLoginMock({
      isError: true,
      error: {
        response: {
          status: 401,
          data: {
            code: "INVALID_CREDENTIALS",
            details: { failed_login_attempts: 3, max_failed_attempts: 10 },
          },
        },
      } as Partial<Error> as Error,
    });
    renderWithProviders(<LoginForm />);

    const alert = screen.getByRole("alert");
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent("3/10 failed attempts");
  });

  it("should not display failed attempts counter when error details are absent", () => {
    setupLoginMock({
      isError: true,
      error: {
        response: { status: 500 },
      } as Partial<Error> as Error,
    });
    renderWithProviders(<LoginForm />);

    const alert = screen.getByRole("alert");
    expect(alert).toBeInTheDocument();
    expect(alert).not.toHaveTextContent("failed attempts");
  });
});
