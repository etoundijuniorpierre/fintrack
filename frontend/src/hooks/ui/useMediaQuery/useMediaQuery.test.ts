// Tests : hook useMediaQuery (etat initial, reaction au changement, nettoyage).

import { renderHook, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import useMediaQuery from "./useMediaQuery";

type ChangeHandler = (event: MediaQueryListEvent) => void;

const originalMatchMedia = window.matchMedia;

// Fabrique une MediaQueryList controlable qui capture le handler 'change'.
const installMatchMedia = (initialMatches: boolean) => {
  let handler: ChangeHandler | null = null;
  const removeEventListener = vi.fn();
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: initialMatches,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn((_: string, cb: ChangeHandler) => {
      handler = cb;
    }),
    removeEventListener,
    dispatchEvent: vi.fn(),
  })) as typeof window.matchMedia;
  return {
    emitChange: (matches: boolean) =>
      handler?.({ matches } as MediaQueryListEvent),
    removeEventListener,
  };
};

describe("useMediaQuery", () => {
  beforeEach(() => vi.clearAllMocks());
  afterEach(() => {
    window.matchMedia = originalMatchMedia;
  });

  it("should return the initial state of the media query", () => {
    installMatchMedia(true);
    const { result } = renderHook(() => useMediaQuery("(max-width: 768px)"));
    expect(result.current).toBe(true);
  });

  it("should return false when the media query does not match", () => {
    installMatchMedia(false);
    const { result } = renderHook(() => useMediaQuery("(max-width: 768px)"));
    expect(result.current).toBe(false);
  });

  it("should update when a change event fires", () => {
    const mql = installMatchMedia(false);
    const { result } = renderHook(() => useMediaQuery("(max-width: 768px)"));
    expect(result.current).toBe(false);

    act(() => mql.emitChange(true));
    expect(result.current).toBe(true);
  });

  it("should remove the listener on unmount", () => {
    const mql = installMatchMedia(false);
    const { unmount } = renderHook(() => useMediaQuery("(max-width: 768px)"));
    unmount();
    expect(mql.removeEventListener).toHaveBeenCalled();
  });
});
