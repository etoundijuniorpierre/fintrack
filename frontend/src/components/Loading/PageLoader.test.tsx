// Tests frontend : verifie le comportement de page loader.test.

import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import PageLoader from "./PageLoader";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

describe("PageLoader", () => {
  it("should render the spinner and hide children when isLoading is true", () => {
    render(
      <PageLoader isLoading={true}>
        <p>Content</p>
      </PageLoader>,
    );
    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(screen.queryByText("Content")).not.toBeInTheDocument();
  });

  it("should have an accessible label on the spinner", () => {
    render(
      <PageLoader isLoading={true}>
        <span />
      </PageLoader>,
    );
    expect(screen.getByRole("status")).toHaveAttribute(
      "aria-label",
      "common.loading",
    );
  });

  it("should render the error message and hide children when isError is true", () => {
    render(
      <PageLoader
        isLoading={false}
        isError={true}
        errorMessage="Something went wrong"
      >
        <p>Content</p>
      </PageLoader>,
    );
    expect(screen.getByText("Something went wrong")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.queryByText("Content")).not.toBeInTheDocument();
  });

  it("should render the default error message when errorMessage is omitted", () => {
    render(
      <PageLoader isLoading={false} isError={true}>
        <span />
      </PageLoader>,
    );
    expect(screen.getByText("common.loading_error")).toBeInTheDocument();
  });

  it("should render the Retry button and call onRetry when clicked", async () => {
    const onRetry = vi.fn();
    render(
      <PageLoader isLoading={false} isError={true} onRetry={onRetry}>
        <span />
      </PageLoader>,
    );
    const retryButton = screen.getByRole("button", { name: "common.retry" });
    expect(retryButton).toBeInTheDocument();
    await userEvent.click(retryButton);
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it("should not render the Retry button when onRetry is not provided", () => {
    render(
      <PageLoader isLoading={false} isError={true}>
        <span />
      </PageLoader>,
    );
    expect(
      screen.queryByRole("button", { name: "common.retry" }),
    ).not.toBeInTheDocument();
  });

  it("should render children when not loading and not in error state", () => {
    render(
      <PageLoader isLoading={false}>
        <p>Ready content</p>
      </PageLoader>,
    );
    expect(screen.getByText("Ready content")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("should show the spinner when both isLoading and isError are true", () => {
    render(
      <PageLoader isLoading={true} isError={true} errorMessage="Err">
        <p>Content</p>
      </PageLoader>,
    );
    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(screen.queryByText("Err")).not.toBeInTheDocument();
  });
});
