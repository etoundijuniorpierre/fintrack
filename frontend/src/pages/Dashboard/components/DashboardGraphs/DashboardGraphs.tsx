// Graphiques du dashboard : repartition par type, criticite et services.
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { ValueType } from "recharts/types/component/DefaultTooltipContent";
import { Col, Row } from "antd";
import { useCallback, useMemo } from "react";
import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { EmptyState } from "../../../../components/ui";
import { useUsers } from "../../../../hooks/user/useUsers/useUsers";
import { designTokens } from "../../../../theme/tokens";
import {
  buildAgingData,
  buildCohortData,
  buildCriticalityData,
  buildMonthlySeries,
  buildStatusPhaseData,
  buildTypeData,
  COLORS,
  COHORT_COLORS,
  COHORT_SEGMENTS,
  getCriticalityColor,
  getPairedGraphColSpan,
  getPhaseColor,
  isCohortEmpty,
  UUID_PATTERN,
} from "../../../../utils/dashboard/graphUtils/graphUtils";
import {
  formatArrayOrString,
  formatHours,
} from "../../../../utils/formatters/formatters";
import type {
  MonthlyMetric,
  NamedDurationMetric,
  NamedIncidentCount,
  ServiceIncidentCount,
} from "../../../../types/dashboard";
import ChartCard from "./ChartCard/ChartCard";
import { SectionTitle } from "../../../../components/ui";
import styles from "./DashboardGraphs.module.scss";

// Definit les proprietes attendues par le composant DashboardGraphs.
interface DashboardGraphsProps {
  typeDistribution: Record<string, number>;
  criticalityDistribution: Record<string, number>;
  statusDistribution?: Record<string, number>;
  ageDistribution?: Record<string, number>;
  cohortOutcome?: Record<string, number>;
  topServices?: ServiceIncidentCount[];
  topAgencies?: NamedIncidentCount[];
  topResolvers?: NamedIncidentCount[];
  workload?: NamedIncidentCount[];
  monthlyClosures?: MonthlyMetric[];
  monthlyAvgClosureHours?: MonthlyMetric[];
  closureHoursByType?: NamedDurationMetric[];
  closureHoursByCriticality?: NamedDurationMetric[];
  year?: number;
  showTypeDistribution?: boolean;
  showCriticalityDistribution?: boolean;
  showStatusDistribution?: boolean;
  showAging?: boolean;
  showCohort?: boolean;
  showTopServices?: boolean;
  showTopAgencies?: boolean;
  showTopResolvers?: boolean;
  showWorkload?: boolean;
  showMonthlyClosures?: boolean;
  showMonthlyAvgClosure?: boolean;
  showClosureByType?: boolean;
  showClosureByCriticality?: boolean;
  yearPickerNode?: ReactNode;
}

