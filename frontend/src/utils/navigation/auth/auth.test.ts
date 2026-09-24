// Tests frontend : verifie le comportement de auth.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { authNavigation } from "./auth";
import type { NavigateFunction } from "react-router-dom";

const mockNavigate = vi.fn() as unknown as NavigateFunction;

describe("authNavigation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should navigate to /login when navigateToLogin is called", () => {
    authNavigation.navigateToLogin(mockNavigate);
    expect(mockNavigate).toHaveBeenCalledWith("/login");
  });
});
