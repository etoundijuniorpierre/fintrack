// Tests frontend : verifie le comportement de spinner.test.

import { describe, it, expect, beforeEach, vi } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import Spinner, { type SpinnerProps } from "./Spinner";

// Prepare l'affichage lisible de spinner.test.
const renderSpinner = (props: SpinnerProps = {}) =>
  renderWithProviders(<Spinner {...props} />);

describe("Spinner", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render with role status and default aria-label when no props are provided", () => {
    renderSpinner();
    const wrapper = screen.getByRole("status");
    expect(wrapper).toBeInTheDocument();
    expect(wrapper.querySelector('[aria-label="Loading"]')).toBeInTheDocument();
  });

  it("should use a custom aria-label when label prop is provided", () => {
    renderSpinner({ label: "Loading users..." });
    expect(
      screen
        .getByRole("status")
        .querySelector('[aria-label="Loading users..."]'),
    ).toBeInTheDocument();
  });

  it("should render a div wrapper when centered is true", () => {
    renderSpinner({ centered: true });
    expect(screen.getByRole("status").tagName).toBe("DIV");
  });

  it("should render a span wrapper when centered is false", () => {
    renderSpinner({ centered: false });
    expect(screen.getByRole("status").tagName).toBe("SPAN");
  });

  it("should apply centering flex styles when centered is true", () => {
    renderSpinner({ centered: true });
    expect(screen.getByRole("status")).toHaveStyle({
      display: "flex",
      justifyContent: "center",
    });
  });

  it("should apply the small Ant Design class when size is sm", () => {
    const { container } = renderSpinner({ size: "sm" });
    expect(container.querySelector(".ant-spin-sm")).toBeInTheDocument();
  });

  it("should apply the large Ant Design class when size is lg", () => {
    const { container } = renderSpinner({ size: "lg" });
    expect(container.querySelector(".ant-spin-lg")).toBeInTheDocument();
  });

  it("should apply a custom className when className prop is provided", () => {
    const { container } = renderSpinner({ className: "my-spinner" });
    expect(container.querySelector(".my-spinner")).toBeInTheDocument();
  });
});
