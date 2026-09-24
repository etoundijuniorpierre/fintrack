import type { FormInstance } from "antd";

// Reinitialise un formulaire Ant Design a ses valeurs initiales et efface les erreurs de validation.
export const resetFormState = <T extends object>(
  form: FormInstance<T>,
  initialValues?: Partial<T>,
): void => {
  if (initialValues) {
    form.setFieldsValue(
      initialValues as Parameters<FormInstance<T>["setFieldsValue"]>[0],
    );
  } else {
    form.resetFields();
  }

  const currentValues = form.getFieldsValue();
  if (currentValues) {
    const fieldsToReset = Object.keys(currentValues).map((key) => ({
      name: key,
      errors: [],
    }));

    form.setFields(
      fieldsToReset as unknown as Parameters<FormInstance<T>["setFields"]>[0],
    );
  }
};
