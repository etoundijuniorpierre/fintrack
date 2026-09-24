// Tests frontend : vérifie le contrat de tri commun à tous les tableaux.

import { describe, expect, it } from "vitest";
import {
  compareTableDates,
  compareTableNumbers,
  compareTableText,
  createTableSortPresets,
  readTableSort,
  resolveSortMode,
  sortTableRecords,
  TABLE_SORT_DIRECTIONS,
  toTableSortOrder,
  type SortPresetConfig,
} from "./tableSorting";

describe("tableSorting", () => {
  const presets: Record<"alphabetical" | "seniority", SortPresetConfig> = {
    alphabetical: { field: "name", order: "asc" },
    seniority: { field: "createdAt", order: "desc" },
  };

  it("keeps alternating column sorting without returning to an unsorted state", () => {
    expect(TABLE_SORT_DIRECTIONS).toEqual(["ascend", "descend", "ascend"]);
  });

  it("resolves alphabetical and seniority shortcuts", () => {
    expect(resolveSortMode(presets, "name")).toBe("alphabetical");
    expect(resolveSortMode(presets, "createdAt")).toBe("seniority");
    expect(resolveSortMode(presets, "unknown")).toBeUndefined();
  });

  it("creates shortcuts with A-Z and newest-first defaults", () => {
    expect(createTableSortPresets("label", "createdAt")).toEqual({
      alphabetical: { field: "label", order: "asc" },
      seniority: { field: "createdAt", order: "desc" },
    });
  });

  it("reads the field and direction emitted by Ant Design", () => {
    expect(
      readTableSort({ field: "name", order: "descend" }),
    ).toEqual({ field: "name", order: "desc" });
    expect(toTableSortOrder("asc")).toBe("ascend");
  });

  it("compares text naturally regardless of casing and accents", () => {
    expect(compareTableText("Agence 2", "agence 10")).toBeLessThan(0);
    expect(compareTableText("Émile", "emile")).toBe(0);
  });

  it("compares dates and numbers", () => {
    expect(compareTableDates("2026-01-01", "2026-02-01")).toBeLessThan(0);
    expect(compareTableNumbers(20, 3)).toBeGreaterThan(0);
  });

  it("sorts the complete local collection in either direction", () => {
    const records = [
      { name: "Zeta", createdAt: "2026-01-01" },
      { name: "Alpha", createdAt: "2026-03-01" },
    ];

    expect(sortTableRecords(records, "name", "asc").map(({ name }) => name))
      .toEqual(["Alpha", "Zeta"]);
    expect(
      sortTableRecords(records, "createdAt", "desc").map(({ name }) => name),
    ).toEqual(["Alpha", "Zeta"]);
  });
});