// Centralise la logique d'interface liee a dashboard graphs.
const DashboardGraphs = ({
  typeDistribution,
  criticalityDistribution,
  statusDistribution = {},
  ageDistribution = {},
  cohortOutcome = {},
  topServices = [],
  topAgencies = [],
  topResolvers = [],
  workload = [],
  monthlyClosures = [],
  monthlyAvgClosureHours = [],
  closureHoursByType = [],
  closureHoursByCriticality = [],
  year,
  showTypeDistribution = true,
  showCriticalityDistribution = true,
  showStatusDistribution = true,
  showAging = false,
  showCohort = false,
  showWorkload = false,
  showTopServices = true,
  showTopAgencies = false,
  showTopResolvers = false,
  showMonthlyClosures = false,
  showMonthlyAvgClosure = false,
  showClosureByType = false,
  showClosureByCriticality = false,
  yearPickerNode,
}: DashboardGraphsProps) => {
  const { t } = useTranslation();

  const { data: allUsers = [] } = useUsers();

  const getRealUserName = useCallback(
    (idOrName: string) => {
      const user = allUsers.find((u) => u.id === idOrName);
      if (user) {
        const first = user.firstName || "";
        const last = user.lastName || "";
        const full = `${first} ${last}`.trim();
        return full || user.username || idOrName;
      }
      // Non resolu : on affiche un libelle lisible, jamais un identifiant brut.
      return UUID_PATTERN.test(idOrName)
        ? t("dashboard.graphs.unknown_user")
        : idOrName;
    },
    [allUsers, t],
  );

  const typeData = useMemo(
    () => buildTypeData(typeDistribution, t),
    [t, typeDistribution],
  );

  const criticalityData = useMemo(
    () => buildCriticalityData(criticalityDistribution, t),
    [criticalityDistribution, t],
  );

  const statusData = useMemo(
    () => buildStatusPhaseData(statusDistribution, t),
    [statusDistribution, t],
  );

  const agingData = useMemo(
    () => buildAgingData(ageDistribution),
    [ageDistribution],
  );

  const cohortData = useMemo(
    () => buildCohortData(cohortOutcome),
    [cohortOutcome],
  );
  const cohortEmpty = isCohortEmpty(cohortOutcome);

  const tooltipFormatter = (value: ValueType | undefined): [string, string] => [
    formatArrayOrString(value),
    t("dashboard.graphs.count"),
  ];

  const currentGraphKeys = [
    showStatusDistribution ? "status" : null,
    showAging ? "aging" : null,
    showWorkload ? "workload" : null,
  ].filter((key): key is string => Boolean(key));

  const periodGraphKeys = [
    showTypeDistribution ? "type" : null,
    showCriticalityDistribution ? "criticality" : null,
    showTopServices ? "topServices" : null,
    showTopAgencies ? "topAgencies" : null,
    showTopResolvers ? "topResolvers" : null,
    showMonthlyClosures ? "monthlyClosures" : null,
    showMonthlyAvgClosure ? "monthlyAvg" : null,
    showClosureByType ? "closureByType" : null,
    showClosureByCriticality ? "closureByCriticality" : null,
  ].filter((key): key is string => Boolean(key));

  const currentGraphSpans = currentGraphKeys.reduce<Record<string, number>>(
    (spans, key, index) => ({
      ...spans,
      [key]: getPairedGraphColSpan(index, currentGraphKeys.length),
    }),
    {},
  );

  const periodGraphSpans = periodGraphKeys.reduce<Record<string, number>>(
    (spans, key, index) => ({
      ...spans,
      [key]: getPairedGraphColSpan(index, periodGraphKeys.length),
    }),
    {},
  );

  const visibleGraphCount =
    currentGraphKeys.length + periodGraphKeys.length + (showCohort ? 1 : 0);

  const monthlyClosuresData = useMemo(
    () => buildMonthlySeries(monthlyClosures, t),
    [monthlyClosures, t],
  );
  const monthlyAvgData = useMemo(
    () => buildMonthlySeries(monthlyAvgClosureHours, t),
    [monthlyAvgClosureHours, t],
  );

  // Le delai moyen d'une categorie n'a de sens qu'accompagne de son effectif.
  const durationTooltipFormatter = useCallback(
    (value: ValueType | undefined): [string, string] => [
      formatHours(Number(formatArrayOrString(value))),
      t("dashboard.graphs.avg_closure"),
    ],
    [t],
  );
  const durationExportRows = useCallback(
    (rows: NamedDurationMetric[]) =>
      rows.map((item, index) => ({
        label: `${item.name} (${item.sampleSize})`,
        value: Math.round(item.avgHours * 10) / 10,
        color: COLORS[index % COLORS.length],
      })),
    [],
  );
  const closureByTypeRows = useMemo(
    () => durationExportRows(closureHoursByType),
    [closureHoursByType, durationExportRows],
  );
  const closureByCriticalityRows = useMemo(
    () => durationExportRows(closureHoursByCriticality),
    [closureHoursByCriticality, durationExportRows],
  );

  const typeExportRows = useMemo(
    () =>
      typeData.map((item, index) => ({
        label: item.name,
        value: item.value,
        color: COLORS[index % COLORS.length],
      })),
    [typeData],
  );
  const criticalityExportRows = useMemo(
    () =>
      criticalityData.map((item, index) => ({
        label: item.name,
        value: item.value,
        color: getCriticalityColor(item.rawName, index),
      })),
    [criticalityData],
  );
  const statusExportRows = useMemo(
    () =>
      statusData.map((item, index) => ({
        label: item.name,
        value: item.value,
        color: getPhaseColor(item.rawName, index),
      })),
    [statusData],
  );
  const agingExportRows = useMemo(
    () =>
      agingData.map((item, index) => ({
        label: item.name,
        value: item.value,
        color:
          index >= agingData.length - 1
            ? designTokens.colorError
            : COLORS[index % COLORS.length],
      })),
    [agingData],
  );
  const cohortExportRows = useMemo(
    () =>
      COHORT_SEGMENTS.map((key) => ({
        label: t(`dashboard.graphs.cohort.${key}`),
        value: cohortOutcome[key] ?? 0,
        color: COHORT_COLORS[key],
      })).filter((item) => item.value > 0),
    [cohortOutcome, t],
  );
  const topServicesExportRows = useMemo(
    () =>
      topServices.map((item) => ({
        label: item.serviceName,
        value: item.count,
        color: designTokens.colorWarning,
      })),
    [topServices],
  );
  const topAgenciesExportRows = useMemo(
    () =>
      topAgencies.map((item) => ({
        label: item.name,
        value: item.count,
        color: designTokens.colorInfo,
      })),
    [topAgencies],
  );
  const topResolversData = useMemo(
    () =>
      topResolvers.map((item) => ({
        ...item,
        name: getRealUserName(item.name),
      })),
    [topResolvers, getRealUserName],
  );
  const topResolversExportRows = useMemo(
    () =>
      topResolversData.map((item) => ({
        label: item.name,
        value: item.count,
        color: designTokens.colorSuccess,
      })),
    [topResolversData],
  );
  const workloadData = useMemo(
    () =>
      workload.map((item) => ({ ...item, name: getRealUserName(item.name) })),
    [workload, getRealUserName],
  );
  const workloadExportRows = useMemo(
    () =>
      workloadData.map((item) => ({
        label: item.name,
        value: item.count,
        color: designTokens.colorPrimary,
      })),
    [workloadData],
  );
  const monthlyClosuresExportRows = useMemo(
    () =>
      monthlyClosuresData.map((item) => ({
        label: item.month,
        value: item.value,
        color: designTokens.colorInfo,
      })),
    [monthlyClosuresData],
  );
  const monthlyAvgExportRows = useMemo(
    () =>
      monthlyAvgData.map((item) => ({
        label: item.month,
        value: item.value,
        color: designTokens.colorSuccess,
      })),
    [monthlyAvgData],
  );

  const typeTitle = t("dashboard.graphs.type_distribution");
  const criticalityTitle = t("dashboard.graphs.criticality_distribution");
  const statusTitle = t("dashboard.graphs.status_distribution");
  const agingTitle = t("dashboard.graphs.aging");
  const cohortTitle = t("dashboard.graphs.cohort.title");
  const topServicesTitle = t("dashboard.graphs.top_services");
  const topAgenciesTitle = t("dashboard.graphs.top_agencies");
  const topResolversTitle = t("dashboard.graphs.top_resolvers");
  const workloadTitle = t("dashboard.graphs.workload");
  const monthlyClosuresTitle = t("dashboard.graphs.monthlyClosures", {
    year: year ?? "",
  });
  const monthlyAvgTitle = t("dashboard.graphs.monthly_avg_closure", {
    year: year ?? "",
  });

  if (visibleGraphCount === 0) {
    return <EmptyState title={t("dashboard.graphs.empty")} />;
  }

  return (
    <div className={styles.graphsContainer}>
      {currentGraphKeys.length > 0 && (
        <>
          <SectionTitle title={t("dashboard.sections.current")} />
          <Row gutter={[24, 24]} className={styles.graphRow}>
            {showStatusDistribution && (
              <Col xs={24} lg={currentGraphSpans.status}>
                <ChartCard
                  title={statusTitle}
                  filename="status-distribution"
                  rows={statusExportRows}
                >
                  <div className={styles.chartFrame}>
                    {statusData.length > 0 ? (
                      <ResponsiveContainer
                        width="100%"
                        height="100%"
                        minWidth={0}
                        minHeight={1}
                      >
                        <BarChart
                          data={statusData}
                          margin={{ top: 12, right: 16, bottom: 8, left: 0 }}
                        >
                          <CartesianGrid
                            strokeDasharray="3 3"
                            vertical={false}
                          />
                          <XAxis dataKey="name" />
                          <YAxis allowDecimals={false} />
                          <Tooltip formatter={tooltipFormatter} />
                          <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                            {statusData.map(({ rawName }, index) => (
                              <Cell
                                key={`status-${rawName}`}
                                fill={getPhaseColor(rawName, index)}
                              />
                            ))}
                          </Bar>
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <EmptyState
                        title={t("dashboard.typeDistribution.empty")}
                      />
                    )}
                  </div>
                </ChartCard>
              </Col>
            )}{" "}
            {showAging && (
              <Col xs={24} lg={currentGraphSpans.aging}>
                <ChartCard
                  title={agingTitle}
                  filename="backlog-aging"
                  rows={agingExportRows}
                >
                  <div className={styles.chartFrame}>
                    {agingData.length > 0 ? (
                      <ResponsiveContainer
                        width="100%"
                        height="100%"
                        minWidth={0}
                        minHeight={1}
                      >
                        <BarChart
                          data={agingData}
                          margin={{ top: 12, right: 16, bottom: 8, left: 0 }}
                        >
                          <CartesianGrid
                            strokeDasharray="3 3"
                            vertical={false}
                          />
                          <XAxis dataKey="name" />
                          <YAxis allowDecimals={false} />
                          <Tooltip formatter={tooltipFormatter} />
                          <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                            {agingData.map(({ name }, index) => (
                              <Cell
                                key={`age-${name}`}
                                fill={
                                  index >= agingData.length - 1
                                    ? designTokens.colorError
                                    : COLORS.at(index % COLORS.length)
                                }
                              />
                            ))}
                          </Bar>
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <EmptyState title={t("dashboard.graphs.empty")} />
                    )}
                  </div>
                </ChartCard>
              </Col>
            )}{" "}
            {showWorkload && (
              <Col xs={24} lg={currentGraphSpans.workload}>
                <ChartCard
                  title={workloadTitle}
                  filename="workload"
                  rows={workloadExportRows}
                >
                  <div className={styles.chartFrame}>
                    {workload.length > 0 ? (
                      <ResponsiveContainer
                        width="100%"
                        height="100%"
                        minWidth={0}
                        minHeight={1}
                      >
                        <BarChart
                          data={workloadData}
                          layout="vertical"
                          margin={{ top: 8, right: 24, bottom: 8, left: 12 }}
                        >
                          <CartesianGrid
                            strokeDasharray="3 3"
                            horizontal={false}
                          />
                          <XAxis type="number" allowDecimals={false} />
                          <YAxis
                            dataKey="name"
                            type="category"
                            width={130}
                            tick={{ fontSize: 12 }}
                          />
                          <Tooltip formatter={tooltipFormatter} />
                          <Bar
                            dataKey="count"
                            fill={designTokens.colorPrimary}
                            radius={[0, 6, 6, 0]}
                          />
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <EmptyState title={t("dashboard.graphs.empty")} />
                    )}
                  </div>
                </ChartCard>
              </Col>
            )}
          </Row>
        </>
      )}
      {(periodGraphKeys.length > 0 || showCohort) && (
        <>
          <SectionTitle title={t("dashboard.sections.period")} />
          <Row gutter={[24, 24]} className={styles.graphRow}>
            {showTypeDistribution && (
              <Col xs={24} lg={periodGraphSpans.type}>
                <ChartCard
                  title={typeTitle}
                  filename="type-distribution"
                  rows={typeExportRows}
                >
                  <div className={styles.chartFrame}>
                    {typeData.length > 0 ? (
                      <ResponsiveContainer
                        width="100%"
                        height="100%"
                        minWidth={0}
                        minHeight={1}
                      >
                        <BarChart
                          data={typeData}
                          layout="vertical"
                          margin={{ top: 8, right: 24, bottom: 8, left: 12 }}
                        >
                          <CartesianGrid
                            strokeDasharray="3 3"
                            horizontal={false}
                          />
                          <XAxis type="number" allowDecimals={false} />
                          <YAxis
                            dataKey="name"
                            type="category"
                            width={150}
                            tick={{ fontSize: 12 }}
                          />
                          <Tooltip formatter={tooltipFormatter} />
                          <Bar dataKey="value" radius={[0, 6, 6, 0]}>
                            {typeData.map(({ rawName }, index) => (
                              <Cell
                                key={`type-${rawName}`}
                                fill={COLORS.at(index % COLORS.length)}
                              />
                            ))}
                          </Bar>
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <EmptyState
                        title={t("dashboard.typeDistribution.empty")}
                      />
                    )}
                  </div>
                </ChartCard>
              </Col>
            )}{" "}
            {showCriticalityDistribution && (
              <Col xs={24} lg={periodGraphSpans.criticality}>
                <ChartCard
                  title={criticalityTitle}
                  filename="criticality-distribution"
                  rows={criticalityExportRows}
                >
                  <div className={styles.chartFrame}>
                    {criticalityData.length > 0 ? (
                      <ResponsiveContainer
                        width="100%"
                        height="100%"
                        minWidth={0}
                        minHeight={1}
                      >
                        <PieChart>
                          <Pie
                            isAnimationActive={false}
                            data={criticalityData}
                            dataKey="value"
                            nameKey="name"
                            innerRadius="45%"
                            outerRadius="75%"
                            paddingAngle={2}
                          >
                            {criticalityData.map(({ rawName }, index) => (
                              <Cell
                                key={`criticality-${rawName}`}
                                fill={getCriticalityColor(rawName, index)}
                              />
                            ))}
                          </Pie>
                          <Tooltip formatter={tooltipFormatter} />
                          <Legend />
                        </PieChart>
                      </ResponsiveContainer>
                    ) : (
                      <EmptyState
                        title={t("dashboard.typeDistribution.empty")}
                      />
                    )}
                  </div>
                </ChartCard>
              </Col>
            )}{" "}
            {showCohort && (
              <Col xs={24}>
                <ChartCard
                  title={cohortTitle}
                  filename="cohort-outcome"
                  rows={cohortExportRows}
                >
                  <div className={styles.chartFrame}>
                    {!cohortEmpty ? (
                      <ResponsiveContainer
                        width="100%"
                        height="100%"
                        minWidth={0}
                        minHeight={1}
                      >
                        <BarChart
                          data={cohortData}
                          layout="vertical"
                          stackOffset="expand"
                          margin={{ top: 8, right: 24, bottom: 8, left: 12 }}
                        >
                          <XAxis type="number" hide domain={[0, 1]} />
                          <YAxis dataKey="name" type="category" hide />
                          <Tooltip />
                          <Legend />
                          {COHORT_SEGMENTS.map((key) => (
                            <Bar
                              key={key}
                              dataKey={key}
                              name={t(`dashboard.graphs.cohort.${key}`)}
                              stackId="cohort"
                              fill={COHORT_COLORS[key]}
                            />
                          ))}
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <EmptyState title={t("dashboard.graphs.empty")} />
                    )}
                  </div>
                </ChartCard>
              </Col>
            )}{" "}
            {showTopServices && (
              <Col xs={24} lg={periodGraphSpans.topServices}>
                <ChartCard
                  title={topServicesTitle}
                  filename="top-services"
                  rows={topServicesExportRows}
                >
                  <div className={styles.chartFrame}>
                    {topServices.length > 0 ? (
                      <ResponsiveContainer
                        width="100%"
                        height="100%"
                        minWidth={0}
                        minHeight={1}
                      >
                        <BarChart
                          data={topServices}
                          layout="vertical"
                          margin={{ top: 8, right: 24, bottom: 8, left: 12 }}
                        >
                          <CartesianGrid
                            strokeDasharray="3 3"
                            horizontal={false}
                          />
                          <XAxis type="number" allowDecimals={false} />
                          <YAxis
                            dataKey="serviceName"
                            type="category"
                            width={130}
                            tick={{ fontSize: 12 }}
                          />
                          <Tooltip formatter={tooltipFormatter} />
                          <Bar
                            dataKey="count"
                            fill={designTokens.colorWarning}
                            radius={[0, 6, 6, 0]}
                          />
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <EmptyState title={t("dashboard.topServices.empty")} />
                    )}
                  </div>
                </ChartCard>
              </Col>
            )}{" "}
            {showTopAgencies && (
              <Col xs={24} lg={periodGraphSpans.topAgencies}>
                <ChartCard
                  title={topAgenciesTitle}
                  filename="top-agencies"
                  rows={topAgenciesExportRows}
                >
                  <div className={styles.chartFrame}>
                    {topAgencies.length > 0 ? (
                      <ResponsiveContainer
                        width="100%"
                        height="100%"
                        minWidth={0}
                        minHeight={1}
                      >
                        <BarChart
                          data={topAgencies}
                          layout="vertical"
                          margin={{ top: 8, right: 24, bottom: 8, left: 12 }}
                        >
                          <CartesianGrid
                            strokeDasharray="3 3"
                            horizontal={false}
                          />
                          <XAxis type="number" allowDecimals={false} />
                          <YAxis
                            dataKey="name"
                            type="category"
                            width={130}
                            tick={{ fontSize: 12 }}
                          />
                          <Tooltip formatter={tooltipFormatter} />
                          <Bar
                            dataKey="count"
                            fill={designTokens.colorInfo}
                            radius={[0, 6, 6, 0]}
                          />
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <EmptyState title={t("dashboard.topAgencies.empty")} />
                    )}
                  </div>
                </ChartCard>
              </Col>
            )}{" "}
            {showTopResolvers && (
              <Col xs={24} lg={periodGraphSpans.topResolvers}>
                <ChartCard
                  title={topResolversTitle}
                  filename="top-resolvers"
                  rows={topResolversExportRows}
                >
                  <div className={styles.chartFrame}>
                    {topResolvers.length > 0 ? (
                      <ResponsiveContainer
                        width="100%"
                        height="100%"
                        minWidth={0}
                        minHeight={1}
                      >
                        <BarChart
                          data={topResolversData}
                          layout="vertical"
                          margin={{ top: 8, right: 24, bottom: 8, left: 12 }}
                        >
                          <CartesianGrid
                            strokeDasharray="3 3"
                            horizontal={false}
                          />
                          <XAxis type="number" allowDecimals={false} />
                          <YAxis
                            dataKey="name"
                            type="category"
                            width={130}
                            tick={{ fontSize: 12 }}
                          />
                          <Tooltip formatter={tooltipFormatter} />
                          <Bar
                            dataKey="count"
                            fill={designTokens.colorSuccess}
                            radius={[0, 6, 6, 0]}
                          />
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <EmptyState title={t("dashboard.topResolvers.empty")} />
                    )}
                  </div>
                </ChartCard>
              </Col>
            )}{" "}
            {showMonthlyClosures && (
              <Col xs={24} lg={periodGraphSpans.monthlyClosures}>
                <ChartCard
                  title={monthlyClosuresTitle}
                  filename="monthly-closures"
                  rows={monthlyClosuresExportRows}
                  toolbar={yearPickerNode}
                >
                  <div className={styles.chartFrame}>
                    <ResponsiveContainer
                      width="100%"
                      height="100%"
                      minWidth={0}
                      minHeight={1}
                    >
                      <LineChart
                        data={monthlyClosuresData}
                        margin={{ top: 12, right: 24, bottom: 8, left: 0 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" vertical={false} />
                        <XAxis dataKey="month" />
                        <YAxis allowDecimals={false} />
                        <Tooltip />
                        <Line
                          type="monotone"
                          dataKey="value"
                          stroke={designTokens.colorInfo}
                          strokeWidth={2}
                          dot={{ r: 3 }}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </ChartCard>
              </Col>
            )}{" "}
            {showClosureByType && (
              <Col xs={24} lg={periodGraphSpans.closureByType}>
                <ChartCard
                  title={t("dashboard.graphs.closure_by_type")}
                  filename="closure-hours-by-type"
                  rows={closureByTypeRows}
                >
                  <div className={styles.chartFrame}>
                    {closureHoursByType.length > 0 ? (
                      <ResponsiveContainer
                        width="100%"
                        height="100%"
                        minWidth={0}
                        minHeight={1}
                      >
                        <BarChart
                          data={closureHoursByType}
                          layout="vertical"
                          margin={{ top: 8, right: 24, bottom: 8, left: 12 }}
                        >
                          <CartesianGrid
                            strokeDasharray="3 3"
                            horizontal={false}
                          />
                          <XAxis type="number" allowDecimals />
                          <YAxis
                            dataKey="name"
                            type="category"
                            width={130}
                            tick={{ fontSize: 12 }}
                          />
                          <Tooltip formatter={durationTooltipFormatter} />
                          <Bar
                            dataKey="avgHours"
                            fill={designTokens.colorInfo}
                            radius={[0, 6, 6, 0]}
                          />
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <EmptyState title={t("dashboard.graphs.empty")} />
                    )}
                  </div>
                </ChartCard>
              </Col>
            )}{" "}
            {showClosureByCriticality && (
              <Col xs={24} lg={periodGraphSpans.closureByCriticality}>
                <ChartCard
                  title={t("dashboard.graphs.closure_by_criticality")}
                  filename="closure-hours-by-criticality"
                  rows={closureByCriticalityRows}
                >
                  <div className={styles.chartFrame}>
                    {closureHoursByCriticality.length > 0 ? (
                      <ResponsiveContainer
                        width="100%"
                        height="100%"
                        minWidth={0}
                        minHeight={1}
                      >
                        <BarChart
                          data={closureHoursByCriticality}
                          layout="vertical"
                          margin={{ top: 8, right: 24, bottom: 8, left: 12 }}
                        >
                          <CartesianGrid
                            strokeDasharray="3 3"
                            horizontal={false}
                          />
                          <XAxis type="number" allowDecimals />
                          <YAxis
                            dataKey="name"
                            type="category"
                            width={130}
                            tick={{ fontSize: 12 }}
                          />
                          <Tooltip formatter={durationTooltipFormatter} />
                          <Bar
                            dataKey="avgHours"
                            fill={designTokens.colorWarning}
                            radius={[0, 6, 6, 0]}
                          />
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <EmptyState title={t("dashboard.graphs.empty")} />
                    )}
                  </div>
                </ChartCard>
              </Col>
            )}{" "}
            {showMonthlyAvgClosure && (
              <Col xs={24} lg={periodGraphSpans.monthlyAvg}>
                <ChartCard
                  title={monthlyAvgTitle}
                  filename="monthly-average-closure"
                  rows={monthlyAvgExportRows}
                  toolbar={yearPickerNode}
                >
                  <div className={styles.chartFrame}>
                    <ResponsiveContainer
                      width="100%"
                      height="100%"
                      minWidth={0}
                      minHeight={1}
                    >
                      <LineChart
                        data={monthlyAvgData}
                        margin={{ top: 12, right: 24, bottom: 8, left: 0 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" vertical={false} />
                        <XAxis dataKey="month" />
                        <YAxis allowDecimals />
                        <Tooltip />
                        <Line
                          type="monotone"
                          dataKey="value"
                          stroke={designTokens.colorSuccess}
                          strokeWidth={2}
                          dot={{ r: 3 }}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </ChartCard>
              </Col>
            )}
          </Row>
        </>
      )}
    </div>
  );
};

export default DashboardGraphs;
