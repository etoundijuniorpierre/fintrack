import { describe, it, expect, vi, beforeEach } from "vitest";
import type { FormInstance } from "antd";
import { resetFormState } from "./formReset";
import type { Mocked } from "vitest";

describe("resetFormState", () => {
  let mockForm: Mocked<FormInstance<Record<string, unknown>>>;

  beforeEach(() => {
    mockForm = {
      setFieldsValue: vi.fn(),
      resetFields: vi.fn(),
      getFieldsValue: vi.fn(),
      setFields: vi.fn(),
    } as unknown as Mocked<FormInstance<Record<string, unknown>>>;
  });

  it("should call resetFields when initialValues is not provided", () => {
    mockForm.getFieldsValue.mockReturnValue({});

    resetFormState(mockForm);

    expect(mockForm.resetFields).toHaveBeenCalledTimes(1);
    expect(mockForm.setFieldsValue).not.toHaveBeenCalled();
  });

  it("should call setFieldsValue when initialValues is provided", () => {
    const initialValues = { testField: "value" };
    mockForm.getFieldsValue.mockReturnValue({});

    resetFormState(mockForm, initialValues);

    expect(mockForm.setFieldsValue).toHaveBeenCalledWith(initialValues);
    expect(mockForm.resetFields).not.toHaveBeenCalled();
  });

  it("should clear validation errors for all current fields", () => {
    const currentValues = { field1: "value1", field2: "value2" };
    mockForm.getFieldsValue.mockReturnValue(currentValues);

    resetFormState(mockForm);

    expect(mockForm.setFields).toHaveBeenCalledWith([
      { name: "field1", errors: [] },
      { name: "field2", errors: [] },
    ]);
  });
});
