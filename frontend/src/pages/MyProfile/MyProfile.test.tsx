// Tests frontend : verifie le comportement de mon profile.p13.test.

import React from "react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent, waitFor, act } from "@testing-library/react";
import MyProfile from "./MyProfile";
import { makeUser, makePermission } from "../../mocks";
import { renderWithProviders } from "../../test-utils/renderWithProviders";
import { useMyProfile, useUpdateProfile } from "../../hooks/user/useMyProfile";
import { authApi } from "../../api/user/auth/authApi";
import { documentApi } from "../../api/document/documentApi";
import type { UseQueryResult } from "@tanstack/react-query";

// Fabrique une fixture de test pour mon profile.p13.test.
const makeQueryResult = <T,>(
  overrides: Partial<UseQueryResult<T, Error>>,
): UseQueryResult<T, Error> => overrides as UseQueryResult<T, Error>;

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

const mockUpdateAuthSession = vi.fn();
const mockLogout = vi.fn();

vi.mock("../../store/authStore/authStore", () => ({
  useAuthStore: () => ({
    user: {
      id: "user-1",
      username: "jdoe",
      roles: [],
      permissions: ["USER_MANAGE_PROFILE"],
    },
    hasPermission: (p: string) => p === "USER_MANAGE_PROFILE",
    login: mockUpdateAuthSession,
    logout: mockLogout,
  }),
}));

const mockMutate = vi.fn();

vi.mock("../../hooks/user/useMyProfile", () => ({
  useMyProfile: vi.fn(),
  useUpdateProfile: vi.fn(),
}));

vi.mock("../../api/user/auth/authApi", () => ({
  authApi: {
    changePassword: vi.fn().mockResolvedValue(undefined),
    refreshToken: vi.fn(),
  },
}));

const mockMessage = { success: vi.fn(), error: vi.fn() };

vi.mock("antd", async () => {
  const actual = await vi.importActual<typeof import("antd")>("antd");
  const MockApp = Object.assign(
    ({ children }: { children: React.ReactNode }) =>
      React.createElement(actual.App, {}, children),
    { useApp: () => ({ message: mockMessage }) },
  );
  return { ...actual, App: MockApp };
});

vi.mock("./components/RolesSection/RolesSection", () => ({
  default: ({ roles }: { roles: unknown[] }) => (
    <div data-testid="roles-section" data-count={roles.length} />
  ),
}));

const testProfile = makeUser({
  id: "user-1",
  username: "jdoe",
  email: "john.doe@finstar-cm.com",
  phoneNumber: 771234567,
  isActive: true,
  roles: [],
  permissions: [makePermission({ id: "perm-1", name: "USER_MANAGE_PROFILE" })],
});

// Prepare l'affichage lisible de mon profile.p13.test.
const renderPage = () => renderWithProviders(<MyProfile />);

// Recupere les champs de formulaire controles par le test.
const getFormInputs = () =>
  document.querySelectorAll<HTMLInputElement>("form input, form textarea");

describe("P-13.A — form fields are read-only when isEditing === false", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authApi.refreshToken).mockResolvedValue({
      token: "renewed-token",
      id: "user-1",
      username: "jdoe",
      roles: [],
      permissions: ["USER_MANAGE_PROFILE"],
      isActive: true,
      isFirstLogin: false,
    });
    vi.mocked(useMyProfile).mockReturnValue(
      makeQueryResult({
        data: testProfile,
        isLoading: false,
        isError: false,
        refetch: vi.fn(),
      }),
    );
    vi.mocked(useUpdateProfile).mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    } as Partial<ReturnType<typeof useUpdateProfile>> as ReturnType<
      typeof useUpdateProfile
    >);
  });

  it("should disable all form inputs when isEditing is false by default", () => {
    renderPage();

    const inputs = getFormInputs();
    expect(inputs.length).toBeGreaterThanOrEqual(3);

    inputs.forEach((input) => {
      expect(input).toBeDisabled();
    });
  });

  it("rejects an empty avatar before upload", async () => {
    const upload = vi.spyOn(documentApi, "uploadAvatar");
    renderPage();
    await act(async () => {
      fireEvent.change(document.querySelector('input[type="file"]')!, {
        target: { files: [new File([], "avatar.png", { type: "image/png" })] },
      });
    });
    expect(mockMessage.error).toHaveBeenCalledWith("incidents.attachments.empty_file");
    expect(upload).not.toHaveBeenCalled();
    upload.mockRestore();
  });

  it("should disable username input when isEditing is false", () => {
    renderPage();

    const usernameInput =
      document.querySelector<HTMLInputElement>("input#username");
    expect(usernameInput).not.toBeNull();
    expect(usernameInput).toBeDisabled();
  });

  it("should disable email input when isEditing is false", () => {
    renderPage();

    const emailInput = document.querySelector<HTMLInputElement>("input#email");
    expect(emailInput).not.toBeNull();
    expect(emailInput).toBeDisabled();
  });

  it("should disable phoneNumber input when isEditing is false", () => {
    renderPage();

    const phoneInput =
      document.querySelector<HTMLInputElement>("input#phoneNumber");
    expect(phoneInput).not.toBeNull();
    expect(phoneInput).toBeDisabled();
  });

  it("should enable form inputs when Edit button is clicked", async () => {
    renderPage();

    const inputsBefore = getFormInputs();
    inputsBefore.forEach((input) => {
      expect(input).toBeDisabled();
    });

    fireEvent.click(screen.getByText("myProfile.buttons.edit"));

    await waitFor(() => {
      const inputsAfter = getFormInputs();
      expect(inputsAfter.length).toBeGreaterThanOrEqual(3);
      const usernameInput =
        document.querySelector<HTMLInputElement>("input#username");
      expect(usernameInput).not.toBeDisabled();
      expect(
        document.querySelector<HTMLInputElement>("input#email"),
      ).toBeDisabled();
    });
  });

  it("should return form inputs to disabled state when Cancel button is clicked", async () => {
    renderPage();

    fireEvent.click(screen.getByText("myProfile.buttons.edit"));

    await waitFor(() => {
      const usernameInput =
        document.querySelector<HTMLInputElement>("input#username");
      expect(usernameInput).not.toBeDisabled();
    });

    fireEvent.click(screen.getByText("myProfile.buttons.cancel"));

    await waitFor(() => {
      const inputs = getFormInputs();
      inputs.forEach((input) => {
        expect(input).toBeDisabled();
      });
    });
  });
});

