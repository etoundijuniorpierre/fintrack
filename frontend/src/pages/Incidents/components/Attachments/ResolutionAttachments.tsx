// Pieces jointes de traitement / resolution / cloture, affichees dans la section Resolution.
import { memo, useMemo } from "react";
import { Collapse, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { useIncidentAttachments } from "../../../../hooks/document/useDocuments";
import CommentAttachments from "../Comments/CommentAttachments";
import styles from "./ResolutionAttachments.module.scss";
import {
  useIncident,
  useIncidentHistory,
} from "../../../../hooks/incident/useIncidents/useIncidents";
import { IncidentStatus } from "../../../../api/incident";
import type { AttachmentResponse } from "../../../../api/document/types";

const { Text } = Typography;

type ResolutionCategory =
  | "SOLUTION"
  | "TREATMENT"
  | "RESOLUTION"
  | "UNRESOLVED"
  | "CLOSURE"
  | "CANCELLATION";

interface ResolutionAttachmentsProps {
  incidentId: string;
  category?: ResolutionCategory;
}

// Cycle de vie : un incident reouvert repart a zero. Le rang du cycle d'une PJ se
// deduit de sa date de depot face aux reouvertures successives ; rien n'est duplique
// en base et les PJ deja stockees sont classees retroactivement.
const cycleOf = (uploadedAt: string, reopenBoundaries: number[]) => {
  const uploadedTime = new Date(uploadedAt).getTime();
  return reopenBoundaries.filter((boundary) => boundary < uploadedTime).length;
};

// Rend les pièces jointes de procédure, de traitement, de résolution ou de clôture
// selon la catégorie demandée, en isolant celles des cycles antérieurs.
const ResolutionAttachments = memo(
  ({ incidentId, category }: ResolutionAttachmentsProps) => {
    const { t } = useTranslation();
    const { data: incident } = useIncident(incidentId);
    const { data: attachments = [] } = useIncidentAttachments(incidentId);
    const { data: history = [] } = useIncidentHistory(incidentId);

    const reopenBoundaries = useMemo(
      () =>
        history
          .filter((entry) => entry.action === "REOPENING")
          .map((entry) => new Date(entry.createdAt).getTime())
          .sort((a, b) => a - b),
      [history],
    );

    const currentCycle = reopenBoundaries.length;

    // Pour chaque catégorie : les PJ du cycle courant, et celles des cycles antérieurs
    // regroupées par rang décroissant (la réouverture la plus récente d'abord).
    const groupsByCategory = useMemo(() => {
      const build = (wanted: ResolutionCategory) => {
        const files = attachments.filter(
          (attachment) => attachment.category === wanted,
        );
        const current: AttachmentResponse[] = [];
        const previous = new Map<number, AttachmentResponse[]>();

        files.forEach((file) => {
          const cycle = cycleOf(file.uploadedAt, reopenBoundaries);
          if (cycle >= currentCycle) {
            current.push(file);
            return;
          }
          const bucket = previous.get(cycle) ?? [];
          bucket.push(file);
          previous.set(cycle, bucket);
        });

        return {
          current,
          previous: [...previous.entries()].sort(([a], [b]) => b - a),
          total: files.length,
        };
      };

      return {
        SOLUTION: build("SOLUTION"),
        TREATMENT: build("TREATMENT"),
        RESOLUTION: build("RESOLUTION"),
        UNRESOLVED: build("UNRESOLVED"),
        CLOSURE: build("CLOSURE"),
        CANCELLATION: build("CANCELLATION"),
      };
    }, [attachments, reopenBoundaries, currentCycle]);

    // La proposition de solution change de libellé tant que l'incident n'est pas pris en charge.
    const solutionLabel =
      incident?.status === IncidentStatus.DRAFT ||
      incident?.status === IncidentStatus.PENDING_VALIDATION
        ? t("incidents.attachments.proposedSolutionTitle")
        : t("incidents.attachments.treatmentSolutionTitle");

    const labels: Record<ResolutionCategory, string> = {
      SOLUTION: solutionLabel,
      TREATMENT: t("incidents.attachments.treatment_title"),
      RESOLUTION: t("incidents.attachments.resolution_title"),
      UNRESOLVED: t("incidents.attachments.unresolved_title"),
      CLOSURE: t("incidents.attachments.closure_title"),
      CANCELLATION: t("incidents.attachments.cancellation_title"),
    };

    const visibleCategories = (
      [
        "SOLUTION",
        "TREATMENT",
        "RESOLUTION",
        "UNRESOLVED",
        "CLOSURE",
        "CANCELLATION",
      ] as const
    ).filter(
      (candidate) =>
        (!category || category === candidate) &&
        groupsByCategory[candidate].total > 0,
    );

    if (visibleCategories.length === 0) return null;

    return (
      <div className={styles.wrapper}>
        {visibleCategories.map((visible) => {
          const group = groupsByCategory[visible];
          return (
            <div key={visible} className={styles.group}>
              <Text type="secondary" className={styles.label}>
                {labels[visible]}
              </Text>
              {group.current.length > 0 && (
                <CommentAttachments attachments={group.current} />
              )}
              {group.previous.length > 0 && (
                <Collapse
                  ghost
                  size="small"
                  className={styles.previousCycles}
                  items={group.previous.map(([cycle, files]) => ({
                    key: String(cycle),
                    label: t("incidents.attachments.previous_cycle", {
                      cycle: cycle + 1,
                    }),
                    children: <CommentAttachments attachments={files} />,
                  }))}
                />
              )}
            </div>
          );
        })}
      </div>
    );
  },
);

ResolutionAttachments.displayName = "ResolutionAttachments";

export default ResolutionAttachments;
