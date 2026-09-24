import { designTokens } from "./theme/tokens";

/**
 * Thème Ant Design — aligné sur les design tokens FinTrack.
 * Garder ces valeurs synchronisées avec src/styles/tokens.scss.
 */
export const themeConfig = {
  token: designTokens,
  components: {
    Button: {
      borderRadius: 8,
      controlHeight: 42,
      fontWeight: 600,
    },
    Input: {
      borderRadius: 8,
      controlHeight: 42,
    },
    Select: {
      borderRadius: 8,
      controlHeight: 42,
    },
    Modal: {
      borderRadiusLG: 8,
      titleFontSize: 18,
      titleLineHeight: 1.6,
    },
    Tabs: {
      borderRadius: 8,
    },
    Tag: {
      borderRadius: 6,
      fontSizeSM: 12,
      fontWeight: 500,
    },
    Table: {
      headerBorderRadius: 8,
    },
    Card: {
      borderRadiusLG: 8,
    },
    Form: {
      labelFontSize: 14,
      itemMarginBottom: 24,
      labelRequiredMarkColor: "#E53935",
    },
    Notification: {
      paddingContentHorizontal: 24,
      paddingMD: 24,
      width: 420,
    },
    Popconfirm: {
      borderRadius: 8,
    },
    Switch: {
      colorPrimary: "#10B981",
      colorPrimaryHover: "#059669",
    },
  },
};
