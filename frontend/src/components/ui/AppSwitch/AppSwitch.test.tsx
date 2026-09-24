// Tests frontend : verifie le comportement de app switch.test.

import { screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { Form } from "antd";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import AppSwitch from "./AppSwitch";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) =>
      ({
        "common.switch.on": "Active",
        "common.switch.off": "Coupe",
      })[key] ?? key,
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

describe("AppSwitch", () => {
  it("should expose the default on/off state labels", () => {
    renderWithProviders(<AppSwitch defaultChecked />);
    expect(screen.getByText("Active")).toBeInTheDocument();
    expect(screen.getByText("Coupe")).toBeInTheDocument();
  });

  it("should let the caller override the state labels", () => {
    renderWithProviders(
      <AppSwitch defaultChecked checkedChildren="ON" unCheckedChildren="OFF" />,
    );
    expect(screen.getByText("ON")).toBeInTheDocument();
    expect(screen.queryByText("Active")).not.toBeInTheDocument();
  });

  it("should forward checked and onChange when driven by a Form.Item", () => {
    const handleChange = vi.fn();
    renderWithProviders(
      <Form>
        <Form.Item name="enabled" valuePropName="checked" noStyle>
          <AppSwitch onChange={handleChange} />
        </Form.Item>
      </Form>,
    );
    fireEvent.click(screen.getByRole("switch"));
    expect(handleChange).toHaveBeenCalledWith(true, expect.anything());
  });
});
