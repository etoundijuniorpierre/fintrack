// Tests frontend : verifie le comportement de login.test.

import { screen } from "@testing-library/react";
import { describe, it, expect, beforeEach, vi } from "vitest";
import Login from "./Login";
import styles from "./Login.module.scss";
import { renderWithProviders } from "../../test-utils/renderWithProviders";

const TEST_SELECTORS = {
  LOGIN_BANNER: "login-banner",
  LOGIN_FORM: "login-form",
  LANGUAGE_SELECTOR: "language-selector",
} as const;

const CSS_CLASSES = {
  PAGE: styles.page,
  TOP_BAR: styles.topBar,
  CONTENT: styles.content,
} as const;

vi.mock("./components/Banner/LoginBanner", () => ({
  default: () => (
    <div data-testid={TEST_SELECTORS.LOGIN_BANNER}>LoginBanner</div>
  ),
}));

vi.mock("./components/Form/LoginForm/LoginForm", () => ({
  default: () => <div data-testid={TEST_SELECTORS.LOGIN_FORM}>LoginForm</div>,
}));

vi.mock("../../components/LanguageSelector/LanguageSelector", () => ({
  default: () => (
    <div data-testid={TEST_SELECTORS.LANGUAGE_SELECTOR}>LanguageSelector</div>
  ),
}));

describe("Login", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render the main element with the page class when mounted", () => {
    renderWithProviders(<Login />);

    const loginPage = screen.getByRole("main");
    expect(loginPage).toBeInTheDocument();
    expect(loginPage).toHaveClass(CSS_CLASSES.PAGE);
  });

  it("should render the top bar header with the language selector when mounted", () => {
    renderWithProviders(<Login />);

    const topBar = screen.getByRole("banner");
    expect(topBar).toBeInTheDocument();
    expect(topBar).toHaveClass(CSS_CLASSES.TOP_BAR);

    const languageSelector = screen.getByTestId(
      TEST_SELECTORS.LANGUAGE_SELECTOR,
    );
    expect(languageSelector).toBeInTheDocument();
    expect(topBar).toContainElement(languageSelector);
  });

  it("should render the content area containing the banner and form when mounted", () => {
    const { container } = renderWithProviders(<Login />);

    const content = container.querySelector(`.${CSS_CLASSES.CONTENT}`);
    expect(content).toBeInTheDocument();

    const banner = screen.getByTestId(TEST_SELECTORS.LOGIN_BANNER);
    const form = screen.getByTestId(TEST_SELECTORS.LOGIN_FORM);

    expect(banner).toBeInTheDocument();
    expect(form).toBeInTheDocument();
    expect(content).toContainElement(banner);
    expect(content).toContainElement(form);
  });

  it("should render with correct semantic HTML structure when mounted", () => {
    const { container } = renderWithProviders(<Login />);

    const main = container.querySelector("main");
    const header = container.querySelector("header");
    const content = container.querySelector(`.${CSS_CLASSES.CONTENT}`);

    expect(main).toBeInTheDocument();
    expect(header).toBeInTheDocument();
    expect(content).toBeInTheDocument();
  });

  it("should render all required child components when mounted", () => {
    renderWithProviders(<Login />);

    expect(screen.getByTestId(TEST_SELECTORS.LOGIN_BANNER)).toBeInTheDocument();
    expect(screen.getByTestId(TEST_SELECTORS.LOGIN_FORM)).toBeInTheDocument();
    expect(
      screen.getByTestId(TEST_SELECTORS.LANGUAGE_SELECTOR),
    ).toBeInTheDocument();
  });

  it("should apply correct CSS classes to all structural elements when mounted", () => {
    const { container } = renderWithProviders(<Login />);

    const main = container.querySelector("main");
    const header = container.querySelector("header");
    const content = container.querySelector(`.${CSS_CLASSES.CONTENT}`);

    expect(main).toHaveClass(CSS_CLASSES.PAGE);
    expect(header).toHaveClass(CSS_CLASSES.TOP_BAR);
    expect(content).toHaveClass(CSS_CLASSES.CONTENT);
  });

  it("should place child components inside their correct parent containers when mounted", () => {
    const { container } = renderWithProviders(<Login />);

    const header = container.querySelector("header");
    const content = container.querySelector(`.${CSS_CLASSES.CONTENT}`);

    expect(header).toContainElement(
      screen.getByTestId(TEST_SELECTORS.LANGUAGE_SELECTOR),
    );
    expect(content).toContainElement(
      screen.getByTestId(TEST_SELECTORS.LOGIN_BANNER),
    );
    expect(content).toContainElement(
      screen.getByTestId(TEST_SELECTORS.LOGIN_FORM),
    );
  });

  it("should expose accessible landmark roles when mounted", () => {
    renderWithProviders(<Login />);

    expect(screen.getByRole("main")).toBeInTheDocument();
    expect(screen.getByRole("banner")).toBeInTheDocument();
  });
});
