// Tests frontend : verifie le comportement de users.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import type { NavigateFunction } from "react-router-dom";
import { userNavigation } from "./users";

const mockNavigate = vi.fn() as unknown as NavigateFunction;

describe("userNavigation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should navigate to /dashboard/users when navigateToUsers is called", () => {
    userNavigation.navigateToUsers(mockNavigate);
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/users");
  });

  it("should navigate to /dashboard/users/create when navigateToUserCreate is called", () => {
    userNavigation.navigateToUserCreate(mockNavigate);
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/users/create");
  });

  it("should navigate to /dashboard/users/:id when navigateToUserDetail is called", () => {
    userNavigation.navigateToUserDetail(mockNavigate, "user-1");
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/users/user-1");
  });

  it("should navigate to /dashboard/users/:id with edit state when navigateToUserDetailEdit is called", () => {
    userNavigation.navigateToUserDetailEdit(mockNavigate, "user-1");
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/users/user-1", {
      state: { edit: true },
    });
  });

  it("should navigate to /dashboard/users/edit/:id when navigateToUserEdit is called", () => {
    userNavigation.navigateToUserEdit(mockNavigate, "user-1");
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/users/edit/user-1");
  });
});
