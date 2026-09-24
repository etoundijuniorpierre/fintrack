// Page de detail d'un incident : consultation, edition inline, actions workflow, pieces jointes, commentaires et historique.
import {
  Alert,
  App,
  Breadcrumb,
  Divider,
  Row,
  Col,
  Typography,
  Skeleton,
} from "antd";
import type { ReactNode } from "react";
import { memo, useCallback, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router-dom";
import dayjs from "dayjs";
import { PageLoader } from "../../../components";
import {
  PageContainer,
  SectionCard,
  SplitLayout,
} from "../../../components/Layout";
import { Button, PageHeader, StatusTag } from "../../../components/ui";
import { useAuth } from "../../../hooks/auth/useAuth";
import { useIncident } from "../../../hooks/incident/useIncidents/useIncidents";
import { incidentApi } from "../../../api/incident";
import { downloadFile } from "../../../utils/download/downloadFile";
import { incidentNavigation } from "../../../utils/navigation/incidents/incidents";
import {
  formatDate,
  formatDateTime,
  formatUserName,
} from "../../../utils/formatters/formatters";
import { CRITICALITY_COLORS, STATUS_COLORS } from "../../../api/incident/types";
import { IncidentCause, IncidentStatus } from "../../../api/incident/enums/enums";
import { PERMISSIONS } from "../../../utils/permissions/permissions";
import WorkflowActions from "../components/Actions/WorkflowActions";
import CommentsSection from "../components/Comments/sections/CommentsSection";
import AttachmentsSection from "../components/Attachments/section/AttachmentsSection";
import ResolutionAttachments from "../components/Attachments/ResolutionAttachments";
import HistoryTimeline from "../components/History/HistoryTimeline";
import IncidentForm from "../components/Form/IncidentForm";
import { actionIcons } from "../../../utils/icons/appIcons";
import styles from "./ViewIncident.module.scss";

const { Text } = Typography;

/** Paire label/valeur uniforme pour le mode view */
// Centralise la logique d'interface liee a field.
const Field = ({ label, children }: { label: string; children: ReactNode }) => (
  <div className={styles.field}>
    <span className={styles.fieldLabel}>{label}</span>
    <div className={styles.fieldValue}>{children}</div>
  </div>
);

// Rend le composant ViewIncident pour l'interface view incident.
const ViewIncident = memo(() => {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, hasPermission } = useAuth();

  const { message } = App.useApp();
  const { data: incident, isLoading, isError, error } = useIncident(id);
  const [isEditing, setIsEditing] = useState(false);
  const [isDownloadingReport, setIsDownloadingReport] = useState(false);

  // Auteur du dernier rejet (source de verite : historique). Sur un incident REJETE,
  // seul le createur ou ce rejeteur peut rouvrir : on n'expose le bouton qu'a eux,
  // en miroir de IncidentServiceImpl.reopen.
  const reopenRejectorId = useMemo(() => {
    const rejections = (incident?.history ?? []).filter(
      (entry) => entry.newValue === IncidentStatus.REJECTED,
    );
    if (rejections.length === 0) return null;
    const latest = rejections.reduce((mostRecent, entry) =>
      entry.createdAt >= mostRecent.createdAt ? entry : mostRecent,
    );
    return latest.user?.id ?? null;
  }, [incident?.history]);

  // Nomme celui de qui la confirmation d'actualite est attendue, comme la fiche nomme
  // deja le valideur attendu : « l'entité source » ne dit a personne s'il est concerne.
  const relevanceResponderLabel = useMemo(() => {
    const role = incident?.relevanceResponderRole;
    if (!role) {
      return t("incidents.workflow.modals.request_confirmation.responder_fallback");
    }
    return incident?.relevanceResponderTarget
      ? t(`incidents.validator_roles.${role}_named`, {
          name: incident.relevanceResponderTarget,
        })
      : t(`incidents.validator_roles.${role}`);
  }, [
    incident?.relevanceResponderRole,
    incident?.relevanceResponderTarget,
    t,
  ]);

  // Telecharge la fiche de traitement PDF de l'incident.
  const handleDownloadReport = useCallback(async () => {
    if (!incident?.id) return;
    setIsDownloadingReport(true);
    try {
      const blob = await incidentApi.downloadReport(incident.id);

      const fallbackName = incident.reference ?? "incident";
      const titleSlug = incident.title
        ? incident.title
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .trim()
            .replace(/[^a-z0-9]+/g, "-")
            .replace(/^-+|-+$/g, "")
        : fallbackName;
      downloadFile(
        blob,
        `${titleSlug || fallbackName}.pdf`,
        "application/pdf",
      );
    } catch {
      message.error(t("incidents.report.download_error"));
    } finally {
      setIsDownloadingReport(false);
    }
  }, [incident?.id, incident?.title, incident?.reference, message, t]);

  const isEditableStatus =
    incident &&
    ["PENDING_VALIDATION", "OPEN", "REOPENED"].includes(incident.status);

  const canEditIncident =
    incident &&
    isEditableStatus &&
    incident.createdBy.id === user?.id &&
    hasPermission(PERMISSIONS.INCIDENT.UPDATE);

  // En mode strict (si l'incident a depassé l'état OPEN/PENDING_VALIDATION),
  // on verrouille les champs structurels (Type, Criticité, etc.)
  const isStrictMode = incident
    ? !["OPEN", "PENDING_VALIDATION", "REOPENED"].includes(incident.status)
    : false;

  // Traite le retour a l'ecran precedent.
  const handleBack = useCallback(
    () => incidentNavigation.navigateToIncidents(navigate),
    [navigate],
  );

  // Traite la fin d'edition reussie.
  const handleEditSuccess = useCallback(() => {
    setIsEditing(false);
  }, []);

  // Traite l'annulation de l'edition.
  const handleEditCancel = useCallback(() => {
    setIsEditing(false);
  }, []);

  const breadcrumbItems = useMemo(
    () => [
      {
        title: t("incidents.title"),
        className: styles.breadcrumbItem,
        onClick: handleBack,
      },
      { title: incident?.title ?? "..." },
    ],
    [t, handleBack, incident?.title],
  );

  /** Label du champ causeDetail selon la cause sélectionnée */
  const causeDetailLabel = useMemo(() => {
    if (!incident?.cause) return t("incidents.form.labels.cause_detail_other");
    if (incident.cause === IncidentCause.HUMAN)
      return t("incidents.form.labels.cause_detail_human");
    if (incident.cause === IncidentCause.EXTERNAL)
      return t("incidents.form.labels.cause_detail_external");
    return t("incidents.form.labels.cause_detail_other");
  }, [incident, t]);

  /** Valeurs initiales mappées pour IncidentForm (mode edit) */
  const formInitialValues = useMemo(() => {
    if (!incident) return undefined;
    return {
      title: incident.title,
      description: incident.description,
      typeId: incident.type.id,
      criticality: incident.criticality,
      dueDate: incident.dueDate ? dayjs(incident.dueDate) : undefined,
      incidentDate: incident.incidentDate
        ? dayjs(incident.incidentDate)
        : undefined,
      observationDate: incident.observationDate
        ? dayjs(incident.observationDate)
        : undefined,
      cause: incident.cause ?? undefined,
      causeDetail: incident.causeDetail ?? undefined,
    };
  }, [incident]);

  const hasCauseSection = Boolean(incident?.cause || incident?.causeDetail);
  const hasResolutionSection = Boolean(
    incident?.treatmentDescription ||
      incident?.resolutionDescription ||
      incident?.closureDescription ||
      incident?.proposedSolution,
  );

  if (isLoading && !incident) {
    return (
      <PageContainer>
        <Breadcrumb
          className={styles.breadcrumb}
          items={[
            {
              title: t("incidents.title"),
              className: styles.breadcrumbItem,
              onClick: handleBack,
            },
            { title: "..." },
          ]}
        />
        <PageHeader
          title={t("incidents.form.titles.view")}
          onBack={handleBack}
        />
        <SplitLayout
          main={
            <SectionCard>
              <Skeleton active paragraph={{ rows: 10 }} />
            </SectionCard>
          }
          sidebar={
            <SectionCard>
              <Skeleton active paragraph={{ rows: 6 }} />
            </SectionCard>
          }
        />
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <PageLoader
        isLoading={isLoading}
        isError={isError}
        errorMessage={
          error instanceof Error ? error.message : t("common.unknown_error")
        }
      >
        {incident && (
          <>
            <Breadcrumb className={styles.breadcrumb} items={breadcrumbItems} />

            <PageHeader
              title={
                isEditing ? t("incidents.form.titles.edit") : incident.title
              }
              subtitle={
                isEditing ? incident.title : t("incidents.form.titles.view")
              }
              onBack={handleBack}
              titleTag={
                <>
                  {incident.reference && (
                    <Typography.Text code copyable>
                      {incident.reference}
                    </Typography.Text>
                  )}
                  <StatusTag
                    status={incident.status}
                    colorMap={STATUS_COLORS}
                    label={t(`incidents.status.${incident.status}`)}
                  />
                </>
              }
              actions={[
                // La fiche de traitement n'est disponible que pour un incident cloture.
                !isEditing && incident.status === "CLOSED" && (
                  <Button
                    key="report"
                    icon={actionIcons.download}
                    loading={isDownloadingReport}
                    onClick={handleDownloadReport}
                  >
                    {t("incidents.report.download")}
                  </Button>
                ),
                !isEditing && isEditableStatus && canEditIncident && (
                  <Button
                    key="edit"
                    icon={actionIcons.edit}
                    onClick={() => setIsEditing(true)}
                  >
                    {t("common.edit")}
                  </Button>
                ),
                isEditing && (
                  <Button
                    key="cancel"
                    icon={actionIcons.close}
                    onClick={handleEditCancel}
                  >
                    {t("common.cancel")}
                  </Button>
                ),
              ].filter(Boolean)}
            />

            {/* ── Actions workflow ── */}
            {id && !isEditing && (
              <WorkflowActions
                incidentId={incident.id}
                status={incident.status}
                currentAssigneeId={incident.assignedTo?.id ?? null}
                existingClosureDescription={
                  incident.resolutionDescription ?? null
                }
                currentServiceId={incident.transferredToService?.id ?? null}
                defaultServiceId={
                  incident.type.defaultTargetService?.id ?? null
                }
                defaultUserId={incident.type.defaultTargetUser?.id ?? null}
                creatorId={incident.createdBy?.id ?? null}
                agencyId={incident.agency?.id ?? null}
                agencyName={incident.agency?.name ?? null}
                creatorServiceId={incident.creatorServiceId ?? null}
                currentAgencyTargetId={
                  incident.transferredToAgency?.id ?? null
                }
                treaterRoles={incident.type?.treaterRoles}
                resolverRoles={incident.type?.resolverRoles}
                closerRoles={incident.type?.closerRoles}
                reopenerRoles={incident.type?.reopenerRoles}
                requiresCauseAnalysis={
                  incident.type?.requiresCauseAnalysis ?? false
                }
                existingCauseDetail={incident.causeDetail ?? null}
                isReopenExpired={incident.isReopenExpired ?? false}
                isMaxReopenReached={incident.isMaxReopenReached ?? false}
                reopenRejectorId={reopenRejectorId}
                canValidate={incident.canValidate ?? false}
                canCancel={incident.canCancel ?? false}
                canRequestConfirmation={
                  incident.canRequestConfirmation ?? false
                }
                canConfirmRelevance={incident.canConfirmRelevance ?? false}
                relevanceResponderRole={incident.relevanceResponderRole}
                relevanceResponderTarget={incident.relevanceResponderTarget}
                chosenTargetServiceId={
                  incident.transferredToService?.id ?? null
                }
                requiresDirectionValidation={
                  incident.type?.requiresDirectionValidation ?? false
                }
                currentTypeId={incident.type?.id ?? null}
                incidentTitle={incident.title ?? null}
                incidentDescription={incident.description ?? null}
                proposedSolution={incident.proposedSolution ?? null}
                directionRejectionReason={
                  incident.directionRejectionReason ?? null
                }
              />
            )}

            {/* ── Mode édition : formulaire inline ── */}
            {isEditing ? (
              <SectionCard>
                <IncidentForm
                  incidentId={incident.id}
                  initialValues={formInitialValues}
                  isStrictMode={isStrictMode}
                  currentServiceName={incident.transferredToService?.name}
                  incidentStatus={incident.status}
                  creatorId={incident.createdBy?.id}
                  onSuccess={handleEditSuccess}
                  onCancel={handleEditCancel}
                />
              </SectionCard>
            ) : (
              /* ── Mode consultation ── */
              <SplitLayout
                main={
                  <>
                    {/* ── Informations principales ── */}
                    <SectionCard className={styles.card}>
                      <Field label={t("incidents.form.labels.description")}>
                        <Text>{incident.description}</Text>
                      </Field>

                      {incident.proposedSolution && (
                        <Alert
                          type="info"
                          style={{ marginTop: 16 }}
                          message={
                            incident.status === IncidentStatus.DRAFT ||
                            incident.status === IncidentStatus.PENDING_VALIDATION
                              ? t("incidents.workflow.modals.submit_solution.proposedSolutionTitle")
                              : t("incidents.workflow.modals.submit_solution.treatmentSolutionTitle")
                          }
                          description={
                            <>
                              <Text style={{ whiteSpace: "pre-wrap" }}>
                                {incident.proposedSolution}
                              </Text>
                              {incident.estimatedResolutionHours != null && (
                                <Text
                                  strong
                                  style={{ display: "block", marginTop: 8 }}
                                >
                                  {t(
                                    "incidents.detail.estimated_in_proposal",
                                    {
                                      count:
                                        incident.estimatedResolutionHours,
                                    },
                                  )}
                                </Text>
                              )}
                              {id && (
                                <ResolutionAttachments
                                  incidentId={incident.id}
                                  category="SOLUTION"
                                />
                              )}
                            </>
                          }
                          showIcon
                        />
                      )}

                      {incident.directionRejectionReason && (
                        <Alert
                          type="warning"
                          style={{ marginTop: 16 }}
                          message={t("incidents.workflow.modals.direction_reject.directionRejectionReasonTitle")}
                          description={
                            <Text style={{ whiteSpace: "pre-wrap" }}>
                              {incident.directionRejectionReason}
                            </Text>
                          }
                          showIcon
                        />
                      )}

                      <Divider />

                      <Row gutter={[24, 24]}>
                        <Col xs={24} sm={12}>
                          <Field label={t("incidents.form.labels.type")}>
                            <Text>{incident.type?.displayName ?? "-"}</Text>
                          </Field>
                        </Col>
                        <Col xs={24} sm={12}>
                          <Field label={t("incidents.form.labels.criticality")}>
                            <StatusTag
                              status={incident.criticality}
                              colorMap={CRITICALITY_COLORS}
                              label={t(
                                `incidents.criticality.${incident.criticality}`,
                              )}
                            />
                          </Field>
                        </Col>
                        {incident.status === "PENDING_VALIDATION" &&
                          incident.expectedValidatorRole && (
                            <Col xs={24} sm={12}>
                              <Field
                                label={t(
                                  "incidents.form.labels.validator_required",
                                )}
                              >
                                <StatusTag
                                  status="DEFAULT"
                                  colorMap={{ DEFAULT: "purple" }}
                                  label={
                                    incident.expectedValidatorTarget
                                      ? t(
                                        `incidents.validator_roles.${incident.expectedValidatorRole}_named`,
                                        {
                                          name: incident.expectedValidatorTarget,
                                        },
                                      )
                                      : t(
                                        `incidents.validator_roles.${incident.expectedValidatorRole}`,
                                      )
                                  }
                                />
                              </Field>
                            </Col>
                          )}
                        {(incident.status === "OPEN" ||
                          incident.status === "PENDING_VALIDATION") &&
                          (incident.type.defaultTargetService ||
                            incident.type.defaultTargetUser) && (
                            <Col xs={24} sm={12}>
                              <Field
                                label={t(
                                  "incidents.form.labels.default_target",
                                )}
                              >
                                <Text>
                                  {incident.type.defaultTargetService
                                    ? incident.type.defaultTargetService.name
                                    : incident.type.defaultTargetUser
                                      ? formatUserName(
                                        incident.type.defaultTargetUser,
                                      )
                                      : ""}
                                </Text>
                              </Field>
                            </Col>
                          )}
                        <Col xs={24} sm={12}>
                          <Field label={t("incidents.form.labels.due_date")}>
                            <Text>
                              {incident.dueDate
                                ? formatDateTime(incident.dueDate)
                                : t("incidents.detail.no_due_date")}
                            </Text>
                          </Field>
                        </Col>
                        {incident.estimatedResolutionHours != null && (
                          <Col xs={24} sm={12}>
                            <Field
                              label={t(
                                "incidents.form.labels.estimated_resolution_hours",
                              )}
                            >
                              <Text>
                                {t("incidents.detail.estimated_hours_value", {
                                  count: incident.estimatedResolutionHours,
                                })}
                              </Text>
                            </Field>
                          </Col>
                        )}
                        <Col xs={24} sm={12}>
                          <Field label={t("incidents.form.labels.incident_date")}>
                            <Text>
                              {incident.incidentDate
                                ? formatDate(incident.incidentDate)
                                : "-"}
                            </Text>
                          </Field>
                        </Col>
                        <Col xs={24} sm={12}>
                          <Field label={t("incidents.form.labels.observation_date")}>
                            <Text>
                              {incident.observationDate
                                ? formatDate(incident.observationDate)
                                : "-"}
                            </Text>
                          </Field>
                        </Col>
                        <Col xs={24} sm={12}>
                          <Field label={t("incidents.form.labels.created_by")}>
                            <Text>{formatUserName(incident.createdBy)}</Text>
                          </Field>
                        </Col>
                        <Col xs={24} sm={12}>
                          <Field label={t("incidents.form.labels.agency")}>
                            <Text>{incident.agency.name}</Text>
                          </Field>
                        </Col>
                        {incident.transferredToAgency && (
                          <Col xs={24} sm={12}>
                            <Field
                              label={t(
                                "incidents.form.labels.transferred_to_agency",
                              )}
                            >
                              <Text>{incident.transferredToAgency.name}</Text>
                            </Field>
                          </Col>
                        )}
                        {incident.assignedTo && (
                          <Col xs={24} sm={12}>
                            <Field
                              label={t("incidents.form.labels.assigned_to")}
                            >
                              <Text>{`${incident.assignedTo.firstName} ${incident.assignedTo.lastName}`}</Text>
                            </Field>
                          </Col>
                        )}
                        {incident.transferredToService && (
                          <Col xs={24} sm={12}>
                            <Field
                              label={t(
                                "incidents.form.labels.transferred_to_service",
                              )}
                            >
                              <Text>{incident.transferredToService.name}</Text>
                            </Field>
                          </Col>
                        )}
                        {incident.transferReason && (
                          <Col xs={24}>
                            <Field
                              label={t("incidents.form.labels.transfer_reason")}
                            >
                              <Text>{incident.transferReason}</Text>
                            </Field>
                          </Col>
                        )}
                      </Row>
                    </SectionCard>

                    {/* ── Motif de rejet mis en valeur ── */}
                    {incident.rejectReason && (
                      <Alert
                        type="error"
                        showIcon
                        className={styles.rejectAlert}
                        message={t("incidents.form.labels.reject_reason")}
                        description={
                          <span className={styles.preWrap}>
                            {incident.rejectReason}
                          </span>
                        }
                      />
                    )}

                    {/* ── Confirmation d'actualité en attente de réponse ── */}
                    {incident.confirmationRequestedAt && (
                      <Alert
                        type="info"
                        showIcon
                        className={styles.rejectAlert}
                        message={t(
                          "incidents.workflow.pending_confirmation.title",
                        )}
                        description={t(
                          "incidents.workflow.pending_confirmation.description",
                          {
                            date: formatDateTime(
                              incident.confirmationRequestedAt,
                            ),
                            responder: relevanceResponderLabel,
                          },
                        )}
                      />
                    )}

                    {/* ── Motif d'annulation : decision distincte du rejet ── */}
                    {incident.cancelReason && (
                      <Alert
                        type="warning"
                        showIcon
                        className={styles.rejectAlert}
                        message={t(
                          "incidents.workflow.modals.cancel.reason_label",
                        )}
                        description={
                          <span className={styles.preWrap}>
                            {incident.cancelReason}
                          </span>
                        }
                      />
                    )}

                    {/* ── Carte : Analyse des causes ── */}
                    {hasCauseSection && (
                      <SectionCard
                        className={styles.card}
                        title={t("incidents.detail.sections.cause")}
                      >
                        <Row gutter={[24, 24]}>
                          {incident.cause && (
                            <Col xs={24} sm={12}>
                              <Field label={t("incidents.form.labels.cause")}>
                                <Text>
                                  {t(
                                    `incidents.cause_values.${incident.cause}`,
                                  )}
                                </Text>
                              </Field>
                            </Col>
                          )}
                          {incident.causeDetail && (
                            <Col xs={24} sm={incident.cause ? 12 : 24}>
                              <Field label={causeDetailLabel}>
                                <Text>{incident.causeDetail}</Text>
                              </Field>
                            </Col>
                          )}
                        </Row>
                      </SectionCard>
                    )}

                    {/* ── Carte : Résolution / clôture ── */}
                    {hasResolutionSection && (
                      <SectionCard
                        className={styles.card}
                        title={t("incidents.detail.sections.resolution")}
                      >
                        {incident.treatmentDescription && (
                          <Field label={t("incidents.detail.treatment_report")}>
                            <Text className={styles.preWrap}>
                              {incident.treatmentDescription}
                            </Text>
                          </Field>
                        )}
                        {id && (
                          <ResolutionAttachments
                            incidentId={incident.id}
                            category="TREATMENT"
                          />
                        )}
                        {incident.resolutionDescription && (
                          <Field
                            label={t("incidents.detail.resolution_report")}
                          >
                            <Text className={styles.preWrap}>
                              {incident.resolutionDescription}
                            </Text>
                          </Field>
                        )}
                        {id && (
                          <ResolutionAttachments
                            incidentId={incident.id}
                            category="RESOLUTION"
                          />
                        )}
                        {id && (
                          <ResolutionAttachments
                            incidentId={incident.id}
                            category="UNRESOLVED"
                          />
                        )}
                        {incident.reopenReason && (
                          <Field label={t("incidents.form.labels.reopen_reason")}>
                            <Text className={styles.preWrap}>{incident.reopenReason}</Text>
                          </Field>
                        )}
                        {incident.blockedReason && (
                          <Field label={t("incidents.form.labels.blocked_reason")}>
                            <Text className={styles.preWrap}>{incident.blockedReason}</Text>
                          </Field>
                        )}
                        {incident.closureDescription && (
                          <Field label={t("incidents.detail.closure_note")}>
                            <Text className={styles.preWrap}>
                              {incident.closureDescription}
                            </Text>
                          </Field>
                        )}
                        {id && (
                          <ResolutionAttachments
                            incidentId={incident.id}
                            category="CLOSURE"
                          />
                        )}
                      </SectionCard>
                    )}

                    {!hasResolutionSection && id && (
                      <>
                        <ResolutionAttachments
                          incidentId={incident.id}
                          category="TREATMENT"
                        />
                        <ResolutionAttachments
                          incidentId={incident.id}
                          category="RESOLUTION"
                        />
                        <ResolutionAttachments
                          incidentId={incident.id}
                          category="UNRESOLVED"
                        />
                        <ResolutionAttachments
                          incidentId={incident.id}
                          category="CLOSURE"
                        />
                      </>
                    )}

                    {id && (
                      <ResolutionAttachments
                        incidentId={incident.id}
                        category="CANCELLATION"
                      />
                    )}

                    {id && (
                      <AttachmentsSection
                        incidentId={incident.id}
                        incidentStatus={incident.status}
                        creatorId={incident.createdBy?.id ?? null}
                      />
                    )}
                    {id && (
                      <CommentsSection
                        incidentId={incident.id}
                        incidentStatus={incident.status}
                      />
                    )}
                  </>
                }
                sidebar={
                  <>
                    {/* ── Dates clés ── */}
                    <SectionCard
                      className={styles.card}
                      title={t("incidents.detail.sections.key_dates")}
                    >
                      <Field label={t("incidents.form.labels.created_at")}>
                        <Text>{formatDateTime(incident.createdAt)}</Text>
                      </Field>
                      <Field label={t("incidents.form.labels.updated_at")}>
                        <Text>{formatDateTime(incident.updatedAt)}</Text>
                      </Field>
                      {incident.validatedAt && (
                        <Field label={t("incidents.form.labels.validated_at")}>
                          <Text>{formatDateTime(incident.validatedAt)}</Text>
                        </Field>
                      )}
                      {incident.transferredAt && (
                        <Field
                          label={t("incidents.form.labels.transferred_at")}
                        >
                          <Text>{formatDateTime(incident.transferredAt)}</Text>
                        </Field>
                      )}
                      {incident.resolvedAt && (
                        <Field label={t("incidents.form.labels.resolved_at")}>
                          <Text>{formatDateTime(incident.resolvedAt)}</Text>
                        </Field>
                      )}
                      {incident.closedAt && (
                        <Field label={t("incidents.form.labels.closed_at")}>
                          <Text>{formatDateTime(incident.closedAt)}</Text>
                        </Field>
                      )}
                      {incident.blockedAt && (
                        <Field label={t("incidents.form.labels.blocked_at")}>
                          <Text>{formatDateTime(incident.blockedAt)}</Text>
                        </Field>
                      )}
                      {incident.reopenedAt && (
                        <Field label={t("incidents.form.labels.reopened_at")}>
                          <Text>{formatDateTime(incident.reopenedAt)}</Text>
                        </Field>
                      )}
                    </SectionCard>

                    {id && <HistoryTimeline incidentId={incident.id} />}
                  </>
                }
              />
            )}
          </>
        )}
      </PageLoader>
    </PageContainer>
  );
});

ViewIncident.displayName = "ViewIncident";

export default ViewIncident;
