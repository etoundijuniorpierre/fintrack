import React from "react";
import { render, type RenderOptions } from "@testing-library/react";
import { AllProviders } from "./AllProviders";

export const renderWithProviders = (
  ui: React.ReactElement,
  options?: Omit<RenderOptions, "wrapper">,
) => render(ui, { wrapper: AllProviders, ...options });
