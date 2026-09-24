// Tests frontend : verifie le comportement de main layout.test.

import { render, screen } from "@testing-library/react";
import { describe, it, expect, beforeEach, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import MainLayout from "./MainLayout";
import { useAuthStore } from "../../store/authStore/authStore";

vi.mock("../Sidebar/Sidebar", () => ({
  default: () => <div data-testid="sidebar">Sidebar</div>,
}));

vi.mock("../Header/AppHeader", () => ({
  default: () => <div data-testid="app-header">AppHeader</div>,
}));

vi.mock("../FirstLoginModal/FirstLoginModal", () => ({
  default: () => null,
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

// Couvre les comportements du module teste.
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{children}</MemoryRouter>
    </QueryClientProvider>
  );
};

describe("MainLayout", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: null, isAuthenticated: false });
  });

  it("should render the sidebar component", () => {
    render(<MainLayout />, { wrapper: createWrapper() });
    expect(screen.getByTestId("sidebar")).toBeInTheDocument();
  });

  it("should render the app header component", () => {
    render(<MainLayout />, { wrapper: createWrapper() });
    expect(screen.getByTestId("app-header")).toBeInTheDocument();
  });

  it("should render the main layout container", () => {
    render(<MainLayout />, { wrapper: createWrapper() });
    expect(screen.getByTestId("main-layout")).toBeInTheDocument();
  });

  it("should render the content area for child routes", () => {
    render(<MainLayout />, { wrapper: createWrapper() });
    expect(screen.getByTestId("app-content")).toBeInTheDocument();
  });

  it("should not mount application chrome during first login", () => {
    useAuthStore.setState({
      user: {
        id: "user-1",
        username: "newuser",
        roles: [],
        permissions: [],
        isFirstLogin: true,
      },
      isAuthenticated: true,
    });

    render(<MainLayout />, { wrapper: createWrapper() });

    expect(screen.queryByTestId("sidebar")).not.toBeInTheDocument();
    expect(screen.queryByTestId("app-header")).not.toBeInTheDocument();
    expect(screen.queryByTestId("app-content")).not.toBeInTheDocument();
  });
});
