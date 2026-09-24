// Section Guide des champs : explique chaque champ du formulaire d'incident
// pour faciliter la saisie.
import { memo } from "react";
import { Typography, Tag, List, Space } from "antd";
import { useTranslation } from "react-i18next";
import { INCIDENT_FIELDS } from "../../../../utils/help/helpContent";
import styles from "../../Help.module.scss";

const { Paragraph, Text } = Typography;

// Rend le composant FieldGuideSection pour l'interface field guide section.
export const FieldGuideSection = memo(() => {
  const { t } = useTranslation();

  return (
    <div>
      <Paragraph type="secondary" className={styles.sectionIntro}>
        {t("help.sections.fields.intro")}
      </Paragraph>

      <List
        size="small"
        dataSource={INCIDENT_FIELDS}
        renderItem={(field) => (
          <List.Item>
            <Space direction="vertical" size={2}>
              <Space size={8}>
                <Text strong>{t(`help.fields.${field.key}.label`)}</Text>
                <Tag
                  color={field.required ? "red" : "default"}
                  className={styles.fieldTag}
                >
                  {field.required
                    ? t("help.sections.fields.required")
                    : t("help.sections.fields.optional")}
                </Tag>
              </Space>
              <Text>{t(`help.fields.${field.key}.help`)}</Text>
            </Space>
          </List.Item>
        )}
      />
    </div>
  );
});

FieldGuideSection.displayName = "FieldGuideSection";
