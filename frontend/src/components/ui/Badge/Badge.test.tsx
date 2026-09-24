// Tests frontend : verifie le comportement de badge.test.

import { describe, it, expect } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import Badge from "./Badge";
import type { BadgeVariant } from "../types";

const VARIANTS: BadgeVariant[] = ["low", "medium", "high", "critical"];

describe("Badge", () => {
  it("should render the label text", () => {
    renderWithProviders(<Badge variant="low" label="Low Risk" />);
    expect(screen.getByText("Low Risk")).toBeInTheDocument();
  });

  it("should render an aria-label describing the badge", () => {
    renderWithProviders(<Badge variant="high" label="High" />);
    expect(screen.getByLabelText("Criticality: High")).toBeInTheDocument();
  });

  it("should apply a custom className", () => {
    renderWithProviders(
      <Badge variant="medium" label="Medium" className="extra-class" />,
    );
    const badge = screen.getByText("Medium");
    expect(badge).toHaveClass("extra-class");
  });

  it.each(VARIANTS)(
    'should render without crashing for variant "%s"',
    (variant) => {
      renderWithProviders(<Badge variant={variant} label={variant} />);
      expect(screen.getByText(variant)).toBeInTheDocument();
    },
  );
});
