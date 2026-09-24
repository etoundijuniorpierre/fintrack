import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import SectionTitle from "./SectionTitle";
import { designTokens } from "../../../theme/tokens";

describe("SectionTitle", () => {
  it("renders the title correctly", () => {
    render(<SectionTitle title="Test Title" />);
    expect(
      screen.getByRole("heading", { level: 3, name: "Test Title" }),
    ).toBeInTheDocument();
  });

  it("applies the default primary color to the decorator", () => {
    const { container } = render(<SectionTitle title="Default Color" />);
    const decorator = container.querySelector('div[aria-hidden="true"]');
    expect(decorator).toHaveStyle({
      backgroundColor: designTokens.colorPrimary,
    });
  });

  it("allows overriding the decorator color", () => {
    const { container } = render(
      <SectionTitle title="Custom Color" color="#ff0000" />,
    );
    const decorator = container.querySelector('div[aria-hidden="true"]');
    expect(decorator).toHaveStyle({ backgroundColor: "#ff0000" });
  });

  it("applies the correct size class", () => {
    const { container } = render(
      <SectionTitle title="Small Size" size="small" />,
    );
    // The wrapper div is the parent of the decorator
    const wrapper = container.firstChild as HTMLElement;
    // CSS modules will hash the class name, so we check if the string matches the generic format or just rely on class list containing something
    // A simpler way is to check the class attribute contains 'small'
    expect(wrapper.className).toMatch(/small/);
  });
});