describe("P-13.B — changePassword API only called when both password fields are filled", () => {
  const setupUpdateProfileSuccess = () => {
    mockMutate.mockImplementation(
      (_args: unknown, callbacks: { onSuccess?: () => void }) => {
        callbacks?.onSuccess?.();
      },
    );
    vi.mocked(useUpdateProfile).mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    } as Partial<ReturnType<typeof useUpdateProfile>> as ReturnType<
      typeof useUpdateProfile
    >);
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authApi.refreshToken).mockResolvedValue({
      token: "renewed-token",
      id: "user-1",
      username: "jdoe",
      roles: [],
      permissions: ["USER_MANAGE_PROFILE"],
      isActive: true,
      isFirstLogin: false,
    });
    vi.mocked(useMyProfile).mockReturnValue(
      makeQueryResult({
        data: testProfile,
        isLoading: false,
        isError: false,
        refetch: vi.fn(),
      }),
    );
    setupUpdateProfileSuccess();
  });

  const enterEditAndSave = async (
    opts: {
      username?: string;
      currentPassword?: string;
      newPassword?: string;
    } = {},
  ) => {
    renderPage();

    fireEvent.click(screen.getByText("myProfile.buttons.edit"));

    await waitFor(() => {
      expect(
        screen.getByText("myProfile.change_password.title"),
      ).toBeInTheDocument();
    });

    if (opts.username) {
      const usernameInput =
        document.querySelector<HTMLInputElement>("input#username");
      expect(usernameInput).not.toBeNull();
      await act(async () => {
        fireEvent.change(usernameInput!, {
          target: { value: opts.username },
        });
      });
    }

    if (opts.currentPassword) {
      const currentPwdInput = document.querySelector<HTMLInputElement>(
        "input#currentPassword",
      );
      expect(currentPwdInput).not.toBeNull();
      await act(async () => {
        fireEvent.change(currentPwdInput!, {
          target: { value: opts.currentPassword },
        });
      });
    }

    if (opts.newPassword) {
      const newPwdInput =
        document.querySelector<HTMLInputElement>("input#newPassword");
      expect(newPwdInput).not.toBeNull();
      await act(async () => {
        fireEvent.change(newPwdInput!, { target: { value: opts.newPassword } });
      });
    }

    await act(async () => {
      fireEvent.click(screen.getByText("myProfile.buttons.save"));
    });
  };

  it("should not call changePassword when both password fields are empty", async () => {
    await enterEditAndSave({
      currentPassword: undefined,
      newPassword: undefined,
    });

    await waitFor(() => {
      expect(authApi.changePassword).not.toHaveBeenCalled();
    });
  });

  it("should not call changePassword when only currentPassword is filled", async () => {
    await enterEditAndSave({
      currentPassword: "OldPass123",
      newPassword: undefined,
    });

    await waitFor(() => {
      expect(authApi.changePassword).not.toHaveBeenCalled();
    });
  });

  it("should not call changePassword when only newPassword is filled", async () => {
    await enterEditAndSave({
      currentPassword: undefined,
      newPassword: "NewPass123",
    });

    await waitFor(() => {
      expect(authApi.changePassword).not.toHaveBeenCalled();
    });
  });

  it("should call changePassword when both currentPassword and newPassword are filled", async () => {
    await enterEditAndSave({
      currentPassword: "OldPass123",
      newPassword: "NewPass456!",
    });

    await waitFor(() => {
      expect(authApi.changePassword).toHaveBeenCalledTimes(1);
      expect(authApi.changePassword).toHaveBeenCalledWith("user-1", {
        currentPassword: "OldPass123",
        newPassword: "NewPass456!",
      });
    });
  });

  it("should call changePassword with the correct userId and credentials when both fields are filled", async () => {
    await enterEditAndSave({
      currentPassword: "Secret1!",
      newPassword: "Secret2!",
    });

    await waitFor(() => {
      expect(authApi.changePassword).toHaveBeenCalledWith(
        "user-1",
        expect.objectContaining({
          currentPassword: "Secret1!",
          newPassword: "Secret2!",
        }),
      );
    });
  });

  it("should renew JWT and auth store after changing username", async () => {
    vi.mocked(authApi.refreshToken).mockResolvedValue({
      token: "renewed-token",
      id: "user-1",
      username: "jean pierre",
      roles: ["AGENT"],
      permissions: ["USER_MANAGE_PROFILE"],
      isActive: true,
      isFirstLogin: false,
    });

    await enterEditAndSave({ username: "  jean pierre  " });

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledWith(
        {
          id: "user-1",
          data: expect.objectContaining({ username: "jean pierre" }),
        },
        expect.any(Object),
      );
      expect(authApi.refreshToken).toHaveBeenCalledTimes(1);
      expect(mockUpdateAuthSession).toHaveBeenCalledWith(
        "renewed-token",
        expect.objectContaining({
          username: "jean pierre",
          isFirstLogin: false,
        }),
      );
    });
  });
});
