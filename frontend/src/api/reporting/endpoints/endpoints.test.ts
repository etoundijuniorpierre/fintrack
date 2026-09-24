// Tests frontend : verifie le comportement de endpoints.test.

import { describe, expect, it } from "vitest";
import { REPORTING_ENDPOINTS } from "./endpoints";

describe("REPORTING_ENDPOINTS", () => {
  it("builds report endpoints", () => {
    expect(REPORTING_ENDPOINTS.REPORTS.BASE).toBe(
      "/api/v1/reportingService/reports",
    );
    expect(REPORTING_ENDPOINTS.REPORTS.GENERATE).toBe(
      "/api/v1/reportingService/reports/generate",
    );
    expect(REPORTING_ENDPOINTS.REPORTS.DOWNLOAD("report-1")).toBe(
      "/api/v1/reportingService/reports/report-1/download",
    );
    expect(REPORTING_ENDPOINTS.REPORTS.SEND_EMAIL("report-1")).toBe(
      "/api/v1/reportingService/reports/report-1/send-email",
    );
  });
});
