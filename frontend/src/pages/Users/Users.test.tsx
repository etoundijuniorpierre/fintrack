// Tests frontend : verifie le comportement de users.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import UsersWithLayout from "./Users";

vi.mock("../../components/Sidebar/Sidebar", () => ({
  default: () => <div data-testid="sidebar" />,
}));

vi.mock("../../components/Header/AppHeader", () => ({
  default: () => <div data-testid="app-header" />,
}));

// Prepare l'affichage lisible de users.test.
const renderUsersWithLayout = () =>
  render(
    <MemoryRouter>
      <Routes>
        <Route path="/" element={<UsersWithLayout />} />
      </Routes>
    </MemoryRouter>,
  );

describe("UsersWithLayout", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should render the sidebar and app header when mounted", () => {
    renderUsersWithLayout();
    expect(screen.getByTestId("sidebar")).toBeInTheDocument();
    expect(screen.getByTestId("app-header")).toBeInTheDocument();
  });

  it("should render child route content when a nested route matches", () => {
    render(
      <MemoryRouter initialEntries={["/users"]}>
        <Routes>
          <Route path="/users" element={<UsersWithLayout />}>
            <Route index element={<div data-testid="child-route">Child</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByTestId("child-route")).toBeInTheDocument();
  });
});
