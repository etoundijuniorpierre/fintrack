// Tests : modale de contact administrateur (validation, envoi, fermeture).

import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import ContactAdminModal from "./ContactAdminModal";
import { authApi } from "../../../../../api/user/auth/authApi";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, fallback?: string) => fallback ?? key,
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../../api/user/auth/authApi", () => ({
  authApi: { contactAdmin: vi.fn() },
}));

const api = vi.mocked(authApi);

describe("ContactAdminModal", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should not render the content when open is false", () => {
    renderWithProviders(
      <ContactAdminModal open={false} onCancel={vi.fn()} username="" />,
    );
    expect(
      screen.queryByText("login.contact_admin_title"),
    ).not.toBeInTheDocument();
  });

  it("should render the title when open is true", () => {
    renderWithProviders(
      <ContactAdminModal open onCancel={vi.fn()} username="" />,
    );
    expect(screen.getByText("login.contact_admin_title")).toBeInTheDocument(); // Because fallback is used
  });

  it("should not send the message when the form is invalid", async () => {
    renderWithProviders(
      <ContactAdminModal open onCancel={vi.fn()} username="" />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: "login.contact_admin_send" }),
    );
    await waitFor(() =>
      expect(screen.getByText("login.username_required")).toBeInTheDocument(),
    );
    expect(api.contactAdmin).not.toHaveBeenCalled();
  });

  it("should send the message and close the modal when the form is valid", async () => {
    api.contactAdmin.mockResolvedValue(undefined as never);
    const onCancel = vi.fn();
    renderWithProviders(
      <ContactAdminModal open onCancel={onCancel} username="etoun" />,
    );

    await userEvent.type(
      screen.getByLabelText("login.subject_label"),
      "Acces bloque",
    );
    await userEvent.type(
      screen.getByLabelText("login.message_label"),
      "Je ne peux plus me connecter.",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "login.contact_admin_send" }),
    );

    await waitFor(() =>
      expect(api.contactAdmin).toHaveBeenCalledWith({
        username: "etoun",
        subject: "Acces bloque",
        message: "Je ne peux plus me connecter.",
      }),
    );
    await waitFor(() => expect(onCancel).toHaveBeenCalled());
  });
});
