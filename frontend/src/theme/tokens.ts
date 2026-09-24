/**
 * Thème Ant Design — aligné sur les design tokens FinTrack.
 *   PRIMARY = Navy/Ardoise (#1E293B) → colorPrimary, actions, sélection, focus
 *   DANGER  = Rouge (#E53935) → destructif ET accent incident/signature « Fin »
 *   ACCENT  = Indigo (#6366F1) → info
 *   En dark mode, colorPrimary est éclairci (slate) côté App.tsx pour rester visible.
 */
export const designTokens = {
  colorPrimary: "#1E293B", // Navy
  colorPrimaryHover: "#334155",
  colorPrimaryActive: "#0F172A",

  colorError: "#E53935", // Rouge danger / accent marque
  colorErrorHover: "#EF4444",
  colorErrorActive: "#B91C1C",

  colorSuccess: "#10B981", // Vert
  colorWarning: "#F59E0B", // Ambre (Warning souvent = Primary maintenant)
  colorInfo: "#6366F1", // Indigo

  // NB : ne pas figer colorBgContainer / colorBorder ici — ils sont fournis par
  // l'algorithme AntD (default/dark) et les figer casserait le mode sombre.

  borderRadius: 8,
  borderRadiusLG: 8,
  borderRadiusSM: 8,
  fontFamily:
    "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif",
  fontSize: 14,
  controlHeight: 40,
  controlHeightSM: 32,
  controlHeightLG: 48,
};
