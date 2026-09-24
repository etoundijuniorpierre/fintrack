// Tests frontend : verifie le comportement de split layout.test.

import { describe, it, expect } from "vitest";
import { screen } from "@testing-library/react";
import SplitLayout from "./SplitLayout";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";

describe("SplitLayout", () => {
  it("should render main and sidebar content", () => {
    renderWithProviders(
      <SplitLayout
        main={<div data-testid="main-content">Main</div>}
        sidebar={<div data-testid="sidebar-content">Sidebar</div>}
      />,
    );

    expect(screen.getByTestId("main-content")).toBeInTheDocument();
    expect(screen.getByTestId("sidebar-content")).toBeInTheDocument();
  });

  it("should apply sidebarWidth as a CSS variable", () => {
    const width = "400px";
    renderWithProviders(
      <SplitLayout
        main={<div>Main</div>}
        sidebar={<div>Sidebar</div>}
        sidebarWidth={width}
      />,
    );

    const container = screen.getByTestId("split-layout");
    expect(container).toHaveStyle({ "--split-sidebar-width": width });
  });

  it("should apply custom className", () => {
    const customClass = "custom-split";
    renderWithProviders(
      <SplitLayout
        main={<div>Main</div>}
        sidebar={<div>Sidebar</div>}
        className={customClass}
      />,
    );

    const container = screen.getByTestId("split-layout");
    expect(container).toHaveClass(customClass);
  });

  it("should apply custom styles", () => {
    const customStyle = { padding: "20px" };
    renderWithProviders(
      <SplitLayout
        main={<div>Main</div>}
        sidebar={<div>Sidebar</div>}
        style={customStyle}
      />,
    );

    const container = screen.getByTestId("split-layout");
    expect(container).toHaveStyle("padding: 20px");
  });
});
