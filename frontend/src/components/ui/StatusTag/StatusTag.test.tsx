// Tests frontend : verifie le comportement de status tag.test.

import { screen } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import StatusTag from "./StatusTag";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

const STATUS_COLORS: Record<string, string> = {
  OPEN: "blue",
  RESOLVED: "green",
  PENDING: "orange",
  CLOSED: "default",
  REJECTED: "red",
};

const CRITICALITY_COLORS: Record<string, string> = {
  LOW: "green",
  MEDIUM: "orange",
  HIGH: "red",
  CRITICAL: "purple",
};

describe("StatusTag", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("color mapping", () => {
    it("should render with blue color when status is OPEN", () => {
      renderWithProviders(<StatusTag status="OPEN" colorMap={STATUS_COLORS} />);
      const tag = screen.getByText("OPEN").closest(".ant-tag") as HTMLElement;
      expect(tag).toBeInTheDocument();
      expect(tag.className + (tag.getAttribute("style") ?? "")).toContain(
        "blue",
      );
    });

    it("should render with green color when status is RESOLVED", () => {
      renderWithProviders(
        <StatusTag status="RESOLVED" colorMap={STATUS_COLORS} />,
      );
      const tag = screen
        .getByText("RESOLVED")
        .closest(".ant-tag") as HTMLElement;
      expect(tag.className + (tag.getAttribute("style") ?? "")).toContain(
        "green",
      );
    });

    it("should render with orange color when status is PENDING", () => {
      renderWithProviders(
        <StatusTag status="PENDING" colorMap={STATUS_COLORS} />,
      );
      const tag = screen
        .getByText("PENDING")
        .closest(".ant-tag") as HTMLElement;
      expect(tag.className + (tag.getAttribute("style") ?? "")).toContain(
        "orange",
      );
    });

    it("should render with red color when status is REJECTED", () => {
      renderWithProviders(
        <StatusTag status="REJECTED" colorMap={STATUS_COLORS} />,
      );
      const tag = screen
        .getByText("REJECTED")
        .closest(".ant-tag") as HTMLElement;
      expect(tag.className + (tag.getAttribute("style") ?? "")).toContain(
        "red",
      );
    });

    it("should render with green color when criticality is LOW", () => {
      renderWithProviders(
        <StatusTag status="LOW" colorMap={CRITICALITY_COLORS} />,
      );
      const tag = screen.getByText("LOW").closest(".ant-tag") as HTMLElement;
      expect(tag.className + (tag.getAttribute("style") ?? "")).toContain(
        "green",
      );
    });

    it("should render with orange color when criticality is MEDIUM", () => {
      renderWithProviders(
        <StatusTag status="MEDIUM" colorMap={CRITICALITY_COLORS} />,
      );
      const tag = screen.getByText("MEDIUM").closest(".ant-tag") as HTMLElement;
      expect(tag.className + (tag.getAttribute("style") ?? "")).toContain(
        "orange",
      );
    });

    it("should render with red color when criticality is HIGH", () => {
      renderWithProviders(
        <StatusTag status="HIGH" colorMap={CRITICALITY_COLORS} />,
      );
      const tag = screen.getByText("HIGH").closest(".ant-tag") as HTMLElement;
      expect(tag.className + (tag.getAttribute("style") ?? "")).toContain(
        "red",
      );
    });

    it("should render with purple color when criticality is CRITICAL", () => {
      renderWithProviders(
        <StatusTag status="CRITICAL" colorMap={CRITICALITY_COLORS} />,
      );
      const tag = screen
        .getByText("CRITICAL")
        .closest(".ant-tag") as HTMLElement;
      expect(tag.className + (tag.getAttribute("style") ?? "")).toContain(
        "purple",
      );
    });

    it("should render without crashing when status is not in colorMap", () => {
      renderWithProviders(
        <StatusTag status="UNKNOWN_STATUS" colorMap={STATUS_COLORS} />,
      );
      const tag = screen
        .getByText("UNKNOWN_STATUS")
        .closest(".ant-tag") as HTMLElement;
      expect(tag).toBeInTheDocument();
    });
  });

  describe("label display", () => {
    it("should display the status value as label when no custom label is provided", () => {
      renderWithProviders(<StatusTag status="OPEN" colorMap={STATUS_COLORS} />);
      expect(screen.getByText("OPEN")).toBeInTheDocument();
    });

    it("should display the custom label when label prop is provided", () => {
      renderWithProviders(
        <StatusTag
          status="OPEN"
          colorMap={STATUS_COLORS}
          label="Open Incident"
        />,
      );
      expect(screen.getByText("Open Incident")).toBeInTheDocument();
      expect(screen.queryByText("OPEN")).not.toBeInTheDocument();
    });
  });

  describe("color map completeness", () => {
    it("should render a tag for every status in the colorMap", () => {
      Object.keys(STATUS_COLORS).forEach((status) => {
        const { unmount } = renderWithProviders(
          <StatusTag status={status} colorMap={STATUS_COLORS} />,
        );
        expect(screen.getByText(status)).toBeInTheDocument();
        unmount();
      });
    });

    it("should map each status to the exact color defined in colorMap", () => {
      Object.entries(STATUS_COLORS).forEach(([status, expectedColor]) => {
        const { unmount } = renderWithProviders(
          <StatusTag status={status} colorMap={STATUS_COLORS} />,
        );
        const tag = screen.getByText(status).closest(".ant-tag") as HTMLElement;
        expect(tag.className + (tag.getAttribute("style") ?? "")).toContain(
          expectedColor,
        );
        unmount();
      });
    });
  });
});
