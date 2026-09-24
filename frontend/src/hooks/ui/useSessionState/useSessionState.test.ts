import { renderHook, act } from "@testing-library/react";
import { useSessionState } from "./useSessionState";
import { describe, beforeEach, vi, it, expect } from "vitest";

describe("useSessionState", () => {
  const TEST_KEY = "test_session_key";

  beforeEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  it("should return initial value when session storage is empty", () => {
    const { result } = renderHook(() => useSessionState(TEST_KEY, "initial"));
    expect(result.current[0]).toBe("initial");
  });

  it("should initialize with function if provided", () => {
    const { result } = renderHook(() =>
      useSessionState(TEST_KEY, () => "initial_from_fn"),
    );
    expect(result.current[0]).toBe("initial_from_fn");
  });

  it("should read from session storage if value exists", () => {
    sessionStorage.setItem(TEST_KEY, JSON.stringify("stored_value"));
    const { result } = renderHook(() => useSessionState(TEST_KEY, "initial"));
    expect(result.current[0]).toBe("stored_value");
  });

  it("should update state and session storage", () => {
    const { result } = renderHook(() => useSessionState(TEST_KEY, "initial"));

    act(() => {
      result.current[1]("new_value");
    });

    expect(result.current[0]).toBe("new_value");
    expect(sessionStorage.getItem(TEST_KEY)).toBe(JSON.stringify("new_value"));
  });

  it("should support function updates", () => {
    const { result } = renderHook(() => useSessionState<number>(TEST_KEY, 0));

    act(() => {
      result.current[1]((prev) => prev + 1);
    });

    expect(result.current[0]).toBe(1);
    expect(sessionStorage.getItem(TEST_KEY)).toBe(JSON.stringify(1));
  });

  it("should handle JSON parse errors gracefully and fallback to initial value", () => {
    sessionStorage.setItem(TEST_KEY, "{ invalid json");
    const consoleSpy = vi.spyOn(console, "warn").mockImplementation(() => {});

    const { result } = renderHook(() => useSessionState(TEST_KEY, "fallback"));

    expect(result.current[0]).toBe("fallback");
    expect(consoleSpy).toHaveBeenCalled();

    consoleSpy.mockRestore();
  });
});
