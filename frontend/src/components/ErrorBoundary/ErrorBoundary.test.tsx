// Tests frontend : verifie le comportement de error boundary.test.

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ConfigProvider, App as AntdApp } from "antd";
import ErrorBoundary from "./ErrorBoundary";

vi.mock("../../i18n", () => ({
  default: {
    t: (key: string) => key,
  },
}));

// Rend le composant de test declenchant l'ErrorBoundary.
const ThrowingChild = ({
  shouldThrow = false,
  message = "Test error",
}: {
  shouldThrow?: boolean;
  message?: string;
}) => {
  if (shouldThrow) throw new Error(message);
  return <div data-testid="child">Safe content</div>;
};

// Prepare l'affichage lisible du test ErrorBoundary.
const renderBoundary = (
  props: Partial<React.ComponentProps<typeof ErrorBoundary>> & {
    shouldThrow?: boolean;
    errorMessage?: string;
  } = {},
) => {
  const {
    shouldThrow = false,
    errorMessage = "Test error",
    fallback,
    children,
  } = props;
  return render(
    <ConfigProvider>
      <AntdApp>
        <ErrorBoundary fallback={fallback}>
          {children ?? (
            <ThrowingChild shouldThrow={shouldThrow} message={errorMessage} />
          )}
        </ErrorBoundary>
      </AntdApp>
    </ConfigProvider>,
  );
};

describe("ErrorBoundary", () => {
  let consoleErrorSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  it("should render children when no error is thrown", () => {
    renderBoundary();
    expect(screen.getByTestId("child")).toBeInTheDocument();
    expect(screen.queryByText("Something went wrong")).not.toBeInTheDocument();
  });

  it("should catch a render error and show the generic fallback UI when an error is thrown", () => {
    renderBoundary({ shouldThrow: true, errorMessage: "Boom!" });
    expect(screen.getByText("common.boundary_title")).toBeInTheDocument();
    // Le detail technique de l'erreur n'est jamais expose a l'utilisateur.
    expect(screen.queryByText("Boom!")).not.toBeInTheDocument();
    expect(screen.queryByTestId("child")).not.toBeInTheDocument();
  });

  it("should show a generic subtitle without exposing technical details", () => {
    renderBoundary({
      shouldThrow: true,
      errorMessage: "Sensitive technical detail",
    });
    expect(screen.getByText("common.boundary_message")).toBeInTheDocument();
    expect(
      screen.queryByText("Sensitive technical detail"),
    ).not.toBeInTheDocument();
  });

  it("should log the error via componentDidCatch", () => {
    renderBoundary({ shouldThrow: true });
    const boundaryCall = consoleErrorSpy.mock.calls.find((args: unknown[]) =>
      String(args[0]).includes("[ErrorBoundary]"),
    );
    expect(boundaryCall).toBeDefined();
  });

  it("should reload the page when the reload button is clicked", () => {
    const reloadMock = vi.fn();
    Object.defineProperty(window, "location", {
      value: { reload: reloadMock },
      writable: true,
    });
    renderBoundary({ shouldThrow: true });
    fireEvent.click(screen.getByRole("button", { name: "common.reload" }));
    expect(reloadMock).toHaveBeenCalledTimes(1);
  });

  it("should render the custom fallback instead of the default UI when fallback prop is provided", () => {
    renderBoundary({
      shouldThrow: true,
      fallback: <p data-testid="custom">Custom error UI</p>,
    });
    expect(screen.getByTestId("custom")).toBeInTheDocument();
    expect(screen.queryByText("common.boundary_title")).not.toBeInTheDocument();
  });
});
