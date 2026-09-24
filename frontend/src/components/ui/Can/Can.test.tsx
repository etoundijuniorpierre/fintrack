// Tests : garde de permission Can (affichage conditionnel selon les droits).

import { screen } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import Can from "./Can";
import { useAuth } from "../../../hooks/auth/useAuth";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";

vi.mock("../../../hooks/auth/useAuth", () => ({ useAuth: vi.fn() }));

const mockAuth = (granted: string[]) => {
  vi.mocked(useAuth).mockReturnValue({
    user: null,
    isAuthenticated: false,
    hasRole: vi.fn().mockReturnValue(false),
    hasPermission: vi.fn((code: string) => granted.includes(code)),
  });
};

describe("Can", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should render children when the single permission is granted", () => {
    mockAuth(["INCIDENT_CREATE"]);
    renderWithProviders(
      <Can permission="INCIDENT_CREATE">
        <span>ok</span>
      </Can>,
    );
    expect(screen.getByText("ok")).toBeInTheDocument();
  });

  it("should hide children when the single permission is denied", () => {
    mockAuth([]);
    renderWithProviders(
      <Can permission="INCIDENT_CREATE">
        <span>ok</span>
      </Can>,
    );
    expect(screen.queryByText("ok")).not.toBeInTheDocument();
  });

  it("should render the fallback when access is denied", () => {
    mockAuth([]);
    renderWithProviders(
      <Can permission="INCIDENT_CREATE" fallback={<span>refuse</span>}>
        <span>ok</span>
      </Can>,
    );
    expect(screen.getByText("refuse")).toBeInTheDocument();
    expect(screen.queryByText("ok")).not.toBeInTheDocument();
  });

  it("should render children when at least one anyOf permission is granted", () => {
    mockAuth(["USER_VIEW_ALL"]);
    renderWithProviders(
      <Can anyOf={["INCIDENT_CREATE", "USER_VIEW_ALL"]}>
        <span>ok</span>
      </Can>,
    );
    expect(screen.getByText("ok")).toBeInTheDocument();
  });

  it("should hide children when no anyOf permission is granted", () => {
    mockAuth([]);
    renderWithProviders(
      <Can anyOf={["INCIDENT_CREATE", "USER_VIEW_ALL"]}>
        <span>ok</span>
      </Can>,
    );
    expect(screen.queryByText("ok")).not.toBeInTheDocument();
  });

  it("should hide children when neither permission nor anyOf is provided", () => {
    mockAuth(["INCIDENT_CREATE"]);
    renderWithProviders(
      <Can>
        <span>ok</span>
      </Can>,
    );
    expect(screen.queryByText("ok")).not.toBeInTheDocument();
  });
});
