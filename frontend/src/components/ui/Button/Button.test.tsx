// Tests frontend : verifie le comportement de button.test.

import { type ComponentProps } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { ConfigProvider, App as AntdApp } from "antd";
import Button from "./Button";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

// Prepare l'affichage lisible de button.test.
const renderButton = (props: ComponentProps<typeof Button> = {}) =>
  render(
    <ConfigProvider>
      <AntdApp>
        <Button {...props}>{props.children ?? "Click me"}</Button>
      </AntdApp>
    </ConfigProvider>,
  );

describe("Button", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("rendering", () => {
    it("renders with default props", () => {
      renderButton();
      expect(
        screen.getByRole("button", { name: "Click me" }),
      ).toBeInTheDocument();
    });

    it("renders children correctly", () => {
      renderButton({ children: "Save Changes" });
      expect(screen.getByText("Save Changes")).toBeInTheDocument();
    });

    it("renders all variants without crashing", () => {
      const variants = [
        "primary",
        "secondary",
        "danger",
        "ghost",
        "text",
      ] as const;
      variants.forEach((variant) => {
        const { unmount } = renderButton({ variant, children: variant });
        expect(screen.getByText(variant)).toBeInTheDocument();
        unmount();
      });
    });

    it("renders all sizes without crashing", () => {
      const sizes = ["sm", "md", "lg"] as const;
      sizes.forEach((size) => {
        const { unmount } = renderButton({ size, children: size });
        expect(screen.getByText(size)).toBeInTheDocument();
        unmount();
      });
    });
  });

  describe("disabled state — P-1.A", () => {
    it("does not fire onClick when disabled", () => {
      const onClick = vi.fn();
      renderButton({ disabled: true, onClick });

      fireEvent.click(screen.getByRole("button"));

      expect(onClick).not.toHaveBeenCalled();
    });

    it("has aria-disabled attribute when disabled", () => {
      renderButton({ disabled: true });
      const button = screen.getByRole("button");
      expect(button).toHaveAttribute("aria-disabled", "true");
    });

    it("has disabled attribute when disabled", () => {
      renderButton({ disabled: true });
      expect(screen.getByRole("button")).toBeDisabled();
    });

    it("fires onClick when not disabled", () => {
      const onClick = vi.fn();
      renderButton({ disabled: false, onClick });

      fireEvent.click(screen.getByRole("button"));

      expect(onClick).toHaveBeenCalledTimes(1);
    });
  });

  describe("loading state", () => {
    it("does not fire onClick when loading", () => {
      const onClick = vi.fn();
      renderButton({ loading: true, onClick });

      fireEvent.click(screen.getByRole("button"));

      expect(onClick).not.toHaveBeenCalled();
    });

    it("is disabled when loading", () => {
      renderButton({ loading: true });
      expect(screen.getByRole("button")).toBeDisabled();
    });

    it("has aria-disabled when loading", () => {
      renderButton({ loading: true });
      expect(screen.getByRole("button")).toHaveAttribute(
        "aria-disabled",
        "true",
      );
    });

    it("shows spinner when loading", () => {
      renderButton({ loading: true });

      const button = screen.getByRole("button");
      expect(button).toBeInTheDocument();

      expect(button).toBeDisabled();
    });
  });

  describe("click behavior", () => {
    it("calls onClick handler when clicked", () => {
      const onClick = vi.fn();
      renderButton({ onClick });

      fireEvent.click(screen.getByRole("button"));

      expect(onClick).toHaveBeenCalledTimes(1);
    });

    it("does not throw when onClick is not provided", () => {
      renderButton();
      expect(() => fireEvent.click(screen.getByRole("button"))).not.toThrow();
    });
  });

  describe("htmlType", () => {
    it("renders as submit button when htmlType is submit", () => {
      renderButton({ htmlType: "submit" });
      expect(screen.getByRole("button")).toHaveAttribute("type", "submit");
    });

    it("renders as button by default", () => {
      renderButton();
      expect(screen.getByRole("button")).toHaveAttribute("type", "button");
    });
  });
});
