import { render, screen, fireEvent, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { DebouncedSearchInput } from "./DebouncedSearchInput";

describe("DebouncedSearchInput", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  it("renders with placeholder", () => {
    render(<DebouncedSearchInput onSearch={vi.fn()} placeholder="Custom search..." />);
    expect(screen.getByPlaceholderText("Custom search...")).toBeInTheDocument();
  });

  it("initializes with initialValue and calls onSearch after delay", () => {
    const onSearch = vi.fn();
    render(<DebouncedSearchInput onSearch={onSearch} initialValue="test" delay={500} />);
    expect(screen.getByRole("textbox")).toHaveValue("test");
    act(() => vi.advanceTimersByTime(500));
    expect(onSearch).toHaveBeenCalledWith("test");
    expect(onSearch).toHaveBeenCalledTimes(1);
  });

  it("debounces input changes", () => {
    const onSearch = vi.fn();
    render(<DebouncedSearchInput onSearch={onSearch} delay={300} />);
    const input = screen.getByRole("textbox");
    
    fireEvent.change(input, { target: { value: "a" } });
    fireEvent.change(input, { target: { value: "ab" } });
    fireEvent.change(input, { target: { value: "abc" } });
    
    act(() => vi.advanceTimersByTime(200));
    expect(onSearch).not.toHaveBeenCalled();
    
    act(() => vi.advanceTimersByTime(100));
    expect(onSearch).toHaveBeenCalledWith("abc");
    expect(onSearch).toHaveBeenCalledTimes(1);
  });

  it("calls onSearch when input is cleared", () => {
    const onSearch = vi.fn();
    render(<DebouncedSearchInput onSearch={onSearch} initialValue="initial" delay={500} />);
    const input = screen.getByRole("textbox");
    
    fireEvent.change(input, { target: { value: "" } });
    act(() => vi.advanceTimersByTime(500));
    expect(onSearch).toHaveBeenCalledWith("");
  });
});
