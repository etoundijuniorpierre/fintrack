// Composant backend : porte la logique liee a system thresholds.

package com.fintrack.reporting.model.readmodel.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Porte la responsabilite applicative liee a systeme seuils.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemThresholds {

  private Long defaultSlaHours;
  private Long criticalIncidentHours;
  private Long slaReminderIntervalHours;
  private Long maxTransfersBeforeAlert;
  private Long notificationMaxRetryCount;
  private Long loginMaxFailedAttempts;
  private Long tempPasswordValidityMinutes;
  private Long escalationScanIntervalMinutes;
  private Long maxReopenCount;
  private Long reopenTimeLimitHours;
  private Long autoBlockOverdueWorkingDays;
  private Long blockedReminderIntervalDays;
  private Long criticalReminderIntervalHours;
  private Long prolongedWaitDays;
  private Long validationDelayHours;
  private Long validationReminderEnabled;
  private Long serviceManagerSelfValidationEnabled;
  private Long pendingActionReminderIntervalHours;
  private Long pendingActionInternalEnabled;
  private Long reportRetentionDays;
  private Long backupScheduleEnabled;
  private Long backupScheduleHour;
}
