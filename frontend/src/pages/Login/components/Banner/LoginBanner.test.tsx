// Tests frontend : verifie le comportement de connexion banner.test.

import { screen } from "@testing-library/react";
import { describe, it, expect, beforeEach, vi } from "vitest";
import LoginBanner from "./LoginBanner";
import styles from "./LoginBanner.module.scss";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";

vi.mock("/Img/loginIMG.png", () => "test-image-stub");

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
  I18nextProvider: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

describe("LoginBanner", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render the banner with correct semantic structure when mounted", () => {
    renderWithProviders(<LoginBanner />);

    const banner = screen.getByRole("complementary", { name: "login.title" });
    expect(banner).toBeInTheDocument();
  });

  it("should display the FinTrack logo when mounted", () => {
    renderWithProviders(<LoginBanner />);

    const image = screen.getByAltText("FinTrack");
    expect(image).toBeInTheDocument();
    expect(image).toHaveAttribute("src", "/Img/logo-dark.svg");
  });

  it("should render the geometric motif with aria-hidden when mounted", () => {
    const { container } = renderWithProviders(<LoginBanner />);

    const motif = container.querySelector(`.${styles.motif}`);
    expect(motif).toBeInTheDocument();
    expect(motif).toHaveAttribute("aria-hidden", "true");
  });

  it("should display the tagline heading with highlight when mounted", () => {
    renderWithProviders(<LoginBanner />);

    const tagline = screen.getByRole("heading", { level: 1 });
    expect(tagline).toBeInTheDocument();
    expect(tagline).toHaveTextContent("login.banner.tagline");
    expect(tagline).toHaveTextContent("login.banner.tagline_highlight");
  });

  it("should render the description text when mounted", () => {
    renderWithProviders(<LoginBanner />);

    expect(screen.getByText("login.banner.description")).toBeInTheDocument();
  });

  it("should display the feature list with correct accessibility label when mounted", () => {
    renderWithProviders(<LoginBanner />);

    const featureList = screen.getByRole("list", {
      name: "login.banner.features_label",
    });
    expect(featureList).toBeInTheDocument();
  });

  it("should render three feature items when mounted", () => {
    renderWithProviders(<LoginBanner />);

    const featureItems = screen.getAllByRole("listitem");
    expect(featureItems).toHaveLength(3);
  });

  it("should render each feature item with an aria-hidden icon when mounted", () => {
    const { container } = renderWithProviders(<LoginBanner />);
    const featureItems = container.querySelectorAll("li");

    featureItems.forEach((item) => {
      expect(item).toBeInTheDocument();
      const icon = item.querySelector(`.${styles.featureIcon}`);
      expect(icon).toBeInTheDocument();
      expect(icon).toHaveAttribute("aria-hidden", "true");
    });
  });

  it("should apply the correct CSS class structure when mounted", () => {
    const { container } = renderWithProviders(<LoginBanner />);

    expect(container.querySelector(`.${styles.banner}`)).toBeInTheDocument();
    expect(container.querySelector(`.${styles.content}`)).toBeInTheDocument();
    expect(container.querySelector(`.${styles.tagline}`)).toBeInTheDocument();
  });
});
