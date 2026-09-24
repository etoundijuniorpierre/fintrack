// Tests : store de theme (etat initial, bascule, selection explicite).

import { describe, it, expect, beforeEach } from "vitest";
import { useThemeStore } from "./themeStore";

describe("useThemeStore", () => {
  beforeEach(() => {
    useThemeStore.setState({ mode: "dark" });
  });

  it("should start in dark mode by default", () => {
    expect(useThemeStore.getState().mode).toBe("dark");
  });

  it("should toggle dark -> light then light -> dark via toggleTheme", () => {
    useThemeStore.getState().toggleTheme();
    expect(useThemeStore.getState().mode).toBe("light");
    useThemeStore.getState().toggleTheme();
    expect(useThemeStore.getState().mode).toBe("dark");
  });

  it("should apply the requested mode via setTheme", () => {
    useThemeStore.getState().setTheme("light");
    expect(useThemeStore.getState().mode).toBe("light");
  });

  it("should persist the mode to localStorage", () => {
    useThemeStore.getState().setTheme("light");
    expect(localStorage.getItem("fintrack-theme-storage")).toContain("light");
  });
});
