// Chronologie visuelle retracant le cycle de vie d'un incident.

import { Timeline, Typography, Tag, Empty, Divider } from "antd";
import { Trans, useTranslation } from "react-i18next";
import { memo, useMemo, useCallback, type ReactElement } from "react";
import {
  useIncidentHistory,
  useActionTypes,
} from "../../../../hooks/incident/useIncidents/useIncidents";
import type { IncidentHistoryResponse } from "../../../../api/incident/types";
import {
  ACTION_TYPE_COLORS,
  STATUS_COLORS,
} from "../../../../api/incident/types";
import {
  Criticality,
  IncidentStatus,
} from "../../../../api/incident/enums/enums";
import {
  formatDateTime,
  formatUserName,
} from "../../../../utils/formatters/formatters";
import styles from "./HistoryTimeline.module.scss";

const { Text } = Typography;

const INCIDENT_STATUS_CODES = new Set<string>(Object.values(IncidentStatus));
const CRITICALITY_CODES = new Set<string>(Object.values(Criticality));

const SENTENCE_PREFIX = "incidents.history.sentence";

// Marqueur de repli de formatUserName quand aucun utilisateur n'est present.
const NO_USER = "—";

// Actions dont le commentaire est une note descriptive plutot qu'un motif justificatif.
const NOTE_ACTIONS = new Set<string>(["CREATION", "COMMENT"]);

// Une valeur d'historique resolue : soit un statut connu (colore), soit un texte libre.
interface ResolvedValue {
  label: string;
  status: IncidentStatus | null;
}

// Definit les proprietes attendues par le composant HistoryTimeline.
interface HistoryTimelineProps {
  incidentId: string;
}

// Rend le composant HistoryTimeline pour l'interface history timeline.
const HistoryTimeline = memo(({ incidentId }: HistoryTimelineProps) => {
  const { t, i18n } = useTranslation();

  const { data: history = [], isLoading } = useIncidentHistory(incidentId);
  const { data: actionTypes = [] } = useActionTypes();

  // Construit une table de correspondance code -> libelle
  const actionTypeMap = useMemo(
    () =>
      Object.fromEntries(
        actionTypes.map((a) => {
          const key = `incidents.action_type.${a.code}`;
          const translated = t(key, a.name);
          return [a.code, translated === key ? a.name : translated];
        }),
      ),
    [actionTypes, t],
  );

  // Resout une valeur : traduit et colore les statuts connus, laisse le reste tel quel.
  const resolveValue = useCallback(
    (value: string | null | undefined): ResolvedValue | null => {
      if (!value) return null;
      if (INCIDENT_STATUS_CODES.has(value)) {
        const key = `incidents.status.${value}`;
        const translated = t(key);
        return {
          label: translated === key ? value : translated,
          status: value as IncidentStatus,
        };
      }
      // Les entrees CRITICALITY_CHANGE portent un code de criticite, pas un statut.
      if (CRITICALITY_CODES.has(value)) {
        const key = `incidents.criticality.${value}`;
        const translated = t(key);
        return { label: translated === key ? value : translated, status: null };
      }
      return { label: value, status: null };
    },
    [t],
  );

  // Determine l'auteur de l'action : l'utilisateur, ou « Le systeme » pour les actions
  // automatiques (aucun utilisateur associe). On affiche toujours un auteur.
  const resolveActor = useCallback(
    (entry: IncidentHistoryResponse): string => {
      const name = formatUserName(entry.user, entry.user?.username);
      return name && name !== NO_USER
        ? name
        : t("incidents.history.system_actor");
    },
    [t],
  );

  // Construit une pastille pour une valeur : couleur de statut ou pastille neutre.
  const valuePill = useCallback(
    (value: ResolvedValue | null): ReactElement =>
      value?.status ? (
        <Tag color={STATUS_COLORS[value.status]} className={styles.valueTag} />
      ) : (
        <Tag className={styles.valuePillNeutral} />
      ),
    [],
  );

  // Choisit la clef de phrase la plus specifique disponible, avec repli generique.
  // Variante selon les valeurs presentes : « » (depart+arrivee), « _to », « _plain ».
  const resolveSentenceKey = useCallback(
    (action: string, hasFrom: boolean, hasTo: boolean): string | null => {
      const variant = hasFrom && hasTo ? "" : hasTo ? "_to" : "_plain";
      const specific = `${SENTENCE_PREFIX}.${action}${variant}`;
      if (i18n.exists(specific)) return specific;
      // Pas de phrase dediee : on ne force un repli que s'il reste des valeurs a montrer.
      if (variant === "_plain") return null;
      return `${SENTENCE_PREFIX}.fallback${variant}`;
    },
    [i18n],
  );

  const sortedHistory = useMemo(
    () =>
      [...history].sort(
        (a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
      ),
    [history],
  );

  const timelineItems = useMemo(
    () =>
      sortedHistory.map((entry: IncidentHistoryResponse) => {
        const fromValue = resolveValue(entry.oldValue);
        const toValue = resolveValue(entry.newValue);
        // Une transition n'a de sens que si depart et arrivee different.
        const hasFrom = !!fromValue && fromValue.label !== toValue?.label;
        const hasTo = !!toValue;
        const actor = resolveActor(entry);
        const sentenceKey = resolveSentenceKey(entry.action, hasFrom, hasTo);
        // Une transition derivee par le workflow n'a pas d'auteur : son commentaire
        // explique ce qui s'est passe, il ne justifie pas une decision humaine.
        const isSystemEntry = !entry.user;
        const reasonLabelKey =
          isSystemEntry || NOTE_ACTIONS.has(entry.action)
            ? "incidents.history.note_label"
            : "incidents.history.reason_label";

        return {
          color: ACTION_TYPE_COLORS[entry.action] ?? "blue",
          children: (
            <div className={styles.entryContent}>
              <div className={styles.sentence}>
                {/* L'auteur est toujours affiche, en tete : humain ou « Le systeme ». */}
                <Text strong className={styles.actor}>
                  {actor}
                </Text>{" "}
                {sentenceKey ? (
                  <Trans
                    i18nKey={sentenceKey}
                    values={{
                      from: hasFrom ? fromValue.label : undefined,
                      to: toValue?.label,
                    }}
                    components={{
                      from: valuePill(fromValue),
                      to: valuePill(toValue),
                    }}
                  />
                ) : (
                  // Action inconnue sans valeur : libelle brut de l'action.
                  <>— {actionTypeMap[entry.action] ?? entry.action}</>
                )}
              </div>
              <Text type="secondary" className={styles.entryMeta}>
                {formatDateTime(entry.createdAt)}
              </Text>
              {entry.comment && (
                <div className={styles.reason}>
                  <Text type="secondary" className={styles.reasonLabel}>
                    {t(reasonLabelKey)}
                  </Text>
                  <Text className={styles.reasonText}>{entry.comment}</Text>
                </div>
              )}
            </div>
          ),
        };
      }),
    [
      sortedHistory,
      actionTypeMap,
      resolveValue,
      resolveActor,
      resolveSentenceKey,
      valuePill,
      t,
    ],
  );

  return (
    <div className={styles.historyTimeline}>
      <Divider titlePlacement="left" className={styles.sectionDivider}>
        <Text strong>{t("incidents.history.title")}</Text>
      </Divider>

      {!isLoading && sortedHistory.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={t("incidents.history.no_history")}
        />
      ) : (
        <Timeline items={timelineItems} className={styles.timeline} />
      )}
    </div>
  );
});

export default HistoryTimeline;
