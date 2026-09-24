// Tests frontend : verifie le comportement de ui store.test.

import { describe, it, expect, beforeEach } from "vitest";
import { useUIStore } from "./uiStore";

describe("uiStore", () => {
  beforeEach(() => {
    useUIStore.setState({ sidebarCollapsed: false });
  });

  describe("initial state", () => {
    it("should have sidebarCollapsed as false by default", () => {
      expect(useUIStore.getState().sidebarCollapsed).toBe(false);
    });
  });

  describe("setSidebarCollapsed", () => {
    it("should set sidebarCollapsed to true when true is provided", () => {
      useUIStore.getState().setSidebarCollapsed(true);
      expect(useUIStore.getState().sidebarCollapsed).toBe(true);
    });

    it("should set sidebarCollapsed to false when false is provided", () => {
      useUIStore.setState({ sidebarCollapsed: true });
      useUIStore.getState().setSidebarCollapsed(false);
      expect(useUIStore.getState().sidebarCollapsed).toBe(false);
    });
  });

  describe("toggleSidebar", () => {
    it("should set sidebarCollapsed to true when it was false", () => {
      useUIStore.getState().toggleSidebar();
      expect(useUIStore.getState().sidebarCollapsed).toBe(true);
    });

    it("should set sidebarCollapsed to false when it was true", () => {
      useUIStore.setState({ sidebarCollapsed: true });
      useUIStore.getState().toggleSidebar();
      expect(useUIStore.getState().sidebarCollapsed).toBe(false);
    });

    it("should restore the original value after two consecutive toggles", () => {
      const initial = useUIStore.getState().sidebarCollapsed;
      useUIStore.getState().toggleSidebar();
      useUIStore.getState().toggleSidebar();
      expect(useUIStore.getState().sidebarCollapsed).toBe(initial);
    });
  });
});
