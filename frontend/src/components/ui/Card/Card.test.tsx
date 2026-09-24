// Tests frontend : verifie le comportement de card.test.

import { describe, it, expect, vi } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import Card from "./Card";

describe("Card", () => {
  it("should render children content", () => {
    renderWithProviders(
      <Card>
        <div data-testid="card-child">Content</div>
      </Card>,
    );
    expect(screen.getByTestId("card-child")).toBeInTheDocument();
  });

  it("should render the title when provided", () => {
    renderWithProviders(
      <Card title="My Card Title">
        <div />
      </Card>,
    );
    expect(screen.getByText("My Card Title")).toBeInTheDocument();
  });

  it("should render the extra slot when provided", () => {
    renderWithProviders(
      <Card extra={<button>Action</button>}>
        <div />
      </Card>,
    );
    expect(screen.getByRole("button", { name: "Action" })).toBeInTheDocument();
  });

  it("should render the footer when provided", () => {
    renderWithProviders(
      <Card footer={<div data-testid="card-footer">Footer content</div>}>
        <div />
      </Card>,
    );
    expect(screen.getByTestId("card-footer")).toBeInTheDocument();
  });

  it("should not render the footer when not provided", () => {
    renderWithProviders(
      <Card>
        <div />
      </Card>,
    );
    expect(screen.queryByTestId("card-footer")).not.toBeInTheDocument();
  });

  it("should call onClick when the card is clicked", () => {
    const onClick = vi.fn();
    renderWithProviders(
      <Card onClick={onClick}>
        <div data-testid="inner">Content</div>
      </Card>,
    );
    fireEvent.click(screen.getByTestId("inner").closest(".ant-card")!);
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("should apply custom className", () => {
    renderWithProviders(
      <Card className="custom-card">
        <div />
      </Card>,
    );
    expect(document.querySelector(".custom-card")).toBeInTheDocument();
  });

  it("should apply custom inline style", () => {
    renderWithProviders(
      <Card style={{ borderRadius: "8px" }}>
        <div />
      </Card>,
    );
    const card = document.querySelector(".ant-card") as HTMLElement;
    expect(card.style.borderRadius).toBe("8px");
  });
});
