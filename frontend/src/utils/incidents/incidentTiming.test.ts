// Tests frontend : verifie le comportement de incident timing.test.

import { describe, it, expect } from "vitest";
import { getIncidentAgeInfo, getIncidentSlaInfo } from "./incidentTiming";
import { IncidentStatus } from "../../api/incident/types";

describe("incidentTiming", () => {
  describe("getIncidentAgeInfo", () => {
    const now = new Date("2024-01-15T12:00:00Z");

    it("should return null if createdAt is undefined", () => {
      expect(getIncidentAgeInfo(undefined, now)).toBeNull();
    });

    it("should return elapsed minutes if less than an hour has passed", () => {
      const createdAt = new Date("2024-01-15T11:45:00Z");
      expect(getIncidentAgeInfo(createdAt, now)).toEqual({
        value: 15,
        unit: "minutes",
      });

      const createdAtStr = "2024-01-15T11:30:00Z";
      expect(getIncidentAgeInfo(createdAtStr, now)).toEqual({
        value: 30,
        unit: "minutes",
      });
    });

    it("should return elapsed hours if less than a day has passed", () => {
      const createdAt = new Date("2024-01-15T08:00:00Z");
      expect(getIncidentAgeInfo(createdAt, now)).toEqual({
        value: 4,
        unit: "hours",
      });
    });

    it("should return elapsed days if more than a day has passed", () => {
      const createdAt = new Date("2024-01-13T12:00:00Z");
      expect(getIncidentAgeInfo(createdAt, now)).toEqual({
        value: 2,
        unit: "days",
      });
    });
  });

  describe("getIncidentSlaInfo", () => {
    const now = new Date(2024, 0, 15, 12, 0, 0);

    it("should return none state if incident has no dueDate", () => {
      expect(
        getIncidentSlaInfo(
          { status: IncidentStatus.OPEN, dueDate: undefined },
          now,
        ),
      ).toEqual({ state: "none" });
    });

    it("should return closed state when the incident is closed", () => {
      const futureDue = new Date(2024, 0, 16, 12, 0, 0).toISOString();
      expect(
        getIncidentSlaInfo(
          { status: IncidentStatus.CLOSED, dueDate: futureDue },
          now,
        ),
      ).toEqual({ state: "closed" });
    });

    it("should return stopped state for other terminal statuses", () => {
      const futureDue = new Date(2024, 0, 16, 12, 0, 0).toISOString();
      expect(
        getIncidentSlaInfo(
          { status: IncidentStatus.RESOLVED, dueDate: futureDue },
          now,
        ),
      ).toEqual({ state: "stopped" });
      expect(
        getIncidentSlaInfo(
          { status: IncidentStatus.TREATED, dueDate: futureDue },
          now,
        ),
      ).toEqual({ state: "stopped" });
      expect(
        getIncidentSlaInfo(
          { status: IncidentStatus.REJECTED, dueDate: futureDue },
          now,
        ),
      ).toEqual({ state: "stopped" });
    });

    it("should return overdue state if days remaining is negative", () => {
      const overdueDue = new Date(2024, 0, 14, 12, 0, 0).toISOString();
      expect(
        getIncidentSlaInfo(
          { status: IncidentStatus.OPEN, dueDate: overdueDue },
          now,
        ),
      ).toEqual({
        state: "overdue",
        daysRemaining: -1,
      });
    });

    it("should return dueToday state if due date is today", () => {
      const todayDue = new Date(2024, 0, 15, 18, 0, 0).toISOString();
      expect(
        getIncidentSlaInfo(
          { status: IncidentStatus.OPEN, dueDate: todayDue },
          now,
        ),
      ).toEqual({
        state: "dueToday",
        daysRemaining: 1,
      });
    });

    it("should keep the deadline running while awaiting Executive validation", () => {
      const overdueDue = new Date(2024, 0, 15, 11, 0, 0).toISOString();

      expect(
        getIncidentSlaInfo(
          { status: IncidentStatus.DRAFT, dueDate: overdueDue },
          now,
        ),
      ).toEqual({
        state: "overdue",
        daysRemaining: -1,
      });
    });

    it("should return dueSoon state if due date is in 1 or 2 days", () => {
      const soonDue = new Date(2024, 0, 16, 12, 0, 0).toISOString();
      expect(
        getIncidentSlaInfo(
          { status: IncidentStatus.OPEN, dueDate: soonDue },
          now,
        ),
      ).toEqual({
        state: "dueSoon",
        daysRemaining: 1,
      });
    });

    it("should return onTrack state if due date is in more than 2 days", () => {
      const onTrackDue = new Date(2024, 0, 19, 12, 0, 0).toISOString();
      expect(
        getIncidentSlaInfo(
          { status: IncidentStatus.OPEN, dueDate: onTrackDue },
          now,
        ),
      ).toEqual({
        state: "onTrack",
        daysRemaining: 4,
      });
    });
  });
});
