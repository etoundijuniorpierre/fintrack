// Modal de detail d'un enregistrement d'audit : affiche tous les champs et le JSON des details.
import { memo, useCallback } from "react";
import { Descriptions, Tag, Space, Button } from "antd";
import { LinkOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import Modal from "../../../../components/ui/Modal/Modal";
import type { AuditLogResponse } from "../../../../api/audit";
import { getAuditStatusColor } from "../../../../api/audit";
import {
  formatDate,
  formatDateTime,
  formatUserName,
  isUuid,
  humanizeString,
} from "../../../../utils/formatters/formatters";
import { ROLE_NAMES, normalizeRoleName } from "../../../../utils/roles/roles";
import { resourceNavigation } from "../../../../utils/navigation";
import styles from "./AuditDetailModal.module.scss";

// Definit les proprietes attendues par le composant AuditDetailModal.
interface AuditDetailModalProps {
  open: boolean;
  record: AuditLogResponse | null;
  onClose: () => void;
}

// Type guard pour verifier si un objet est un diff (structure { from: X, to: Y })
function isDiffObject(
  details: unknown,
): details is Record<string, { from: unknown; to: unknown }> {
  if (!details || typeof details !== "object") return false;
  const entries = Object.entries(details);
  if (entries.length === 0) return false;
  return entries.every(
    ([_, value]) =>
      value && typeof value === "object" && "from" in value && "to" in value,
  );
}

// Certains diffs arrivent enveloppes : { applied: <etat complet>, delta: { cle: {from,to} } }.
// `applied` est l'etat resultant, redondant avec le delta ; seul le changement interesse
// le lecteur. Sans ce deballage, la modale n'affichait que « applied / delta » sans
// pouvoir dire quel parametre avait bouge ni de quelle valeur vers quelle valeur.
function extractDiff(
  details: unknown,
): Record<string, { from: unknown; to: unknown }> | null {
  if (isDiffObject(details)) return details;
  if (!details || typeof details !== "object") return null;
  const delta = (details as Record<string, unknown>).delta;
  return isDiffObject(delta) ? delta : null;
}

// Type de ressource des enregistrements de configuration systeme : c'est lui qui
// autorise a lire une clef comme un seuil ou un reglage e-mail, plutot que de deviner
// a partir du nom (« reason » n'est pas un seuil).
const SYSTEM_SETTINGS_RESOURCE = "system_settings";

// Clefs de reglage e-mail : emailNotif.<EVENEMENT>.enabled|excluded
const EMAIL_SETTING_KEY = /^emailNotif\.([A-Z_]+)\.(enabled|excluded)$/;

// Libelle d'une clef de configuration systeme (seuil ou reglage e-mail).
function getSettingLabel(
  key: string,
  t: (key: string, options?: Record<string, unknown>) => string,
): string {
  const emailMatch = EMAIL_SETTING_KEY.exec(key);
  if (emailMatch) {
    const [, event, facet] = emailMatch;
    const eventLabel = t(
      `superAdmin.config.emailNotifications.events.${event}`,
      { defaultValue: humanizeString(event) },
    );
    return `${eventLabel} — ${t(`audit.detail.emailFacet.${facet}`)}`;
  }
  return t(`superAdmin.config.thresholdLabels.${key}`, {
    defaultValue: humanizeString(key),
  });
}

// Recupere un libelle lisible et traduit pour une cle de champ.
function getFieldLabel(
  key: string,
  t: (key: string, options?: Record<string, unknown>) => string,
  isSystemSetting = false,
): string {
  if (isSystemSetting) {
    return getSettingLabel(key, t);
  }
  switch (key) {
    case "title":
      return t("incidents.form.labels.title");
    case "description":
      return t("incidents.form.labels.description");
    case "criticality":
      return t("incidents.form.labels.criticality");
    case "status":
      return t("incidents.form.labels.status");
    case "incidentDate":
      return t("incidents.form.labels.incident_date");
    case "observationDate":
      return t("incidents.form.labels.observation_date");
    case "dueDate":
      return t("incidents.form.labels.due_date");
    case "validatedAt":
      return t("incidents.form.labels.validated_at");
    case "reopenReason":
      return t("incidents.workflow.modals.reopen.reason_label");
    case "rejectReason":
      return t("incidents.form.labels.reject_reason");
    case "cancelReason":
      return t("incidents.workflow.modals.cancel.reason_label");
    case "transferReason":
      return t("incidents.form.labels.transfer_reason");
    case "comment":
      return t("incidents.comments.form.label");
    case "username":
      return t("users.form.labels.username");
    case "email":
      return t("users.form.labels.email");
    case "firstName":
      return t("users.form.labels.firstname");
    case "lastName":
      return t("users.form.labels.lastname");
    case "phone":
    case "phoneNumber":
      return t("users.form.labels.phone");
    case "active":
      return t("users.table.status");
    case "roles":
      return t("users.table.roles");
    case "agency":
      return t("users.table.agency");
    case "service":
      return t("users.table.service");
    default: {
      return humanizeString(key);
    }
  }
}

// Formate une valeur de champ pour qu'elle soit humaine et comprehensible.
function formatDetailValue(
  key: string,
  value: unknown,
  t: (key: string, options?: Record<string, unknown>) => string,
): string | null {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  // Filtrer les UUIDs (non informatifs pour un non-technicien)
  if (isUuid(value)) {
    return null;
  }

  if (typeof value === "boolean") {
    if (key === "active") {
      return value
        ? t("users.table.status_active")
        : t("users.table.status_inactive");
    }
    return value ? t("common.yes") : t("common.no");
  }

  if (key === "criticality") {
    return t(`incidents.criticality.${value}`, { defaultValue: String(value) });
  }

  if (key === "status") {
    return t(`incidents.status.${value}`, { defaultValue: String(value) });
  }

  if (
    ["incidentDate", "observationDate", "dueDate", "validatedAt"].includes(key)
  ) {
    try {
      const d = new Date(String(value));
      if (!isNaN(d.getTime())) {
        return formatDate(d);
      }
    } catch {
      // Ignorer l'erreur
    }
  }

  if (key === "roles" && Array.isArray(value)) {
    const validRoleValues = Object.values(ROLE_NAMES) as string[];
    // Prepare l'affichage des roles associes.
    const userRoles = value
      .map((r) => normalizeRoleName(String(r)))
      .filter((r) => validRoleValues.includes(r));
    return userRoles.length > 0
      ? userRoles.map((r) => t(`users.roles.${r}`)).join(", ")
      : null;
  }

  // Listes d'exclusion et autres collections : on annonce le volume plutot que
  // de deverser des identifiants illisibles.
  if (Array.isArray(value)) {
    return value.length === 0
      ? t("audit.detail.emptyList")
      : t("audit.detail.itemCount", { count: value.length });
  }

  // Un objet restant n'a pas de rendu utile : mieux vaut ne rien afficher
  // que « [object Object] ».
  if (typeof value === "object") {
    return null;
  }

  return String(value);
}

// Rend le composant AuditDetailModal pour l'interface audit detail modale.
const AuditDetailModal = memo(
  ({ open, record, onClose }: AuditDetailModalProps) => {
    const { t } = useTranslation();
    const navigate = useNavigate();

    // Traite la fermeture.
    const handleClose = useCallback(() => onClose(), [onClose]);

    const diff = record ? extractDiff(record.details) : null;
    const isSystemSetting =
      record?.resourceType === SYSTEM_SETTINGS_RESOURCE;

    return (
      <Modal
        open={open}
        title={t("audit.detail.title")}
        onClose={handleClose}
        cancelText={t("audit.close")}
        footer={true}
        width={700}
        destroyOnHidden
      >
        {record && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label={t("audit.detail.id")}>
              {record.id}
            </Descriptions.Item>
            <Descriptions.Item label={t("audit.detail.timestamp")}>
              {formatDateTime(record.timestamp)}
            </Descriptions.Item>
            <Descriptions.Item label={t("audit.detail.username")}>
              {record.user?.username || record.username || "—"}
            </Descriptions.Item>
            <Descriptions.Item label={t("audit.detail.userId")}>
              {record.user?.id ?? "-"}
            </Descriptions.Item>
            {record.user && (
              <Descriptions.Item label={t("audit.detail.user")}>
                <Space>
                  {formatUserName(
                    record.user,
                    record.username,
                    t("audit.table.unknownUser"),
                  )}
                  {record.user?.id && (
                    <Button
                      type="link"
                      size="small"
                      icon={<LinkOutlined />}
                      onClick={() => {
                        resourceNavigation.navigateToResourceDetail(
                          navigate,
                          "USER",
                          record.user!.id,
                          { onClose },
                        );
                      }}
                      title={t("audit.table.goToUser")}
                    />
                  )}
                </Space>
              </Descriptions.Item>
            )}
            <Descriptions.Item label={t("audit.detail.roles")}>
              {(() => {
                if (!record?.roles) return "-";
                const validRoleValues = Object.values(ROLE_NAMES) as string[];
                // Prepare l'affichage des roles associes.
                const userRoles = record.roles
                  .map((r) => normalizeRoleName(r))
                  .filter((r) => validRoleValues.includes(r));
                return userRoles.length > 0
                  ? userRoles.map((r) => t(`users.roles.${r}`)).join(", ")
                  : "-";
              })()}
            </Descriptions.Item>
            <Descriptions.Item label={t("audit.detail.action")}>
              {t(`audit.action.${record.action}`, record.action)}
            </Descriptions.Item>
            <Descriptions.Item label={t("audit.detail.resourceType")}>
              {record.resourceType}
            </Descriptions.Item>
            <Descriptions.Item label={t("audit.detail.resourceId")}>
              {record.resourceId && record.resourceId !== "-" ? (
                <Space>
                  {record.resourceId}
                  {resourceNavigation.getResourceDetailPath(
                    record.resourceType,
                    record.resourceId,
                  ) !== "" && (
                    <Button
                      type="link"
                      size="small"
                      icon={<LinkOutlined />}
                      onClick={() => {
                        resourceNavigation.navigateToResourceDetail(
                          navigate,
                          record.resourceType,
                          record.resourceId,
                          { onClose },
                        );
                      }}
                      title={t("audit.table.goToResource")}
                    />
                  )}
                </Space>
              ) : (
                "-"
              )}
            </Descriptions.Item>
            <Descriptions.Item label={t("audit.detail.ipAddress")}>
              {record.ipAddress ?? "-"}
            </Descriptions.Item>
            <Descriptions.Item label={t("audit.detail.userAgent")}>
              {record.userAgent ?? "-"}
            </Descriptions.Item>
            <Descriptions.Item label={t("audit.detail.status")}>
              <Tag color={getAuditStatusColor(record.status)}>
                {t(`audit.status.${record.status}`)}
              </Tag>
            </Descriptions.Item>
            {diff ? (
              <Descriptions.Item label={t("audit.detail.changes")}>
                <div className={styles.diffContainer}>
                  {Object.entries(diff).map(([field, change]) => {
                    const label = getFieldLabel(field, t, isSystemSetting);
                    const fromVal =
                      formatDetailValue(field, change.from, t) ??
                      t("audit.detail.emptyValue");
                    const toVal =
                      formatDetailValue(field, change.to, t) ??
                      t("audit.detail.emptyValue");
                    return (
                      <div key={field} className={styles.diffRow}>
                        <strong className={styles.diffField}>{label}</strong> :{" "}
                        <del className={styles.diffFrom}>{fromVal}</del> &rarr;{" "}
                        <ins className={styles.diffTo}>{toVal}</ins>
                      </div>
                    );
                  })}
                </div>
              </Descriptions.Item>
            ) : record.details ? (
              <Descriptions.Item label={t("audit.detail.details")}>
                <div className={styles.detailsList}>
                  {Object.entries(
                    record.details as Record<string, unknown>,
                  ).map(([key, value]) => {
                    // Formate ted label pour l'interface.
                    const formattedLabel = getFieldLabel(key, t, isSystemSetting);
                    // Formate ted val pour l'interface.
                    const formattedVal = formatDetailValue(key, value, t);
                    if (formattedVal === null) return null;
                    return (
                      <div key={key} className={styles.detailRow}>
                        <strong className={styles.detailKey}>
                          {formattedLabel} :{" "}
                        </strong>
                        <span className={styles.detailValue}>
                          {formattedVal}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </Descriptions.Item>
            ) : null}
          </Descriptions>
        )}
      </Modal>
    );
  },
);

AuditDetailModal.displayName = "AuditDetailModal";

export default AuditDetailModal;
