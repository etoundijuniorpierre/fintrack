// Tests : invariants structurels du contenu du centre d'aide.

import { describe, it, expect } from "vitest";
import { INCIDENT_FIELDS, TEXT_SECTIONS, FAQ_KEYS } from "./helpContent";

describe("INCIDENT_FIELDS", () => {
  it("should expose unique field keys", () => {
    const keys = INCIDENT_FIELDS.map((f) => f.key);
    expect(new Set(keys).size).toBe(keys.length);
  });

  it("should mark the core mandatory fields as required", () => {
    const required = INCIDENT_FIELDS.filter((f) => f.required).map(
      (f) => f.key,
    );
    expect(required).toEqual(
      expect.arrayContaining(["title", "description", "type", "criticality"]),
    );
  });
});

describe("TEXT_SECTIONS", () => {
  it("should contain no empty section", () => {
    for (const items of Object.values(TEXT_SECTIONS)) {
      expect(items.length).toBeGreaterThan(0);
    }
  });

  it("should expose unique paragraph keys within each section", () => {
    for (const items of Object.values(TEXT_SECTIONS)) {
      expect(new Set(items.map((i) => i.key)).size).toBe(items.length);
    }
  });

  it("should correctly expose requiredPermissions when present", () => {
    const editItem = TEXT_SECTIONS.managing.find((item) => item.key === "edit");
    expect(editItem?.requiredPermissions).toBeDefined();
    expect(editItem?.requiredPermissions?.length).toBeGreaterThan(0);
  });
});

describe("FAQ_KEYS", () => {
  it("should expose unique non-empty keys", () => {
    expect(FAQ_KEYS.length).toBeGreaterThan(0);
    expect(new Set(FAQ_KEYS.map((i) => i.key)).size).toBe(FAQ_KEYS.length);
  });
});
