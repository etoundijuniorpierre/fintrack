// Tests frontend : vérifie le tri local partagé avant pagination.

import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { useClientTableSort } from "./useClientTableSort";

describe("useClientTableSort", () => {
  beforeEach(() => sessionStorage.clear());

  const records = [
    { name: "Zulu", createdAt: "2026-01-01" },
    { name: "Alpha", createdAt: "2026-02-01" },
  ];

  it("starts alphabetically and applies newest-first seniority", () => {
    const { result } = renderHook(() =>
      useClientTableSort("local_sort", records, "name", "createdAt"),
    );

    expect(result.current.data.map(({ name }) => name)).toEqual([
      "Alpha",
      "Zulu",
    ]);
    act(() => result.current.selectPreset("seniority"));
    expect(result.current.data.map(({ name }) => name)).toEqual([
      "Alpha",
      "Zulu",
    ]);
  });

  it("reverses the whole collection after a column click", () => {
    const { result } = renderHook(() =>
      useClientTableSort("local_sort", records, "name", "createdAt"),
    );

    act(() =>
      result.current.onChange(undefined, undefined, {
        field: "name",
        order: "descend",
      }),
    );
    expect(result.current.data.map(({ name }) => name)).toEqual([
      "Zulu",
      "Alpha",
    ]);
  });
});
