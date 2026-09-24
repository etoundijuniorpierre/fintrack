// Tests frontend : verifie le comportement de back button.test.

import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { useNavigate } from "react-router-dom";
import BackButton from "./BackButton";

vi.mock("react-router-dom", () => ({
  useNavigate: vi.fn(),
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

describe("BackButton", () => {
  const mockNavigateFn = vi.fn();
  const mockUseNavigate = vi.mocked(useNavigate);

  beforeEach(() => {
    vi.clearAllMocks();
    mockUseNavigate.mockReturnValue(mockNavigateFn);
  });

  it("should display with correct role and label", () => {
    render(<BackButton />);

    const button = screen.getByRole("button", { name: "common.back" });
    expect(button).toBeInTheDocument();
  });

  it("should call navigate(-1) on click by default", () => {
    render(<BackButton />);

    const button = screen.getByRole("button", { name: "common.back" });
    fireEvent.click(button);

    expect(mockNavigateFn).toHaveBeenCalledWith(-1);
  });

  it("should call navigateTo if prop is provided", () => {
    render(<BackButton navigateTo="/dashboard/users" />);

    const button = screen.getByRole("button", { name: "common.back" });
    fireEvent.click(button);

    expect(mockNavigateFn).toHaveBeenCalledWith("/dashboard/users");
  });

  it("should prioritize onClick if provided", () => {
    const mockOnClick = vi.fn();

    render(<BackButton onClick={mockOnClick} />);

    const button = screen.getByRole("button", { name: "common.back" });
    fireEvent.click(button);

    expect(mockOnClick).toHaveBeenCalled();
    expect(mockNavigateFn).not.toHaveBeenCalled();
  });

  it("should prioritize onClick over navigateTo if both are provided", () => {
    const mockOnClick = vi.fn();

    render(<BackButton onClick={mockOnClick} navigateTo="/test" />);

    const button = screen.getByRole("button", { name: "common.back" });
    fireEvent.click(button);

    expect(mockOnClick).toHaveBeenCalled();
    expect(mockNavigateFn).not.toHaveBeenCalled();
  });

  it("should have correct Ant Design CSS classes applied", () => {
    render(<BackButton />);

    const button = screen.getByRole("button", { name: "common.back" });
    expect(button).toHaveClass("ant-btn-text");
  });
});
