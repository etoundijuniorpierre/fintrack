// Liste de repartition par type d'incident avec barres de progression.
import { memo, useMemo } from "react";
import { Skeleton, Empty, Typography, Progress } from "antd";
import { useTranslation } from "react-i18next";
import type { TypeDistributionItem } from "../../../../types/dashboard";
import styles from "./TypeDistributionList.module.scss";

const { Text } = Typography;

// Definit les proprietes attendues par le composant TypeDistributionList.
interface TypeDistributionListProps {
  distribution: TypeDistributionItem[];
  isLoading: boolean;
  isError: boolean;
}

// Expose la constante SKELETON_COUNT utilisee par type distribution liste.
const SKELETON_COUNT = 4;

// Rend le composant SkeletonList pour l'interface type distribution liste.
const SkeletonList = memo(() => (
  <div className={styles.skeletonList} aria-busy="true">
    {Array.from({ length: SKELETON_COUNT }).map((_, index) => (
      <div key={index} className={styles.skeletonItem}>
        <Skeleton active paragraph={{ rows: 1 }} title={{ width: "50%" }} />
      </div>
    ))}
  </div>
));

SkeletonList.displayName = "SkeletonList";

// Rend le composant TypeDistributionList pour l'interface type distribution liste.
const TypeDistributionList = memo(
  ({ distribution, isLoading, isError }: TypeDistributionListProps) => {
    const { t } = useTranslation();

    // Trie les types par nombre d'incidents decroissant.
    const sorted = useMemo(
      () => [...distribution].sort((a, b) => b.count - a.count),
      [distribution],
    );

    // Total des incidents, sert au calcul des pourcentages.
    const totalCount = useMemo(
      () => sorted.reduce((sum, item) => sum + item.count, 0),
      [sorted],
    );

    if (isLoading) {
      return <SkeletonList />;
    }

    if (isError || sorted.length === 0) {
      return (
        <Empty
          description={t("dashboard.typeDistribution.empty")}
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          className={styles.empty}
        />
      );
    }

    return (
      <div className={styles.list} role="list">
        {sorted.map((item) => {
          const percent =
            totalCount > 0 ? Math.round((item.count / totalCount) * 100) : 0;

          return (
            <div key={item.typeId} className={styles.listItem} role="listitem">
              <div className={styles.itemContent}>
                {}
                <div className={styles.itemHeader}>
                  <Text
                    className={styles.typeName}
                    ellipsis={{ tooltip: item.typeName }}
                  >
                    {item.typeName}
                  </Text>
                  <Text className={styles.count}>{item.count}</Text>
                </div>

                {}
                <Progress
                  className={styles.progressBar}
                  percent={percent}
                  showInfo={false}
                  size="small"
                />
              </div>
            </div>
          );
        })}
      </div>
    );
  },
);

TypeDistributionList.displayName = "TypeDistributionList";

export default TypeDistributionList;
