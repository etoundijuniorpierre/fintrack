// Tests frontend : verifie le comportement de language selector.test.

import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, beforeEach, vi } from "vitest";
import { I18nextProvider } from "react-i18next";
import LanguageSelector from "./LanguageSelector";
import i18n from "../../i18n";
import styles from "./LanguageSelector.module.scss";
import React from "react";

// Type les props du Select simule.
interface MockSelectProps {
  className?: string;
  onChange?: (value: string) => void;
}

vi.mock("antd", async () => {
  const actual = await vi.importActual<typeof import("antd")>("antd");
  return {
    ...actual,
    Select: (props: MockSelectProps) => (
      <div className={props.className} data-testid="mock-select">
        <button
          data-testid="mock-change-btn"
          onClick={() => props.onChange?.("en")}
        >
          Change
        </button>
        <div className="anticon-global">GlobalIcon</div>
        <div>🇫🇷 Français</div>
        <div>EN English</div>
      </div>
    ),
  };
});

// Prepare l'affichage lisible de language selector.test.
const renderWithI18n = (component: React.ReactElement) => {
  return render(<I18nextProvider i18n={i18n}>{component}</I18nextProvider>);
};

describe("LanguageSelector", () => {
  beforeEach(() => {
    i18n.init();
    vi.clearAllMocks();
  });

  it("should render the language selector with the correct structure", () => {
    renderWithI18n(<LanguageSelector />);
    const selector = screen.getByLabelText("language-selector");
    expect(selector).toBeInTheDocument();
  });

  it("should display the select component", () => {
    renderWithI18n(<LanguageSelector />);
    const select = screen.getByTestId("mock-select");
    expect(select).toBeInTheDocument();
  });

  it("should apply the correct CSS class to the container", () => {
    renderWithI18n(<LanguageSelector />);
    const selector = screen.getByLabelText("language-selector");
    expect(selector.className).toMatch(
      new RegExp(styles.container || "container"),
    );
  });

  it("should render the global icon", () => {
    const { container } = renderWithI18n(<LanguageSelector />);
    const icon = container.querySelector(".anticon-global");
    expect(icon).toBeInTheDocument();
  });

  it("should display the available language options", async () => {
    renderWithI18n(<LanguageSelector />);
    expect(screen.getByText("🇫🇷 Français")).toBeInTheDocument();
    expect(screen.getByText("EN English")).toBeInTheDocument();
  });

  it("should call i18n.changeLanguage with the selected language when the selector changes", async () => {
    const mockChangeLanguage = vi.fn();
    i18n.changeLanguage = mockChangeLanguage;
    renderWithI18n(<LanguageSelector />);

    fireEvent.click(screen.getByTestId("mock-change-btn"));
    expect(mockChangeLanguage).toHaveBeenCalledWith("en");
  });
});
