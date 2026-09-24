// Documente les roles utilisateur connus du frontend.
export const UserRole = {
  AGENT: "AGENT",
  CHEF_AGENCE: "CHEF_AGENCE",
  CHEF_SERVICE: "CHEF_SERVICE",
  ADMIN: "ADMIN",
  SUPER_ADMIN: "SUPER_ADMIN",
} as const;

// Documente les roles utilisateur connus du frontend.
export type UserRole = (typeof UserRole)[keyof typeof UserRole];

// Documente les permissions utilisateur connues du frontend.
export const UserPermission = {
  USER_CREATE_ALL_AGENT: "USER_CREATE_ALL_AGENT",
  USER_CREATE_AGENT_AGENCY: "USER_CREATE_AGENT_AGENCY",
  USER_CREATE_AGENT_SERVICE: "USER_CREATE_AGENT_SERVICE",
  USER_CREATE_CHEF_AGENCE: "USER_CREATE_CHEF_AGENCE",
  USER_CREATE_CHEF_SERVICE: "USER_CREATE_CHEF_SERVICE",
  USER_CREATE_ADMIN: "USER_CREATE_ADMIN",
  USER_UPDATE: "USER_UPDATE",
  USER_DELETE: "USER_DELETE",
  USER_VIEW_AGENCY: "USER_VIEW_AGENCY",
  USER_VIEW_ALL: "USER_VIEW_ALL",
} as const;

// Documente les permissions utilisateur connues du frontend.
export type UserPermission =
  (typeof UserPermission)[keyof typeof UserPermission];
