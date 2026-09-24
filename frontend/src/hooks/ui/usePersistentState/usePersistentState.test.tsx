// Tests frontend : verifie le comportement de use persistent state.test.

import { renderHook, act } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { usePersistentState } from "./usePersistentState";

describe("usePersistentState", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("loads the default value when storage is empty", () => {
    const { result } = renderHook(() =>
      usePersistentState("display", "table", ["table", "cards"]),
    );

    expect(result.current[0]).toBe("table");
  });

  it("persists updates to localStorage", () => {
    const { result } = renderHook(() =>
      usePersistentState("display", "table", ["table", "cards"]),
    );

    act(() => result.current[1]("cards"));

    expect(localStorage.getItem("display")).toBe("cards");
  });

  it("ignores stored values outside the allowed set", () => {
    localStorage.setItem("display", "invalid");

    const { result } = renderHook(() =>
      usePersistentState("display", "table", ["table", "cards"]),
    );

    expect(result.current[0]).toBe("table");
  });
});
