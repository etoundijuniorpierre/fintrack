// Page de consultation, creation et gestion des rapports d'incidents.

import { memo, useCallback, useEffect, useMemo, useState } from "react";
import { Navigate, useSearchParams } from "react-router-dom";
import { App, Form, Select, Space, Tag, Typography } from "antd";
import { SyncOutlined } from "@ant-design/icons";
import type {
  ColumnsType,
  TablePaginationConfig,
} from "antd/es/table";
import { useTranslation } from "react-i18next";
import {
  REPORT_STATUS,
  type ReportGenerateRequest,
  type ReportResponse,
} from "../../api/reporting/types";
import { PageLoader } from "../../components";
import { PageContainer, SectionCard } from "../../components/Layout";
import {
  Button,
  ConfirmDialog,
  DataGrid,
  EmptyState,
  IconOnlyButton,
  Modal,
  PageHeader,
  StatusTag,
  TableSortControl,
} from "../../components/ui";
import { useAuth } from "../../hooks/auth/useAuth";
import {
  useDeleteReport,
  useDownloadReport,
  useGenerateReport,
  useReports,
  useRetryReport,
  useSendEmailReport,
} from "../../hooks/reporting/useReports/useReports";
import { useUsers } from "../../hooks/user/useUsers/useUsers";
import { APP_ROUTES } from "../../utils/constants";
import {
  EMAIL_RECIPIENT_TOKEN_SEPARATORS,
  formatDate,
  normalizeEmailRecipients,
} from "../../utils/formatters/formatters";
import { actionIcons } from "../../utils/icons/appIcons";
import { downloadFile } from "../../utils/download/downloadFile";
import {
  PERMISSIONS,
  getReportScopeOptions,
  hasAnyPermission,
} from "../../utils/permissions/permissions";
import { isValidEmail } from "../../utils/validators/validators";
import {
  DEFAULT_PAGE_SIZE,
  getPaginationConfig,
} from "../../utils/table/pagination/paginationConfig";
import {
  actionsColumnProps,
  commonColumnProps,
} from "../../utils/table/tableColumns/tableColumns";
import { buildTableToolbar } from "../../utils/table/tableToolbar";
import GenerateReportModal from "./components/GenerateReportModal/GenerateReportModal";
import ReportDetailModal from "./components/ReportDetailModal/ReportDetailModal";
import { useTableSort } from "../../hooks/ui/useTableSort/useTableSort";
import {
  createTableSortPresets,
  readTableSort,
  TABLE_SORT_DIRECTIONS,
  toTableSortOrder,
  type TableSortMode,
} from "../../utils/table/sorting/tableSorting";

const { Text } = Typography;

const REPORT_VIEW_PERMISSIONS = [
  PERMISSIONS.REPORT.VIEW_OWN,
  PERMISSIONS.REPORT.VIEW_AGENCY,
  PERMISSIONS.REPORT.VIEW_SERVICE,
  PERMISSIONS.REPORT.VIEW_ALL,
] as const;

const REPORT_STATUS_COLORS = {
  [REPORT_STATUS.AVAILABLE]: "success",
  [REPORT_STATUS.FAILED]: "error",
  [REPORT_STATUS.PENDING]: "processing",
  [REPORT_STATUS.GENERATING]: "processing",
} as const;

const REPORT_SORT_PRESETS = createTableSortPresets("name", "createdAt");

// Normalise les valeurs de rapport page avant utilisation.
const normalize = (value?: string) => value?.toUpperCase() ?? "";

