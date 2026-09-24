// Tests frontend : verifie le comportement de page container.test.

import { describe, it, expect } from "vitest";
import { screen } from "@testing-library/react";
import PageContainer from "./PageContainer";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";

describe("PageContainer", () => {
  it("should render children correctly", () => {
    renderWithProviders(
      <PageContainer>
        <div data-testid="child">Test Child</div>
      </PageContainer>,
    );

    expect(screen.getByTestId("child")).toBeInTheDocument();
    expect(screen.getByText("Test Child")).toBeInTheDocument();
  });

  it("should apply custom className", () => {
    const customClass = "custom-class";
    renderWithProviders(
      <PageContainer className={customClass}>
        <div>Content</div>
      </PageContainer>,
    );

    const container = screen.getByTestId("page-container");
    expect(container).toHaveClass(customClass);
  });

  it("should apply custom styles", () => {
    const customStyle = { backgroundColor: "rgb(255, 0, 0)" };
    renderWithProviders(
      <PageContainer style={customStyle}>
        <div>Content</div>
      </PageContainer>,
    );

    const container = screen.getByTestId("page-container");
    expect(container).toHaveStyle("background-color: rgb(255, 0, 0)");
  });
});
