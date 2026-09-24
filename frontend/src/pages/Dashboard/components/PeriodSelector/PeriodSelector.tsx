// Selecteur de periode : periodes predefinies ou plage personnalisee avec validation des dates.
import { memo, useCallback, useMemo, useState } from "react";
import { Select, DatePicker, Typography } from "antd";
import { useTranslation } from "react-i18next";
import dayjs, { type Dayjs } from "dayjs";
import { PeriodType, type PeriodFilter } from "../../../../types/dashboard";
import {
  getImmediateFilter,
  validateCustomRange,
} from "../../../../utils/dashboard/periodSelector/periodSelectorUtils";
import styles from "./PeriodSelector.module.scss";
import { DATE_FORMATS } from "../../../../utils/date/dateUtils";

const { Text } = Typography;

// Centralise la logique d'interface liee a period selector props.
export interface PeriodSelectorProps {
  value: PeriodFilter;
  onChange: (filter: PeriodFilter) => void;
}

// Rend le composant PeriodSelector pour l'interface periode selecteur.
const PeriodSelector = memo(({ value, onChange }: PeriodSelectorProps) => {
  const { t } = useTranslation();

  const [dateFrom, setDateFrom] = useState<Dayjs | null>(
    value.dateFrom ? dayjs(value.dateFrom) : null,
  );
  const [dateTo, setDateTo] = useState<Dayjs | null>(
    value.dateTo ? dayjs(value.dateTo) : null,
  );
  const [dateError, setDateError] = useState<string | null>(null);

  const isCustom = value.period === PeriodType.CUSTOM;

  const periodOptions = useMemo(
    () => [
      { label: t("dashboard.period.today"), value: PeriodType.TODAY },
      { label: t("dashboard.period.last7Days"), value: PeriodType.LAST_7_DAYS },
      {
        label: t("dashboard.period.last30Days"),
        value: PeriodType.LAST_30_DAYS,
      },
      { label: t("dashboard.period.thisMonth"), value: PeriodType.THIS_MONTH },
      {
        label: t("dashboard.period.last365Days"),
        value: PeriodType.LAST_365_DAYS,
      },
      { label: t("dashboard.period.lastMonth"), value: PeriodType.LAST_MONTH },
      {
        label: t("dashboard.period.last2Months"),
        value: PeriodType.LAST_2_MONTHS,
      },
      {
        label: t("dashboard.period.last6Months"),
        value: PeriodType.LAST_6_MONTHS,
      },
      { label: t("dashboard.period.custom"), value: PeriodType.CUSTOM },
      { label: t("dashboard.period.all"), value: PeriodType.ALL },
    ],
    [t],
  );

  // Emet immediatement le filtre pour une periode predefinie, sinon passe en mode personnalise.
  const handlePeriodChange = useCallback(
    (period: PeriodType) => {
      setDateError(null);
      if (period !== PeriodType.CUSTOM) {
        setDateFrom(null);
        setDateTo(null);
        const filter = getImmediateFilter(period);
        if (filter) onChange(filter);
      } else {
        onChange({ period: PeriodType.CUSTOM });
      }
    },
    [onChange],
  );

  // Valide la plage personnalisee et emet le filtre, ou affiche une erreur.
  const validateAndEmit = useCallback(
    (from: Dayjs | null, to: Dayjs | null) => {
      const filter = validateCustomRange(from, to);
      if (!filter) {
        if (!from || !to) {
          setDateError(t("dashboard.period.dateRequired"));
        } else {
          setDateError(t("dashboard.period.dateInvalid"));
        }
        return;
      }
      setDateError(null);
      onChange(filter);
    },
    [onChange, t],
  );

  // Traite le changement de date de debut.
  const handleDateFromChange = useCallback(
    (date: Dayjs | null) => {
      setDateFrom(date);
      validateAndEmit(date, dateTo);
    },
    [dateTo, validateAndEmit],
  );

  // Traite le changement de date de fin.
  const handleDateToChange = useCallback(
    (date: Dayjs | null) => {
      setDateTo(date);
      validateAndEmit(dateFrom, date);
    },
    [dateFrom, validateAndEmit],
  );

  return (
    <div className={styles.container}>
      <Select
        value={value.period}
        options={periodOptions}
        onChange={handlePeriodChange}
        className={styles.select}
        aria-label={t("dashboard.period.label")}
      />

      {isCustom && (
        <div aria-live="polite" className={styles.dateRange}>
          <div className={styles.dateField}>
            <DatePicker
              value={dateFrom}
              onChange={handleDateFromChange}
              format={DATE_FORMATS.DISPLAY}
              placeholder={t("dashboard.period.dateFrom")}
              className={styles.datePicker}
              status={dateError ? "error" : undefined}
            />
          </div>

          <div className={styles.dateField}>
            <DatePicker
              value={dateTo}
              onChange={handleDateToChange}
              format={DATE_FORMATS.DISPLAY}
              placeholder={t("dashboard.period.dateTo")}
              className={styles.datePicker}
              status={dateError ? "error" : undefined}
              disabledDate={(current) =>
                dateFrom ? current.isBefore(dateFrom, "day") : false
              }
            />
          </div>

          {dateError && (
            <Text type="danger" className={styles.errorMessage} role="alert">
              {dateError}
            </Text>
          )}
        </div>
      )}
    </div>
  );
});

PeriodSelector.displayName = "PeriodSelector";

export default PeriodSelector;
