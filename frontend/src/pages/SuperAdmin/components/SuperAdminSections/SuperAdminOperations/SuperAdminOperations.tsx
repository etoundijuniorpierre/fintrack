// Composant React : porte l'interface de super-administration operations.

import { useMemo } from "react";
import {
  Alert,
  App,
  Button,
  Col,
  Descriptions,
  Empty,
  List,
  Row,
  Space,
  Table,
  Tag,
  Typography,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import { useTranslation } from "react-i18next";
import { useQueries, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  superAdminApi,
  type OperationsSection,
} from "../../../../../api/superAdmin";
import { SectionCard } from "../../../../../components/Layout";
import { TableEllipsisText } from "../../../../../components/ui";
import { formatDate } from "../../../../../utils/formatters/formatters";
import { QUERY_KEYS } from "../../../../../utils/constants";
import {
  asArray,
  asNumber,
  asRecord,
  asString,
  type RowMap,
} from "../../../../../utils/superAdmin/superAdminSectionUtils/superAdminSectionUtils";
import { notificationSubject } from "../../../../../utils/notifications/notificationText";
import styles from "../../../SuperAdminPage.module.scss";
import {
  compareTableDates,
  compareTableNumbers,
  compareTableText,
  TABLE_SORT_DIRECTIONS,
} from "../../../../../utils/table/sorting/tableSorting";

const { Text, Title, Paragraph } = Typography;

// Sujet d'une notification agregee, dans la langue du lecteur.
const rowSubject = (row: RowMap, language: string) =>
  notificationSubject(
    {
      subject: asString(row.subject),
      subjectEn: asString(row.subjectEn),
    },
    language,
  );

// Definit les proprietes attendues par le composant Operations.
interface OperationsProps {
  section?: OperationsSection;
  loading: boolean;
}

// Rend le composant SuperAdminOperations pour l'interface super-administration operations.
export const SuperAdminOperations = ({ section, loading }: OperationsProps) => {
  const { t, i18n } = useTranslation();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const notifications = asRecord(section?.notifications);
  const failedNotifications = asArray(notifications.failedNotifications);
  const invalidRecipients = asArray(notifications.invalidRecipients);
  const byChannel = asRecord(notifications.byChannel);
  const emailChannel = asRecord(byChannel.EMAIL);
  const internalChannel = asRecord(byChannel.INTERNAL);

  const jobsQueries = useQueries({
    queries: [
      {
        queryKey: ["superAdmin", "jobs", "user"],
        queryFn: () => superAdminApi.getJobs("user"),
      },
      {
        queryKey: ["superAdmin", "jobs", "reporting"],
        queryFn: () => superAdminApi.getJobs("reporting"),
      },
    ],
  });

  const triggerJobMutation = useMutation({
    mutationFn: ({
      service,
      jobName,
    }: {
      service: "user" | "reporting";
      jobName: string;
    }) => superAdminApi.triggerJob(service, jobName),
    onSuccess: (result) => {
      message.success(
        t("superAdmin.operations.jobTriggeredCount", {
          count: result?.affected ?? 0,
        }),
      );
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.SUPER_ADMIN.OPERATIONS,
      });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.SUPER_ADMIN.REPORTING_OVERVIEW,
      });
    },
    onError: () => message.error(t("superAdmin.operations.jobTriggerFailed")),
  });

  const allJobs = [
    ...(jobsQueries[0].data?.map((j) => ({
      ...asRecord(j),
      service: "user",
    })) || []),
    ...(jobsQueries[1].data?.map((j) => ({
      ...asRecord(j),
      service: "reporting",
    })) || []),
  ];

  const jobsLoading = jobsQueries.some((q) => q.isLoading);

  const jobColumns = useMemo<ColumnsType<RowMap>>(
    () => [
      {
        title: t("superAdmin.operations.jobName"),
        dataIndex: "name",
        key: "name",
        defaultSortOrder: "ascend",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) => compareTableText(left.name, right.name),
        render: (value: unknown) => (
          <Text strong>
            {t(
              `superAdmin.operations.jobNames.${asString(value)}`,
              asString(value),
            )}
          </Text>
        ),
      },
      {
        title: t("superAdmin.operations.jobService"),
        dataIndex: "service",
        key: "service",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) => compareTableText(left.service, right.service),
        render: (value: unknown) => <Tag>{asString(value)}</Tag>,
      },
      {
        title: t("superAdmin.operations.jobDescription"),
        dataIndex: "description",
        key: "description",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableText(left.description, right.description),
        render: (value: unknown) => (
          <TableEllipsisText
            value={t(
              `superAdmin.operations.jobDescriptions.${asString(value)}`,
              asString(value),
            )}
          />
        ),
      },
      {
        title: t("superAdmin.operations.jobSchedule"),
        dataIndex: "schedule",
        key: "schedule",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableText(left.schedule, right.schedule),
        render: (value: unknown) => <Tag color="blue">{asString(value)}</Tag>,
      },
      {
        title: t("superAdmin.operations.jobActions"),
        key: "actions",
        render: (_: unknown, record) =>
          record.triggerable === false ? (
            <Text type="secondary">
              {t("superAdmin.operations.scheduledOnly")}
            </Text>
          ) : (
            <Button
              size="small"
              loading={
                triggerJobMutation.isPending &&
                triggerJobMutation.variables?.jobName === asString(record.name)
              }
              onClick={() =>
                triggerJobMutation.mutate({
                  service: asString(record.service) as "user" | "reporting",
                  jobName: asString(record.name),
                })
              }
            >
              {t("superAdmin.operations.triggerJob")}
            </Button>
          ),
      },
    ],
    [t, triggerJobMutation],
  );

  const failedColumns = useMemo<ColumnsType<RowMap>>(
    () => [
      {
        title: t("superAdmin.notifications.channel"),
        dataIndex: "type",
        key: "type",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) => compareTableText(left.type, right.type),
        render: (value: unknown) => (
          <Tag
            color={
              asString(value).toUpperCase() === "EMAIL" ? "blue" : "purple"
            }
          >
            {asString(value)}
          </Tag>
        ),
      },
      {
        title: t("superAdmin.notifications.recipient"),
        dataIndex: "recipient",
        key: "recipient",
        defaultSortOrder: "ascend",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableText(left.recipient, right.recipient),
        render: (value: unknown) => asString(value),
      },
      {
        title: t("superAdmin.notifications.subject"),
        dataIndex: "subject",
        key: "subject",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableText(
            rowSubject(left, i18n.language),
            rowSubject(right, i18n.language),
          ),
        render: (_value: unknown, record: RowMap) =>
          rowSubject(record, i18n.language),
      },
      {
        title: t("superAdmin.notifications.diagnosis"),
        dataIndex: "diagnosis",
        key: "diagnosis",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableText(left.diagnosis, right.diagnosis),
        render: (value: unknown) => (
          <Tag color="orange">
            {t(
              `superAdmin.notifications.diagnoses.${asString(value)}`,
              asString(value),
            )}
          </Tag>
        ),
      },
      {
        title: t("superAdmin.notifications.retries"),
        key: "retries",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableNumbers(left.retryCount, right.retryCount),
        render: (_: unknown, record) =>
          `${asNumber(record.retryCount)}/${asNumber(record.maxRetry)}`,
      },
      {
        title: t("superAdmin.notifications.nextRetry"),
        dataIndex: "nextRetry",
        key: "nextRetry",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableDates(left.nextRetry, right.nextRetry),
        render: (value: unknown) => {
          const date = asString(value, "");
          return date ? formatDate(date) : "-";
        },
      },
      {
        title: t("superAdmin.notifications.recommendedAction"),
        dataIndex: "recommendedAction",
        key: "recommendedAction",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableText(left.recommendedAction, right.recommendedAction),
        render: (value: unknown) =>
          t(
            `superAdmin.notifications.recommendedActions.${asString(value)}`,
            asString(value),
          ),
      },
    ],
    [t, i18n.language],
  );

  return (
    <Row gutter={[16, 16]}>
      <Col xs={24}>
        <SectionCard title={t("superAdmin.sections.email")} loading={loading}>
          <Space
            direction="vertical"
            size="middle"
            className={styles.fullWidth}
          >
            <Paragraph type="secondary">
              {t("superAdmin.operations.emailIntro")}
            </Paragraph>
            <Row gutter={[16, 16]}>
              <Col xs={24} md={12}>
                <Descriptions
                  column={1}
                  bordered
                  size="small"
                  title={
                    <Text strong>
                      {t("superAdmin.notifications.channelEmail")}
                    </Text>
                  }
                >
                  <Descriptions.Item
                    label={t("superAdmin.notifications.total")}
                  >
                    {asNumber(emailChannel.total)}
                  </Descriptions.Item>
                  <Descriptions.Item label={t("superAdmin.notifications.sent")}>
                    {asNumber(emailChannel.sent)}
                  </Descriptions.Item>
                  <Descriptions.Item
                    label={t("superAdmin.notifications.failed")}
                  >
                    <Tag
                      color={
                        asNumber(emailChannel.failed) > 0 ? "error" : "default"
                      }
                    >
                      {asNumber(emailChannel.failed)}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item
                    label={t("superAdmin.notifications.pending")}
                  >
                    {asNumber(emailChannel.pending)}
                  </Descriptions.Item>
                </Descriptions>
              </Col>
              <Col xs={24} md={12}>
                <Descriptions
                  column={1}
                  bordered
                  size="small"
                  title={
                    <Text strong>
                      {t("superAdmin.notifications.channelInternal")}
                    </Text>
                  }
                >
                  <Descriptions.Item
                    label={t("superAdmin.notifications.total")}
                  >
                    {asNumber(internalChannel.total)}
                  </Descriptions.Item>
                  <Descriptions.Item label={t("superAdmin.notifications.sent")}>
                    {asNumber(internalChannel.sent)}
                  </Descriptions.Item>
                  <Descriptions.Item
                    label={t("superAdmin.notifications.failed")}
                  >
                    <Tag
                      color={
                        asNumber(internalChannel.failed) > 0
                          ? "error"
                          : "default"
                      }
                    >
                      {asNumber(internalChannel.failed)}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item
                    label={t("superAdmin.notifications.pending")}
                  >
                    {asNumber(internalChannel.pending)}
                  </Descriptions.Item>
                </Descriptions>
              </Col>
            </Row>
            {failedNotifications.length > 0 && (
              <Alert
                type="error"
                showIcon
                message={t("superAdmin.notifications.failedAlert", {
                  count: failedNotifications.length,
                })}
                description={t(
                  "superAdmin.notifications.failedAlertDescription",
                )}
              />
            )}

            <Title level={5}>
              {t("superAdmin.notifications.failedHeader")}
            </Title>
            <Table<RowMap>
              rowKey={(row) =>
                asString(
                  row.id,
                  `${asString(row.recipient)}-${asString(row.createdAt)}`,
                )
              }
              columns={failedColumns}
              dataSource={failedNotifications.slice(0, 10)}
              pagination={false}
              size="small"
              locale={{
                emptyText: (
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description={t("superAdmin.notifications.noFailed")}
                  />
                ),
              }}
            />

            <Title level={5}>
              {t("superAdmin.notifications.invalidRecipients")}
            </Title>
            <List
              size="small"
              dataSource={invalidRecipients.slice(0, 5)}
              locale={{
                emptyText: (
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description={t("superAdmin.notifications.noInvalid")}
                  />
                ),
              }}
              renderItem={(item) => (
                <List.Item>
                  <Space direction="vertical" size={0}>
                    <Text>
                      {asString(item.recipientEmail, asString(item.recipient))}
                    </Text>
                    <Text type="secondary">
                      {rowSubject(item, i18n.language)}
                    </Text>
                  </Space>
                </List.Item>
              )}
            />
          </Space>
        </SectionCard>
      </Col>
      <Col xs={24}>
        <SectionCard
          title={t("superAdmin.sections.backgroundJobs")}
          loading={jobsLoading}
        >
          <Table<RowMap>
            rowKey={(row) => `${asString(row.service)}-${asString(row.name)}`}
            columns={jobColumns}
            dataSource={allJobs}
            pagination={false}
            size="small"
            locale={{
              emptyText: (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={t("superAdmin.operations.noJobs")}
                />
              ),
            }}
          />
        </SectionCard>
      </Col>
    </Row>
  );
};
