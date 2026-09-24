// Tests frontend : verifie le comportement de active status field.test.

import { describe, it, expect } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { Form } from "antd";
import { useEffect } from "react";
import { useActiveStatusField } from "./activeStatusField";

// Rend le composant TestComponent pour l'interface active status field.test.
const TestComponent = ({
  initialValue,
  fallback,
}: {
  initialValue?: boolean;
  fallback?: boolean;
}) => {
  const [form] = Form.useForm();
  const value = useActiveStatusField(form, fallback);

  useEffect(() => {
    if (initialValue !== undefined) {
      form.setFieldValue("isActive", initialValue);
    }
  }, [initialValue, form]);

  return (
    <Form form={form}>
      <Form.Item name="isActive">
        <span />
      </Form.Item>
      <span data-testid="status-val">{String(value)}</span>
    </Form>
  );
};

describe("useActiveStatusField", () => {
  it("should return default fallback (true) when field is not set", () => {
    render(<TestComponent />);
    expect(screen.getByTestId("status-val").textContent).toBe("true");
  });

  it("should return explicit fallback when field is not set", () => {
    render(<TestComponent fallback={false} />);
    expect(screen.getByTestId("status-val").textContent).toBe("false");
  });

  it("should return field value when set to false", async () => {
    render(<TestComponent initialValue={false} />);
    await waitFor(() => {
      expect(screen.getByTestId("status-val").textContent).toBe("false");
    });
  });

  it("should return field value when set to true", async () => {
    render(<TestComponent initialValue={true} />);
    await waitFor(() => {
      expect(screen.getByTestId("status-val").textContent).toBe("true");
    });
  });
});
