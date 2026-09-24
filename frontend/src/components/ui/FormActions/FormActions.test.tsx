// Tests frontend : verifie le comportement de la barre d'actions FormActions.

import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import FormActions from "./FormActions";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

describe("FormActions", () => {
  it("should render the submit button with the provided label", () => {
    render(<FormActions submitText="Save" />);
    expect(screen.getByRole("button", { name: "Save" })).toBeInTheDocument();
  });

  it("should render a cancel button only when onCancel is provided", () => {
    const { rerender } = render(<FormActions submitText="Save" />);
    expect(
      screen.queryByRole("button", { name: "common.cancel" }),
    ).not.toBeInTheDocument();

    rerender(<FormActions submitText="Save" onCancel={() => {}} />);
    expect(
      screen.getByRole("button", { name: "common.cancel" }),
    ).toBeInTheDocument();
  });

  it("should call onCancel when the cancel button is clicked", () => {
    const onCancel = vi.fn();
    render(<FormActions submitText="Save" onCancel={onCancel} />);
    fireEvent.click(screen.getByRole("button", { name: "common.cancel" }));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
