// Tests : hook useResetTouchedOnNextTick (remise a zero differee au tick suivant).

import { renderHook, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { useResetTouchedOnNextTick } from "./useResetTouchedOnNextTick";

describe("useResetTouchedOnNextTick", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("should reset touched to false on the next tick", () => {
    const setIsTouched = vi.fn();
    renderHook(() => useResetTouchedOnNextTick(setIsTouched, [1]));

    expect(setIsTouched).not.toHaveBeenCalled();
    act(() => vi.runAllTimers());
    expect(setIsTouched).toHaveBeenCalledWith(false);
  });

  it("should reschedule the reset when a dependency changes", () => {
    const setIsTouched = vi.fn();
    const { rerender } = renderHook(
      ({ dep }) => useResetTouchedOnNextTick(setIsTouched, [dep]),
      { initialProps: { dep: 1 } },
    );
    act(() => vi.runAllTimers());
    expect(setIsTouched).toHaveBeenCalledTimes(1);

    rerender({ dep: 2 });
    act(() => vi.runAllTimers());
    expect(setIsTouched).toHaveBeenCalledTimes(2);
  });

  it("should cancel the pending timer on unmount (no call)", () => {
    const setIsTouched = vi.fn();
    const { unmount } = renderHook(() =>
      useResetTouchedOnNextTick(setIsTouched, [1]),
    );
    unmount();
    act(() => vi.runAllTimers());
    expect(setIsTouched).not.toHaveBeenCalled();
  });
});
