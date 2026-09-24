import { screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { FieldGuideSection } from "./FieldGuideSection";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, opts?: { defaultValue?: string }) =>
      opts?.defaultValue ?? key,
  }),
}));

describe("FieldGuideSection", () => {
  it("renders the intro and list of fields", () => {
    renderWithProviders(<FieldGuideSection />);
    expect(screen.getByText("help.sections.fields.intro")).toBeInTheDocument();
    // Verify at least one field label and help text renders
    expect(screen.getByText("help.fields.title.label")).toBeInTheDocument();
    expect(screen.getByText("help.fields.title.help")).toBeInTheDocument();
    // Verify tags
    expect(
      screen.getAllByText("help.sections.fields.required").length,
    ).toBeGreaterThan(0);
  });
});
