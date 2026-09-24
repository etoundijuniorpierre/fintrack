// Tests frontend : verifie le comportement de formulaire field.test.

import { type ComponentProps } from "react";
import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { Form, Input, ConfigProvider, App as AntdApp } from "antd";
import FormField from "./FormField";

// Prepare l'affichage lisible de formulaire field.test.
const renderField = (props: Partial<ComponentProps<typeof FormField>> = {}) =>
  render(
    <ConfigProvider>
      <AntdApp>
        <Form layout="vertical">
          <FormField name="field" {...props}>
            <Input data-testid="input" />
          </FormField>
        </Form>
      </AntdApp>
    </ConfigProvider>,
  );

describe("FormField", () => {
  it("should render the label and child control", () => {
    renderField({ label: "Incident Title" });
    expect(screen.getByText("Incident Title")).toBeInTheDocument();
    expect(screen.getByTestId("input")).toBeInTheDocument();
  });

  it("should show the required asterisk when required is true", () => {
    renderField({ label: "Title", required: true });

    expect(
      screen
        .getByText("Title")
        .closest(".ant-form-item-label")
        ?.querySelector(".ant-form-item-required"),
    ).toBeInTheDocument();
  });

  it("should show the required asterisk when a required rule is provided without the required prop", () => {
    renderField({
      label: "Title",
      rules: [{ required: true, message: "required" }],
    });

    expect(
      screen
        .getByText("Title")
        .closest(".ant-form-item-label")
        ?.querySelector(".ant-form-item-required"),
    ).toBeInTheDocument();
  });

  it("should display errorMessage and set error state", () => {
    renderField({ label: "Title", errorMessage: "Invalid value" });
    expect(screen.getByText("Invalid value")).toBeInTheDocument();
    expect(screen.getByText("Title").closest(".ant-form-item")).toHaveClass(
      "ant-form-item-has-error",
    );
  });

  it("should show errorMessage instead of help text when both are provided", () => {
    renderField({ help: "Hint text", errorMessage: "Error text" });
    expect(screen.getByText("Error text")).toBeInTheDocument();
    expect(screen.queryByText("Hint text")).not.toBeInTheDocument();
  });

  it("should display help text when no errorMessage is set", () => {
    renderField({ help: "Enter a descriptive title" });
    expect(screen.getByText("Enter a descriptive title")).toBeInTheDocument();
  });

  it("should apply width 100% when fullWidth is true", () => {
    const { container } = renderField({ fullWidth: true });
    expect(container.querySelector(".ant-form-item")).toHaveStyle({
      width: "100%",
    });
  });

  it("should not apply width style when fullWidth is false", () => {
    const { container } = renderField({ fullWidth: false });
    expect(container.querySelector(".ant-form-item")).not.toHaveStyle({
      width: "100%",
    });
  });

  it("should pre-fill the input with initialValue when provided", () => {
    renderField({ initialValue: "Pre-filled" });
    expect(screen.getByTestId("input")).toHaveValue("Pre-filled");
  });
});
