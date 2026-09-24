// Tests frontend : verifie le comportement de section card.test.

import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { ConfigProvider, App as AntdApp } from "antd";
import SectionCard from "./SectionCard";

// Prepare l'affichage lisible de section card.test.
const renderCard = (props: React.ComponentProps<typeof SectionCard> = {}) =>
  render(
    <ConfigProvider>
      <AntdApp>
        <SectionCard {...props} />
      </AntdApp>
    </ConfigProvider>,
  );

describe("SectionCard", () => {
  it("should render children and title", () => {
    renderCard({
      title: "Incident Types",
      children: <p data-testid="body">content</p>,
    });
    expect(screen.getByText("Incident Types")).toBeInTheDocument();
    expect(screen.getByTestId("body")).toBeInTheDocument();
  });

  it("should render the toolbar in the card header when toolbar prop is provided", () => {
    renderCard({ title: "My Section", toolbar: <button>Add</button> });
    expect(screen.getByRole("button", { name: "Add" })).toBeInTheDocument();
  });

  it("should apply a custom className when provided", () => {
    const { container } = renderCard({ className: "my-section" });
    expect(container.querySelector(".my-section")).toBeInTheDocument();
  });

  it("should show a skeleton when loading is true", () => {
    const { container } = renderCard({ loading: true });
    expect(container.querySelector(".ant-skeleton")).toBeInTheDocument();
  });

  it("should not show a skeleton when loading is false", () => {
    const { container } = renderCard({
      children: <span data-testid="body">body</span>,
    });
    expect(screen.getByTestId("body")).toBeInTheDocument();
    expect(container.querySelector(".ant-skeleton")).not.toBeInTheDocument();
  });
});
