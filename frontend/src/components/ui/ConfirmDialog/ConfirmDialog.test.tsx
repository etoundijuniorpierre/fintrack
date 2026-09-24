// Tests frontend : verifie le comportement de confirm dialog.test.

import { type ComponentProps } from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { ConfigProvider, App as AntdApp } from "antd";
import ConfirmDialog from "./ConfirmDialog";

// Prepare l'affichage lisible de confirm dialog.test.
const renderDialog = (
  props: Partial<ComponentProps<typeof ConfirmDialog>> = {},
) =>
  render(
    <ConfigProvider>
      <AntdApp>
        <ConfirmDialog title="Delete iteme" onConfirm={vi.fn()} {...props}>
          <button>Trigger</button>
        </ConfirmDialog>
      </AntdApp>
    </ConfigProvider>,
  );

// Pilote l'interaction de test liee a confirm dialog.test.
const open = () =>
  fireEvent.click(screen.getByRole("button", { name: "Trigger" }));

describe("ConfirmDialog", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should not show the popover before the trigger is clicked", () => {
    renderDialog();
    expect(screen.queryByText("Delete iteme")).not.toBeInTheDocument();
  });

  it("should show title and description after trigger click", async () => {
    renderDialog({ description: "This cannot be undone." });
    open();
    await waitFor(() =>
      expect(screen.getByText("Delete iteme")).toBeInTheDocument(),
    );
    expect(screen.getByText("This cannot be undone.")).toBeInTheDocument();
  });

  it("should use custom confirmText and cancelText when provided", async () => {
    renderDialog({ confirmText: "Yes, delete", cancelText: "No, keep it" });
    open();
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Yes, delete" }),
      ).toBeInTheDocument(),
    );
    expect(
      screen.getByRole("button", { name: "No, keep it" }),
    ).toBeInTheDocument();
  });

  it("should call onConfirm when the confirm button is clicked", async () => {
    const onConfirm = vi.fn();
    renderDialog({ onConfirm });
    open();
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Confirm" }),
      ).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole("button", { name: "Confirm" }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("should call onCancel when the cancel button is clicked", async () => {
    const onCancel = vi.fn();
    renderDialog({ onCancel });
    open();
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Cancel" }),
      ).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("should show the confirm button in loading state when confirmLoading is true", async () => {
    renderDialog({ confirmLoading: true });
    open();
    await waitFor(() =>
      expect(screen.getByText("Confirm")).toBeInTheDocument(),
    );
    expect(screen.getByText("Confirm").closest("button")).toHaveClass(
      "ant-btn-loading",
    );
  });

  it("should apply danger styling to the confirm button when danger is true", async () => {
    renderDialog({ danger: true });
    open();
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Confirm" }),
      ).toBeInTheDocument(),
    );
    expect(screen.getByRole("button", { name: "Confirm" })).toHaveClass(
      "ant-btn-dangerous",
    );
  });
});
