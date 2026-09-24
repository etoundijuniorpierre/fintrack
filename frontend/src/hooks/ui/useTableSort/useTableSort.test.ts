// Teste le tri partage : presets, clics de colonne et resolution du preset actif.

import { describe, it, expect, beforeEach } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { useTableSort } from "./useTableSort";
import {
  USER_SORT_PRESETS,
  type UserSortPreset,
} from "../../../utils/user/sort/userSort";

describe("useTableSort", () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it("should start on the default preset", () => {
    const { result } = renderHook(() =>
      useTableSort<UserSortPreset>("test_sort", USER_SORT_PRESETS, "alphabetical"),
    );

    expect(result.current.sortParam).toBe("lastName,asc");
    expect(result.current.activePreset).toBe("alphabetical");
  });

  it("should apply a selected preset and resolve it as active", () => {
    const { result } = renderHook(() =>
      useTableSort<UserSortPreset>("test_sort", USER_SORT_PRESETS, "alphabetical"),
    );

    act(() => result.current.selectPreset("seniority"));

    expect(result.current.sortParam).toBe("createdAt,desc");
    expect(result.current.activePreset).toBe("seniority");
  });

  it("should follow the clicked column direction and deselect unmatched presets", () => {
    const { result } = renderHook(() =>
      useTableSort<UserSortPreset>("test_sort", USER_SORT_PRESETS, "alphabetical"),
    );

    act(() => result.current.handleColumnSortChange("email", "desc"));

    expect(result.current.sortParam).toBe("email,desc");
    // Aucun preset ne correspond a email desc : le menu doit se deselectionner.
    expect(result.current.activePreset).toBeUndefined();
  });

  it("should reselect a preset when a column click matches its configuration", () => {
    const { result } = renderHook(() =>
      useTableSort<UserSortPreset>("test_sort", USER_SORT_PRESETS, "seniority"),
    );

    act(() => result.current.handleColumnSortChange("lastName", "asc"));

    expect(result.current.activePreset).toBe("alphabetical");
  });

  it("should keep the mode selected when a column reverses its direction", () => {
    const { result } = renderHook(() =>
      useTableSort<UserSortPreset>("test_sort", USER_SORT_PRESETS, "alphabetical"),
    );

    act(() => result.current.handleColumnSortChange("lastName", "desc"));

    expect(result.current.activePreset).toBe("alphabetical");
    expect(result.current.sortParam).toBe("lastName,desc");
  });

  it("should ignore incomplete column changes", () => {
    const { result } = renderHook(() =>
      useTableSort<UserSortPreset>("test_sort", USER_SORT_PRESETS, "alphabetical"),
    );

    act(() => result.current.handleColumnSortChange("email", undefined));

    expect(result.current.sortParam).toBe("lastName,asc");
  });

  it("should recover from a legacy session shape", () => {
    sessionStorage.setItem(
      "test_sort",
      JSON.stringify({ preset: "seniority", field: "createdAt" }),
    );

    const { result } = renderHook(() =>
      useTableSort<UserSortPreset>("test_sort", USER_SORT_PRESETS, "alphabetical"),
    );

    // Champ conserve, direction retombee sur le defaut faute d'ordre stocke.
    expect(result.current.sortParam).toBe("createdAt,asc");
  });
});
