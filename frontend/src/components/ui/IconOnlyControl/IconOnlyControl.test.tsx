// Tests : controles a icone seule (label accessible et bouton).

import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { IconOnlyLabel, IconOnlyButton } from "./IconOnlyControl";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";

describe("IconOnlyLabel", () => {
  it("should expose the label as aria-label and render the icon", () => {
    renderWithProviders(
      <IconOnlyLabel label="Statut" icon={<span data-testid="icon" />} />,
    );
    expect(screen.getByLabelText("Statut")).toBeInTheDocument();
    expect(screen.getByTestId("icon")).toBeInTheDocument();
  });
});

describe("IconOnlyButton", () => {
  it("should render a button whose accessible name is the label", () => {
    renderWithProviders(<IconOnlyButton label="Supprimer" icon={<span />} />);
    expect(
      screen.getByRole("button", { name: "Supprimer" }),
    ).toBeInTheDocument();
  });

  it("should trigger onClick when clicked", async () => {
    const onClick = vi.fn();
    renderWithProviders(
      <IconOnlyButton label="Supprimer" icon={<span />} onClick={onClick} />,
    );
    await userEvent.click(screen.getByRole("button", { name: "Supprimer" }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("should wrap the icon in a class when iconClassName is provided", () => {
    renderWithProviders(
      <IconOnlyButton
        label="Editer"
        icon={<span data-testid="ico" />}
        iconClassName="spin"
      />,
    );
    expect(screen.getByTestId("ico").parentElement).toHaveClass("spin");
  });
});
