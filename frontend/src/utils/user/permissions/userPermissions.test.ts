// Tests frontend : verifie le comportement de userPermissions.

import { describe, it, expect } from "vitest";
import { isCurrentUser, canPerformDestructiveAction } from "./userPermissions";

describe("userPermissions", () => {
  describe("isCurrentUser", () => {
    it("should return true when both IDs are identical", () => {
      expect(isCurrentUser("user-1", "user-1")).toBe(true);
    });

    it("should return false when the IDs are different", () => {
      expect(isCurrentUser("user-1", "user-2")).toBe(false);
    });

    it("should return false when viewedUserId is undefined", () => {
      expect(isCurrentUser(undefined, "user-1")).toBe(false);
    });

    it("should return false when currentUserId is undefined", () => {
      expect(isCurrentUser("user-1", undefined)).toBe(false);
    });

    it("should return false when both IDs are undefined", () => {
      expect(isCurrentUser(undefined, undefined)).toBe(false);
    });

    it("should return false when viewedUserId is an empty string", () => {
      expect(isCurrentUser("", "user-1")).toBe(false);
    });

    it("should return false when currentUserId is an empty string", () => {
      expect(isCurrentUser("user-1", "")).toBe(false);
    });

    it("should return false when comparison is case-sensitive and cases differ", () => {
      expect(isCurrentUser("User-1", "user-1")).toBe(false);
    });
  });

  describe("canPerformDestructiveAction", () => {
    it("should return false when trying to perform action on own account", () => {
      expect(canPerformDestructiveAction("user-1", "user-1")).toBe(false);
    });

    it("should return true when performing action on a different user", () => {
      expect(canPerformDestructiveAction("user-1", "user-2")).toBe(true);
    });

    it("should return true when viewedUserId is undefined", () => {
      expect(canPerformDestructiveAction(undefined, "user-1")).toBe(true);
    });

    it("should return true when currentUserId is undefined", () => {
      expect(canPerformDestructiveAction("user-1", undefined)).toBe(true);
    });

    it("should return true when both IDs are undefined", () => {
      expect(canPerformDestructiveAction(undefined, undefined)).toBe(true);
    });

    it("should return true when viewedUserId is an empty string", () => {
      expect(canPerformDestructiveAction("", "user-1")).toBe(true);
    });

    it("should return true when currentUserId is an empty string", () => {
      expect(canPerformDestructiveAction("user-1", "")).toBe(true);
    });
  });
});
