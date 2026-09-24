// Tests frontend : verifie le comportement de ressource navigation.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import type { NavigateFunction } from "react-router-dom";
import { resourceNavigation } from "./resourceNavigation";

const mockNavigate = vi.fn() as unknown as NavigateFunction;

describe("resourceNavigation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("getResourceDetailPath", () => {
    it("should return empty string for undefined or placeholder IDs", () => {
      expect(
        resourceNavigation.getResourceDetailPath("INCIDENT", undefined),
      ).toBe("");
      expect(resourceNavigation.getResourceDetailPath("USER", "-")).toBe("");
      expect(
        resourceNavigation.getResourceDetailPath("ROLE", "undefined"),
      ).toBe("");
    });

    it("should return correct path for INCIDENT type", () => {
      expect(
        resourceNavigation.getResourceDetailPath("INCIDENT", "inc-123"),
      ).toBe("/dashboard/incidents/inc-123");
    });

    it("should return correct path for USER type", () => {
      expect(resourceNavigation.getResourceDetailPath("USER", "usr-456")).toBe(
        "/dashboard/users/usr-456",
      );
    });

    it("should return correct path for ROLE type", () => {
      expect(resourceNavigation.getResourceDetailPath("ROLE", "rol-789")).toBe(
        "/dashboard/settings/roles/rol-789",
      );
    });

    it("should return correct path for AGENCY type", () => {
      expect(
        resourceNavigation.getResourceDetailPath("AGENCY", "age-111"),
      ).toBe("/dashboard/settings/agencies/age-111");
    });

    it("should return correct path for SERVICE type", () => {
      expect(
        resourceNavigation.getResourceDetailPath("SERVICE", "srv-222"),
      ).toBe("/dashboard/settings/services/srv-222");
    });

    it("should return correct path for REPORT type", () => {
      expect(
        resourceNavigation.getResourceDetailPath("REPORT", "rep-333"),
      ).toBe("/dashboard/reports");
    });

    it("should return empty string for unknown resource type", () => {
      expect(
        resourceNavigation.getResourceDetailPath("UNKNOWN_TYPE", "some-id"),
      ).toBe("");
    });
  });

  describe("navigateToResourceDetail", () => {
    it("should navigate to the resolved path and invoke onClose if provided", () => {
      const mockOnClose = vi.fn();
      resourceNavigation.navigateToResourceDetail(
        mockNavigate,
        "INCIDENT",
        "inc-123",
        { onClose: mockOnClose },
      );

      expect(mockNavigate).toHaveBeenCalledWith("/dashboard/incidents/inc-123");
      expect(mockOnClose).toHaveBeenCalled();
    });

    it("should navigate to the resolved path without onClose if not provided", () => {
      resourceNavigation.navigateToResourceDetail(
        mockNavigate,
        "USER",
        "usr-456",
      );

      expect(mockNavigate).toHaveBeenCalledWith("/dashboard/users/usr-456");
    });

    it("should not navigate or invoke onClose if path is not resolved", () => {
      const mockOnClose = vi.fn();
      resourceNavigation.navigateToResourceDetail(
        mockNavigate,
        "UNKNOWN",
        "id",
        { onClose: mockOnClose },
      );

      expect(mockNavigate).not.toHaveBeenCalled();
      expect(mockOnClose).not.toHaveBeenCalled();
    });
  });
});
