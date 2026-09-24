// Tests frontend : verifie le comportement de profil form.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, screen, waitFor } from "@testing-library/react";
import { Form } from "antd";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import ProfileForm from "./ProfileForm";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

// Type les props du wrapper de formulaire de profil.
interface WrapperProps {
  isEditing?: boolean;
  isActive?: boolean;
  canChangePassword?: boolean;
}

// Rend le composant Wrapper pour l'interface profil form.test.
const Wrapper = ({
  isEditing = false,
  isActive = true,
  canChangePassword = false,
}: WrapperProps) => {
  const [form] = Form.useForm();
  return (
    <ProfileForm
      form={form}
      initialValues={{
        username: "jdoe",
        email: "jdoe@example.com",
        phoneNumber: 771234567,
      }}
      isEditing={isEditing}
      isActive={isActive}
      canChangePassword={canChangePassword}
    />
  );
};

describe("ProfileForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render profile fields with initial values when in view mode", () => {
    renderWithProviders(<Wrapper />);
    expect(screen.getByDisplayValue("jdoe")).toBeInTheDocument();
    expect(screen.getByDisplayValue("jdoe@example.com")).toBeInTheDocument();
    expect(screen.getByDisplayValue("771 234 567")).toBeInTheDocument();
  });

  it("should not render password fields when in view mode", () => {
    renderWithProviders(<Wrapper />);
    expect(
      document.querySelector('input[id*="currentPassword"]'),
    ).not.toBeInTheDocument();
    expect(
      document.querySelector('input[id*="newPassword"]'),
    ).not.toBeInTheDocument();
    expect(
      document.querySelector('input[id*="confirmPassword"]'),
    ).not.toBeInTheDocument();
  });

  it("should render all profile fields and password section when editing with canChangePassword", () => {
    renderWithProviders(<Wrapper isEditing={true} canChangePassword={true} />);
    expect(screen.getByDisplayValue("jdoe")).not.toBeDisabled();
    expect(screen.getByDisplayValue("jdoe@example.com")).toBeDisabled();
    expect(
      document.querySelector('input[id*="currentPassword"]'),
    ).toBeInTheDocument();
    expect(
      document.querySelector('input[id*="newPassword"]'),
    ).toBeInTheDocument();
    expect(
      document.querySelector('input[id*="confirmPassword"]'),
    ).toBeInTheDocument();
  });

  it("should disable password fields when account is inactive", () => {
    renderWithProviders(
      <Wrapper isEditing={true} isActive={false} canChangePassword={true} />,
    );
    expect(
      document.querySelector('input[id*="currentPassword"]'),
    ).toBeDisabled();
    expect(document.querySelector('input[id*="newPassword"]')).toBeDisabled();
    expect(
      document.querySelector('input[id*="confirmPassword"]'),
    ).toBeDisabled();
  });

  it("should enable password fields when account is active", () => {
    renderWithProviders(
      <Wrapper isEditing={true} isActive={true} canChangePassword={true} />,
    );
    expect(
      document.querySelector('input[id*="currentPassword"]'),
    ).not.toBeDisabled();
    expect(
      document.querySelector('input[id*="newPassword"]'),
    ).not.toBeDisabled();
    expect(
      document.querySelector('input[id*="confirmPassword"]'),
    ).not.toBeDisabled();
  });

  it("should not render password section when canChangePassword is false", () => {
    renderWithProviders(<Wrapper isEditing={true} canChangePassword={false} />);
    expect(
      document.querySelector('input[id*="currentPassword"]'),
    ).not.toBeInTheDocument();
    expect(
      document.querySelector('input[id*="newPassword"]'),
    ).not.toBeInTheDocument();
    expect(
      document.querySelector('input[id*="confirmPassword"]'),
    ).not.toBeInTheDocument();
  });

  it("should validate password minimum length when editing profile password", async () => {
    renderWithProviders(<Wrapper isEditing={true} canChangePassword={true} />);
    const newPassword = document.querySelector<HTMLInputElement>(
      'input[id*="newPassword"]',
    );
    expect(newPassword).not.toBeNull();

    fireEvent.change(newPassword!, { target: { value: "pass" } });
    fireEvent.blur(newPassword!);

    await waitFor(() => {
      expect(
        screen.getByText("myProfile.validation.password_min_length"),
      ).toBeInTheDocument();
    });
  });
});
