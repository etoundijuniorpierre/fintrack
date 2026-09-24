// Tests frontend : verifie le comportement de tableau toolbar.test.

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { buildTableToolbar } from "./tableToolbar";
import { act } from "@testing-library/react";

describe("buildTableToolbar", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("should render input search and optional elements", () => {
    const handleSearch = vi.fn();
    const addButton = <button data-testid="add-btn">Add User</button>;
    const extraElement = <span data-testid="extra-el">Filter by status</span>;
    const leadingElement = <button data-testid="leading-el">Sort</button>;

    render(
      <div>
        {buildTableToolbar({
          searchValue: "john",
          onSearch: handleSearch,
          searchPlaceholder: "Search users...",
          leading: leadingElement,
          addButton,
          extra: extraElement,
        })}
      </div>,
    );

    const searchInput = screen.getByPlaceholderText(
      "Search users...",
    ) as HTMLInputElement;
    expect(searchInput).toBeInTheDocument();
    expect(searchInput.value).toBe("john");

    fireEvent.change(searchInput, { target: { value: "johndoe" } });

    act(() => {
      vi.advanceTimersByTime(500);
    });

    expect(handleSearch).toHaveBeenCalled();

    expect(screen.getByTestId("extra-el")).toBeInTheDocument();
    expect(screen.getByTestId("add-btn")).toBeInTheDocument();
    expect(screen.getByTestId("leading-el")).toBeInTheDocument();
  });

  it('should use default placeholder "Search…" when not provided', () => {
    render(
      <div>
        {buildTableToolbar({
          searchValue: "",
          onSearch: vi.fn(),
        })}
      </div>,
    );

    expect(screen.getByPlaceholderText("Search…")).toBeInTheDocument();
  });
});
