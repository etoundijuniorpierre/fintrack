// Tests frontend : valide la persistance des vues de portee autorisees.

import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { usePersistentScopeView } from "./usePersistentScopeView";
import type {
  ScopeView,
  ScopeViewOption,
} from "../../../utils/permissions/permissions";

describe("usePersistentScopeView", () => {
  const availableViews: ScopeViewOption[] = [
    { label: "Perso", value: "own" },
    { label: "Agence", value: "agency" },
  ];
  const fallbackOrder: ScopeView[] = ["agency", "own"];
  const storageKey = "test_view";

  beforeEach(() => {
    localStorage.clear();
  });

  it("should load the preferred view when storage is empty", () => {
    const { result } = renderHook(() =>
      usePersistentScopeView(storageKey, availableViews, fallbackOrder),
    );

    expect(result.current[0]).toBe("agency");
  });

  it("should restore the stored view when it is still allowed", () => {
    localStorage.setItem(storageKey, "own");

    const { result } = renderHook(() =>
      usePersistentScopeView(storageKey, availableViews, fallbackOrder),
    );

    expect(result.current[0]).toBe("own");
  });

  it("should fall back to the preferred view when the stored value is not allowed", () => {
    localStorage.setItem(storageKey, "all");

    const { result } = renderHook(() =>
      usePersistentScopeView(storageKey, availableViews, fallbackOrder),
    );

    expect(result.current[0]).toBe("agency");
  });

  it("should update localStorage when the view changes", () => {
    const { result } = renderHook(() =>
      usePersistentScopeView(storageKey, availableViews, fallbackOrder),
    );

    act(() => {
      result.current[1]("own");
    });

    expect(result.current[0]).toBe("own");
    expect(localStorage.getItem(storageKey)).toBe("own");
  });

  it("should fall back to an allowed view when available options change", async () => {
    let currentAvailable = [...availableViews];

    const { result, rerender } = renderHook(() =>
      usePersistentScopeView(storageKey, currentAvailable, fallbackOrder),
    );

    expect(result.current[0]).toBe("agency");

    act(() => {
      result.current[1]("own");
    });
    expect(result.current[0]).toBe("own");

    currentAvailable = [{ label: "Agence", value: "agency" }];
    rerender();

    await waitFor(() => expect(result.current[0]).toBe("agency"));
  });
});
