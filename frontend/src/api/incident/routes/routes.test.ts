// Tests frontend : verifie le comportement de routes.test.

import { describe, it, expect } from "vitest";
import { INCIDENT_ROUTES } from "./routes";
import { APP_ROUTES } from "../../../utils/constants";

describe("INCIDENT_ROUTES", () => {
  it("should equal APP_ROUTES.INCIDENTS for LIST", () => {
    expect(INCIDENT_ROUTES.LIST).toBe(APP_ROUTES.INCIDENTS);
  });

  it("should construct the create route correctly", () => {
    expect(INCIDENT_ROUTES.CREATE).toBe(`${APP_ROUTES.INCIDENTS}/create`);
  });

  it("should construct the view route with the provided id", () => {
    expect(INCIDENT_ROUTES.VIEW("incident-1")).toBe(
      `${APP_ROUTES.INCIDENTS}/incident-1`,
    );
  });
});
