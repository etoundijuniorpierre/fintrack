// Tests frontend : verifie le comportement de modal.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import Modal from "./Modal";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

// Type les options de rendu de la modale de test.
interface RenderModalOptions {
  open?: boolean;
  onClose?: () => void;
  onConfirm?: () => void;
  confirmText?: string;
  cancelText?: string;
  footer?: boolean;
  confirmLoading?: boolean;
}

const renderModal = ({
  open = true,
  onClose = vi.fn(),
  onConfirm = vi.fn(),
  confirmText = "Confirm",
  cancelText = "Cancel",
  footer = true,
  confirmLoading = false,
}: RenderModalOptions = {}) =>
  renderWithProviders(
    <Modal
      open={open}
      title="Test Modal"
      onClose={onClose}
      onConfirm={onConfirm}
      confirmText={confirmText}
      cancelText={cancelText}
      footer={footer}
      confirmLoading={confirmLoading}
    >
      <div>
        <input data-testid="first-input" placeholder="First input" />
        <input data-testid="second-input" placeholder="Second input" />
        <button data-testid="inner-button">Inner Button</button>
      </div>
    </Modal>,
  );

describe("Modal — keyboard navigation (AC-16.4)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("Escape key", () => {
    it("calls onClose when Escape is pressed while the modal is open", async () => {
      const onClose = vi.fn();
      renderModal({ onClose });

      await waitFor(() =>
        expect(screen.getByText("Test Modal")).toBeInTheDocument(),
      );

      await userEvent.keyboard("{Escape}");

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it("does not call onClose when modal is closed", async () => {
      const onClose = vi.fn();
      renderModal({ open: false, onClose });

      await userEvent.keyboard("{Escape}");

      expect(onClose).not.toHaveBeenCalled();
    });
  });

  describe("Enter key on confirm button", () => {
    it("calls onConfirm when Enter is pressed on the confirm button", async () => {
      const onConfirm = vi.fn();
      renderModal({ onConfirm });

      await waitFor(() =>
        expect(screen.getByText("Test Modal")).toBeInTheDocument(),
      );

      const confirmButton = screen.getByRole("button", { name: "Confirm" });
      confirmButton.focus();
      await userEvent.keyboard("{Enter}");

      expect(onConfirm).toHaveBeenCalledTimes(1);
    });

    it("calls onClose when Enter is pressed on the cancel button", async () => {
      const onClose = vi.fn();
      renderModal({ onClose });

      await waitFor(() =>
        expect(screen.getByText("Test Modal")).toBeInTheDocument(),
      );

      const cancelButton = screen.getByRole("button", { name: "Cancel" });
      cancelButton.focus();
      await userEvent.keyboard("{Enter}");

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it("does not call onConfirm when confirmLoading is true", async () => {
      const onConfirm = vi.fn();
      renderModal({ onConfirm, confirmLoading: true });

      await waitFor(() =>
        expect(screen.getByText("Test Modal")).toBeInTheDocument(),
      );

      const confirmButton = screen.getByText("Confirm").closest("button")!;
      expect(confirmButton).toBeDisabled();

      confirmButton.focus();
      await userEvent.keyboard("{Enter}");
      expect(onConfirm).not.toHaveBeenCalled();
    });
  });

  describe("Tab key focus cycling", () => {
    it("Tab key moves focus to the next focusable element within the modal", async () => {
      renderModal();

      await waitFor(() =>
        expect(screen.getByText("Test Modal")).toBeInTheDocument(),
      );

      const firstInput = screen.getByTestId("first-input");
      firstInput.focus();
      expect(document.activeElement).toBe(firstInput);

      await userEvent.tab();

      expect(document.activeElement).not.toBe(firstInput);
    });

    it("Shift+Tab moves focus to the previous focusable element", async () => {
      renderModal();

      await waitFor(() =>
        expect(screen.getByText("Test Modal")).toBeInTheDocument(),
      );

      const secondInput = screen.getByTestId("second-input");
      secondInput.focus();
      expect(document.activeElement).toBe(secondInput);

      await userEvent.tab({ shift: true });

      expect(document.activeElement).not.toBe(secondInput);
    });

    it("all footer buttons are focusable via Tab", async () => {
      renderModal();

      await waitFor(() =>
        expect(screen.getByText("Test Modal")).toBeInTheDocument(),
      );

      const cancelButton = screen.getByRole("button", { name: "Cancel" });
      const confirmButton = screen.getByRole("button", { name: "Confirm" });

      expect(cancelButton).not.toHaveAttribute("tabindex", "-1");
      expect(confirmButton).not.toHaveAttribute("tabindex", "-1");
    });
  });

  describe("rendering", () => {
    it("renders modal content when open", async () => {
      renderModal();
      await waitFor(() =>
        expect(screen.getByText("Test Modal")).toBeInTheDocument(),
      );
      expect(screen.getByTestId("first-input")).toBeInTheDocument();
    });

    it("renders confirm and cancel buttons in footer", async () => {
      renderModal();
      await waitFor(() =>
        expect(screen.getByText("Test Modal")).toBeInTheDocument(),
      );
      expect(
        screen.getByRole("button", { name: "Confirm" }),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "Cancel" }),
      ).toBeInTheDocument();
    });
  });
});