// Rend le composant ReportPage pour l'interface rapport page.
const ReportPage = memo(() => {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const [emailForm] = Form.useForm<{ recipients: string[] }>();
  const [modalOpen, setModalOpen] = useState(false);
  const [emailModalReport, setEmailModalReport] =
    useState<ReportResponse | null>(null);
  const [searchText, setSearchText] = useState("");
  const [selectedReport, setSelectedReport] = useState<ReportResponse | null>(
    null,
  );
  const [debouncedSearchText, setDebouncedSearchText] = useState("");
  // Seul axe de filtrage conserve : la liste melange rapports manuels et automatiques.
  const [generationTypeFilter, setGenerationTypeFilter] = useState<
    string | undefined
  >(undefined);
  const [paginationState, setPaginationState] = useState({
    current: 1,
    pageSize: DEFAULT_PAGE_SIZE,
  });
  const {
    field: sortField,
    order: sortOrder,
    sortParam,
    activePreset: activeSortPreset,
    selectPreset,
    handleColumnSortChange,
  } = useTableSort<TableSortMode>(
    "reports_sort",
    REPORT_SORT_PRESETS,
    "seniority",
    {
      onChange: () =>
        setPaginationState((current) => ({ ...current, current: 1 })),
    },
  );

  const hasViewPermission = hasAnyPermission(
    hasPermission,
    REPORT_VIEW_PERMISSIONS,
  );
  const canGenerate = hasPermission(PERMISSIONS.REPORT.GENERATE);
  const canExport = hasPermission(PERMISSIONS.REPORT.EXPORT);
  const canSendEmail = hasPermission(PERMISSIONS.REPORT.SEND_EMAIL);
  const canDelete = hasPermission(PERMISSIONS.REPORT.DELETE);
  const canSelectReportSubjectUser = hasPermission(PERMISSIONS.USER.VIEW_ALL);

  useEffect(() => {
    // Differe la recherche pour eviter une requete a chaque frappe.
    const handler = setTimeout(() => {
      setDebouncedSearchText(searchText);
      setPaginationState((current) => ({ ...current, current: 1 }));
    }, 500);
    return () => clearTimeout(handler);
  }, [searchText]);

  // Lien qualite Super Admin : rapports disponibles sans fichier.
  const [searchParams] = useSearchParams();
  const missingFileOnly = searchParams.get("missingFile") === "true";

  const reportParams = useMemo(
    () => ({
      page: paginationState.current - 1,
      size: paginationState.pageSize,
      ...(debouncedSearchText.trim()
        ? { keyword: debouncedSearchText.trim() }
        : {}),
      ...(generationTypeFilter ? { generationType: generationTypeFilter } : {}),
      ...(missingFileOnly ? { missingFile: true } : {}),
      sort: sortParam,
    }),
    [
      paginationState,
      debouncedSearchText,
      generationTypeFilter,
      missingFileOnly,
      sortParam,
    ],
  );

  const {
    data: reportsData,
    isLoading,
    isError,
  } = useReports(undefined, reportParams);
  const { mutateAsync: generateReportAsync, isPending: isGenerating } =
    useGenerateReport();
  const { mutateAsync: retryReportAsync, isPending: isRetrying } =
    useRetryReport();
  const { mutate: deleteReport } = useDeleteReport();
  const { mutateAsync: downloadReportAsync } = useDownloadReport();
  const { mutate: sendEmailReport, isPending: isSendingEmail } =
    useSendEmailReport();
  const { data: users = [] } = useUsers({
    enabled: canGenerate || canSendEmail || canSelectReportSubjectUser,
  });

  const reports = useMemo(() => reportsData?.content ?? [], [reportsData]);

  const scopeOptions = useMemo(
    () => getReportScopeOptions(hasPermission, t),
    [hasPermission, t],
  );

  const generationFilters = useMemo(
    () =>
      ["MANUAL", "AUTOMATIC"].map((value) => ({
        text: t(`reports.generationType.${value}`),
        value,
      })),
    [t],
  );

  // Traite l'ouverture de la generation.
  const handleOpenGenerate = useCallback(() => {
    setModalOpen(true);
  }, []);

  // Traite la fermeture de la modale.
  const handleCloseModal = useCallback(() => {
    setModalOpen(false);
  }, []);

  // Traite la generation.
  const handleGenerate = useCallback(
    async (values: ReportGenerateRequest) => {
      try {
        await generateReportAsync(values);
        setModalOpen(false);
      } catch {
        // Erreur geree par la mutation.
      }
    },
    [generateReportAsync],
  );

  // Traite la relance.
  const handleRetry = useCallback(
    async (id: string) => {
      try {
        await retryReportAsync(id);
      } catch {
        // Erreur geree par la mutation.
      }
    },
    [retryReportAsync],
  );

  // Traite le telechargement.
  const handleDownload = useCallback(
    async (report: ReportResponse) => {
      try {
        const data = await downloadReportAsync(report.id);
        const extension =
          report.format === "EXCEL"
            ? "xlsx"
            : report.format === "JSON"
              ? "json"
              : "pdf";
        downloadFile(data, `${report.name || "rapport"}.${extension}`);
      } catch {
        message.error(t("reports.errors.downloadFailed"));
      }
    },
    [downloadReportAsync, message, t],
  );

  // Traite la suppression.
  const handleDelete = useCallback(
    (id: string) => {
      deleteReport(id);
    },
    [deleteReport],
  );

  // Traite l'envoi d'e-mail.
  const handleSendEmail = useCallback(
    (report: ReportResponse) => {
      setEmailModalReport(report);
      emailForm.setFieldsValue({ recipients: report.recipients ?? [] });
    },
    [emailForm],
  );

  // Traite la fermeture de la modale e-mail.
  const handleCloseEmailModal = useCallback(() => {
    setEmailModalReport(null);
    emailForm.resetFields();
  }, [emailForm]);

  // Traite la confirmation d'envoi d'e-mail.
  const handleConfirmSendEmail = useCallback(async () => {
    if (!emailModalReport) {
      return;
    }
    try {
      const values = await emailForm.validateFields();
      const recipients = normalizeEmailRecipients(values.recipients);
      sendEmailReport(
        { id: emailModalReport.id, recipients },
        { onSuccess: handleCloseEmailModal },
      );
    } catch {
      // Ant Design affiche les erreurs de validation dans la modale.
    }
  }, [emailForm, emailModalReport, handleCloseEmailModal, sendEmailReport]);

  // Change la page de rapports sans appliquer de filtrage client.
  const handlePageChange = useCallback((page: number, pageSize: number) => {
    setPaginationState({ current: page, pageSize });
  }, []);

  const total = reportsData?.totalElements ?? 0;
  const { current: currentPage, pageSize } = paginationState;
  const tablePagination = useMemo<TablePaginationConfig>(
    () =>
      getPaginationConfig({
        current: currentPage,
        pageSize,
        total,
        onChange: handlePageChange,
        onShowSizeChange: handlePageChange,
      }),
    [handlePageChange, currentPage, pageSize, total],
  );

  const toolbar = buildTableToolbar({
    searchValue: searchText,
    onSearch: (e) => setSearchText(e.target.value),
    searchPlaceholder: t("reports.filters.search"),
    leading: (
      <TableSortControl<TableSortMode>
        presets={REPORT_SORT_PRESETS}
        labels={{
          alphabetical: t("common.sort.alphabetical"),
          seniority: t("common.sort.seniority"),
        }}
        label={t("common.sort.label")}
        activePreset={activeSortPreset}
        onSelect={selectPreset}
      />
    ),
    extra: (
      <Select
        size="small"
        allowClear
        style={{ minWidth: 180 }}
        placeholder={t("reports.table.generationType")}
        value={generationTypeFilter}
        onChange={(value) => {
          setGenerationTypeFilter(value);
          setPaginationState((current) => ({ ...current, current: 1 }));
        }}
        options={generationFilters.map((o) => ({
          label: o.text,
          value: o.value,
        }))}
      />
    ),
    addButton: canGenerate ? (
      <Button
        variant="primary"
        icon={actionIcons.add}
        onClick={handleOpenGenerate}
      >
        {t("reports.buttons.generate")}
      </Button>
    ) : undefined,
  });

  const columns = useMemo<ColumnsType<ReportResponse>>(
    () => [
      {
        ...commonColumnProps,
        title: t("reports.table.name"),
        dataIndex: "name",
        key: "name",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "name" ? toTableSortOrder(sortOrder) : undefined,
        render: (text) => (
          <Space>
            {actionIcons.report}
            <Text strong>{text}</Text>
          </Space>
        ),
      },
      {
        ...commonColumnProps,
        title: t("reports.table.type"),
        dataIndex: "type",
        key: "type",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "type" ? toTableSortOrder(sortOrder) : undefined,
        render: (type) => t(`reports.reportType.${type}`),
      },
      {
        ...commonColumnProps,
        title: t("reports.table.generationType"),
        dataIndex: "generationType",
        key: "generationType",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "generationType"
            ? toTableSortOrder(sortOrder)
            : undefined,
        render: (generationType) => (
          <Tag color={generationType === "MANUAL" ? "geekblue" : "purple"}>
            {generationType
              ? t(`reports.generationType.${generationType}`)
              : "-"}
          </Tag>
        ),
      },
      {
        ...commonColumnProps,
        title: t("reports.table.period"),
        dataIndex: "period",
        key: "period",
      },
      {
        ...commonColumnProps,
        title: t("reports.table.format"),
        dataIndex: "format",
        key: "format",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "format" ? toTableSortOrder(sortOrder) : undefined,
        render: (format) => (
          <Tag color="blue">{t(`reports.reportFormat.${format}`)}</Tag>
        ),
      },
      {
        ...commonColumnProps,
        title: t("reports.table.status"),
        dataIndex: "status",
        key: "status",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "status" ? toTableSortOrder(sortOrder) : undefined,
        render: (status) => {
          const normalizedStatus = normalize(status);
          const isPending =
            normalizedStatus === REPORT_STATUS.PENDING ||
            normalizedStatus === REPORT_STATUS.GENERATING;

          return (
            <StatusTag
              status={normalizedStatus}
              colorMap={REPORT_STATUS_COLORS}
              label={t(`reports.status.${normalizedStatus}`)}
              icon={isPending ? <SyncOutlined spin /> : undefined}
            />
          );
        },
      },
      {
        ...commonColumnProps,
        title: t("reports.table.generatedAt"),
        key: "createdAt",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "createdAt" ? toTableSortOrder(sortOrder) : undefined,
        render: (_: unknown, record) => formatDate(record.generatedAt),
      },
      {
        ...actionsColumnProps(160),
        title: t("reports.table.actions"),
        render: (_, record) => {
          const isAvailable =
            normalize(record.status) === REPORT_STATUS.AVAILABLE;
          const isFailed = normalize(record.status) === REPORT_STATUS.FAILED;
          const canDeleteForReport = canDelete;

          return (
            <Space>
              {canGenerate && isFailed && (
                <div onClick={(e) => e.stopPropagation()}>
                  <IconOnlyButton
                    key="retry"
                    variant="text"
                    size="sm"
                    icon={<SyncOutlined spin={isRetrying} />}
                    disabled={isRetrying}
                    onClick={() => handleRetry(record.id)}
                    label={t("reports.buttons.retry")}
                  />
                </div>
              )}
              {canExport && (
                <div onClick={(e) => e.stopPropagation()}>
                  <IconOnlyButton
                    key="download"
                    variant="text"
                    size="sm"
                    icon={actionIcons.download}
                    disabled={!isAvailable}
                    onClick={() => handleDownload(record)}
                    label={t("reports.buttons.download")}
                  />
                </div>
              )}
              {canSendEmail && (
                <div onClick={(e) => e.stopPropagation()}>
                  <IconOnlyButton
                    key="send-email"
                    variant="text"
                    size="sm"
                    icon={actionIcons.sendEmail}
                    disabled={!isAvailable}
                    onClick={() => handleSendEmail(record)}
                    label={t("reports.buttons.send_email")}
                  />
                </div>
              )}
              {canDeleteForReport && (
                <div onClick={(e) => e.stopPropagation()}>
                  <ConfirmDialog
                    key="delete"
                    title={t("reports.messages.deleteConfirm")}
                    description={t("reports.messages.deleteAsk")}
                    onConfirm={() => handleDelete(record.id)}
                    danger
                  >
                    <IconOnlyButton
                      variant="danger"
                      size="sm"
                      icon={actionIcons.delete}
                      label={t("reports.buttons.delete")}
                    />
                  </ConfirmDialog>
                </div>
              )}
            </Space>
          );
        },
      },
    ],
    [
      canDelete,
      canExport,
      canGenerate,
      isRetrying,
      canSendEmail,
      handleDelete,
      handleDownload,
      handleRetry,
      handleSendEmail,
      sortField,
      sortOrder,
      t,
    ],
  );

  // Ant Design fournit `extra.action` : tri QUE sur "sort", pagination QUE sur "paginate".
  // Sinon un clic de page declenche aussi le tri, qui remet a la page 1 et annule la navigation.
  const handleTableChange = useCallback(
    (
      paginationParam: TablePaginationConfig,
      _filters: unknown,
      sorter: Parameters<typeof readTableSort<ReportResponse>>[0],
      extra?: { action?: "paginate" | "sort" | "filter" },
    ) => {
      if (extra?.action === "sort") {
        const { field, order } = readTableSort(sorter);
        handleColumnSortChange(field, order);
        return;
      }
      handlePageChange(
        paginationParam.current ?? 1,
        paginationParam.pageSize ?? DEFAULT_PAGE_SIZE,
      );
    },
    [handleColumnSortChange, handlePageChange],
  );

  if (!hasViewPermission) {
    return <Navigate to={APP_ROUTES.DASHBOARD} replace />;
  }

  return (
    <PageContainer>
      <PageHeader
        title={t("reports.pageTitle")}
        subtitle={t("reports.pageSubtitle")}
      />

      {isError ? (
        <PageLoader isLoading={false} isError={true}>
          <span />
        </PageLoader>
      ) : (
        <SectionCard toolbar={toolbar} noPadding>
          <DataGrid<ReportResponse>
            data={reports}
            columns={columns}
            loading={isLoading}
            rowKey="id"
            pagination={tablePagination}
            onChange={handleTableChange}
            scroll={{ x: 1200 }}
            onRowClick={(record) => setSelectedReport(record)}
            emptyState={
              <EmptyState
                title={t("reports.empty.title")}
                description={t("reports.empty.description")}
              />
            }
          />
        </SectionCard>
      )}

      {canGenerate && (
        <GenerateReportModal
          open={modalOpen}
          scopeOptions={scopeOptions}
          canSendEmail={canSendEmail}
          canSelectAgency={hasPermission(
            PERMISSIONS.REPORT.GENERATE_ALL_SCOPES,
          )}
          canSelectService={
            hasPermission(PERMISSIONS.REPORT.GENERATE_ALL_SCOPES) ||
            hasPermission(PERMISSIONS.REPORT.VIEW_AGENCY)
          }
          users={canSelectReportSubjectUser ? users : []}
          recipientUsers={users}
          isPending={isGenerating}
          onClose={handleCloseModal}
          onGenerate={handleGenerate}
        />
      )}

      <ReportDetailModal
        open={!!selectedReport}
        record={selectedReport}
        onClose={() => setSelectedReport(null)}
      />

      <Modal
        open={!!emailModalReport}
        title={t("reports.emailModal.title")}
        onClose={handleCloseEmailModal}
        onConfirm={handleConfirmSendEmail}
        confirmLoading={isSendingEmail}
        confirmText={t("reports.emailModal.confirm")}
        cancelText={t("reports.buttons.cancel")}
        destroyOnHidden
      >
        <Form form={emailForm} layout="vertical">
          <Form.Item
            label={t("reports.emailModal.recipients")}
            name="recipients"
            rules={[
              {
                required: true,
                message: t("reports.emailModal.recipientsRequired"),
              },
              {
                validator: (_, value?: string[]) => {
                  const recipients = normalizeEmailRecipients(value);
                  if (recipients.length === 0) {
                    return Promise.reject(
                      new Error(t("reports.emailModal.recipientsRequired")),
                    );
                  }
                  const invalid = recipients.find(
                    (email) => !isValidEmail(email),
                  );
                  return invalid
                    ? Promise.reject(
                        new Error(t("reports.emailModal.recipientsInvalid")),
                      )
                    : Promise.resolve();
                },
              },
            ]}
          >
            <Select
              mode="tags"
              tokenSeparators={[...EMAIL_RECIPIENT_TOKEN_SEPARATORS]}
              placeholder={t("reports.emailModal.recipientsPlaceholder")}
              options={[]}
            />
          </Form.Item>
          <Text type="secondary">
            {emailModalReport
              ? t("reports.emailModal.description", {
                  name: emailModalReport.name,
                })
              : ""}
          </Text>
        </Form>
      </Modal>
    </PageContainer>
  );
});

ReportPage.displayName = "ReportPage";
export default ReportPage;
