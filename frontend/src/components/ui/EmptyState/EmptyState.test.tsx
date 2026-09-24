// Tests frontend : verifie le comportement de empty state.test.

import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import EmptyState from "./EmptyState";

describe("EmptyState", () => {
  it("should render title, description, icon, and action together", () => {
    render(
      <EmptyState
        icon={<span data-testid="icon">📭</span>}
        title="No incidents found"
        description="Try adjusting your filters."
        action={<button>Create</button>}
      />,
    );
    expect(screen.getByText("No incidents found")).toBeInTheDocument();
    expect(screen.getByText("Try adjusting your filters.")).toBeInTheDocument();
    expect(screen.getByTestId("icon")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create" })).toBeInTheDocument();
  });

  it("should render with only a title when all optional slots are absent", () => {
    const { container } = render(<EmptyState title="Empty" />);
    expect(screen.getByText("Empty")).toBeInTheDocument();
    expect(
      container.querySelector('[aria-hidden="true"]'),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it('should have role="status" and aria-label equal to the title', () => {
    render(<EmptyState title="No data" />);
    const el = screen.getByRole("status");
    expect(el).toHaveAttribute("aria-label", "No data");
  });

  it("should hide the decorative icon from screen readers with aria-hidden", () => {
    const { container } = render(
      <EmptyState title="Empty" icon={<span>📭</span>} />,
    );
    expect(container.querySelector('[aria-hidden="true"]')).toBeInTheDocument();
  });

  it("should apply a custom className to the container", () => {
    const { container } = render(
      <EmptyState title="Empty" className="my-empty" />,
    );
    expect(container.querySelector(".my-empty")).toBeInTheDocument();
  });
});
