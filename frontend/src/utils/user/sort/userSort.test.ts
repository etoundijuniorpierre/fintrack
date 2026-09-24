// Tests frontend : verifie le comportement de userSort.

import { describe, it, expect } from "vitest";
import {
  resolveUserSortPreset,
  USER_SORT_PRESETS,
} from "./userSort";

describe("resolveUserSortPreset", () => {
  it("should resolve alphabetical preset", () => {
    expect(resolveUserSortPreset("lastName")).toBe("alphabetical");
  });

  it("should resolve seniority preset with newest records first", () => {
    expect(resolveUserSortPreset("createdAt")).toBe("seniority");
  });

  it("should return undefined for unknown field", () => {
    expect(resolveUserSortPreset("email")).toBeUndefined();
  });
});

describe("USER_SORT_PRESETS", () => {
  it("should expose only alphabetical and seniority shortcuts", () => {
    expect(Object.keys(USER_SORT_PRESETS)).toEqual([
      "alphabetical",
      "seniority",
    ]);
  });

  it("each preset should have a field and order", () => {
    for (const config of Object.values(USER_SORT_PRESETS)) {
      expect(config).toHaveProperty("field");
      expect(config).toHaveProperty("order");
      expect(["asc", "desc"]).toContain(config.order);
    }
  });
});
