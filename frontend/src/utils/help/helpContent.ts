// Structure du centre d'aide : quelles sections, quels champs, dans quel ordre.
// Les textes correspondants vivent dans les fichiers i18n (locales/*/help.json).

/** Champ de formulaire documenté dans le guide des champs. */
// Centralise la logique d'interface liee a help field.
export interface HelpField {
  key: string;
  required: boolean;
}

export interface HelpTopic {
  key: string;
  requiredPermissions?: string[];
}

/** Champs du formulaire de création/édition d'incident. */
export const INCIDENT_FIELDS: HelpField[] = [
  { key: "title", required: true },
  { key: "description", required: true },
  { key: "type", required: true },
  { key: "criticality", required: true },
  { key: "dueDate", required: false },
  { key: "incidentDate", required: false },
  { key: "observationDate", required: false },
  { key: "targetService", required: false },
  { key: "assignToSelf", required: false },
  { key: "cause", required: false },
  { key: "causeDetail", required: false },
  { key: "attachments", required: false },
];

import { PERMISSIONS } from "../permissions/permissions";

/**
 * Sections purement textuelles : chaque entrée liste les clés de paragraphes
 * et potentiellement les permissions requises pour les voir.
 * i18n : help.sections.<sectionKey>.items.<paragraphKey>.{title,body}
 */
export const TEXT_SECTIONS: Record<string, HelpTopic[]> = {
  workflow: [
    { key: "creation" },
    { key: "validation" },
    { key: "transfer" },
    { key: "treatment" },
    { key: "resolution" },
    { key: "closure" },
    { key: "reopen" },
  ],
  managing: [
    { key: "viewAndFilter" },
    { key: "create", requiredPermissions: [PERMISSIONS.INCIDENT.CREATE] },
    { key: "edit", requiredPermissions: [PERMISSIONS.INCIDENT.UPDATE] },
    { key: "validate", requiredPermissions: [PERMISSIONS.INCIDENT.VALIDATE] },
    { key: "reject", requiredPermissions: [PERMISSIONS.INCIDENT.REJECT] },
    {
      key: "transfer",
      requiredPermissions: [
        PERMISSIONS.INCIDENT.TRANSFER,
        PERMISSIONS.INCIDENT.AUTO_TRANSFER,
      ],
    },
    { key: "assign", requiredPermissions: [PERMISSIONS.INCIDENT.ASSIGN] },
    { key: "resolve", requiredPermissions: [PERMISSIONS.INCIDENT.RESOLVE] },
    { key: "close", requiredPermissions: [PERMISSIONS.INCIDENT.CLOSE] },
    { key: "reopen", requiredPermissions: [PERMISSIONS.INCIDENT.REOPEN] },
  ],
  incidentDetails: [
    { key: "overview" },
    { key: "attachments" },
    {
      key: "comments",
      requiredPermissions: [
        PERMISSIONS.INCIDENT.VIEW_OWN,
        PERMISSIONS.INCIDENT.VIEW_SERVICE,
        PERMISSIONS.INCIDENT.VIEW_AGENCY,
        PERMISSIONS.INCIDENT.VIEW_ALL,
        PERMISSIONS.INCIDENT.TREAT,
      ],
    },
    {
      key: "history",
      requiredPermissions: [
        PERMISSIONS.INCIDENT.VIEW_OWN,
        PERMISSIONS.INCIDENT.VIEW_SERVICE,
        PERMISSIONS.INCIDENT.VIEW_AGENCY,
        PERMISSIONS.INCIDENT.VIEW_ALL,
        PERMISSIONS.INCIDENT.TREAT,
      ],
    },
    { key: "clone", requiredPermissions: [PERMISSIONS.INCIDENT.CREATE] },
  ],
  dashboard: [
    { key: "overview" },
    { key: "kpis" },
    { key: "period" },
    { key: "scope" },
    { key: "stockFlow" },
    {
      key: "configPanel",
      requiredPermissions: [PERMISSIONS.DASHBOARD.CONFIGURE],
    },
  ],
  notifications: [
    { key: "what" },
    { key: "channels" },
    { key: "browser" },
    { key: "when" },
  ],
  reports: [
    { key: "generate", requiredPermissions: [PERMISSIONS.REPORT.GENERATE] },
    { key: "schedule", requiredPermissions: [PERMISSIONS.SETTINGS.SYSTEM] },
    { key: "formats", requiredPermissions: [PERMISSIONS.REPORT.EXPORT] },
    { key: "email", requiredPermissions: [PERMISSIONS.REPORT.SEND_EMAIL] },
    { key: "delete", requiredPermissions: [PERMISSIONS.REPORT.DELETE] },
    {
      key: "scope",
      requiredPermissions: [
        PERMISSIONS.REPORT.VIEW_OWN,
        PERMISSIONS.REPORT.VIEW_SERVICE,
        PERMISSIONS.REPORT.VIEW_AGENCY,
        PERMISSIONS.REPORT.VIEW_ALL,
      ],
    },
  ],
  profile: [{ key: "info" }, { key: "password" }, { key: "rolesPermissions" }],
  users: [
    {
      key: "view",
      requiredPermissions: [
        PERMISSIONS.USER.VIEW_SERVICE,
        PERMISSIONS.USER.VIEW_AGENCY,
        PERMISSIONS.USER.VIEW_ALL,
      ],
    },
    {
      key: "create",
      requiredPermissions: [
        PERMISSIONS.USER.CREATE_ALL_AGENT,
        PERMISSIONS.USER.CREATE_AGENT_AGENCY,
        PERMISSIONS.USER.CREATE_AGENT_SERVICE,
        PERMISSIONS.USER.CREATE_AGENCY_MANAGER,
        PERMISSIONS.USER.CREATE_SERVICE_MANAGER,
        PERMISSIONS.USER.CREATE_ADMIN,
      ],
    },
    {
      key: "permissions",
      requiredPermissions: [PERMISSIONS.ROLE.ASSIGN],
    },
    { key: "status", requiredPermissions: [PERMISSIONS.USER.UPDATE] },
  ],
  audit: [{ key: "view", requiredPermissions: [PERMISSIONS.AUDIT.VIEW] }],
  settings: [
    {
      key: "incidentTypes",
      requiredPermissions: [PERMISSIONS.SETTINGS.INCIDENT_TYPES],
    },
    { key: "agencies", requiredPermissions: [PERMISSIONS.SETTINGS.SYSTEM] },
    { key: "services", requiredPermissions: [PERMISSIONS.SETTINGS.SYSTEM] },
    {
      key: "rolesPerm",
      requiredPermissions: [
        PERMISSIONS.ROLE.CREATE,
        PERMISSIONS.ROLE.UPDATE,
        PERMISSIONS.ROLE.DELETE,
        PERMISSIONS.ROLE.ASSIGN,
      ],
    },
    { key: "reportSchedules", requiredPermissions: [PERMISSIONS.SETTINGS.SYSTEM] },
    { key: "system", requiredPermissions: [PERMISSIONS.SETTINGS.SYSTEM] },
  ],
};

/** Questions de la FAQ. i18n : help.sections.faq.items.<key>.{q,a} */
export const FAQ_KEYS: HelpTopic[] = [
  { key: "selfAssign", requiredPermissions: [PERMISSIONS.INCIDENT.CREATE] },
  { key: "targetService", requiredPermissions: [PERMISSIONS.INCIDENT.CREATE] },
  {
    key: "autoFill",
    requiredPermissions: [
      PERMISSIONS.USER.CREATE_ALL_AGENT,
      PERMISSIONS.USER.CREATE_AGENT_AGENCY,
      PERMISSIONS.USER.CREATE_AGENT_SERVICE,
      PERMISSIONS.USER.CREATE_AGENCY_MANAGER,
      PERMISSIONS.USER.CREATE_SERVICE_MANAGER,
    ],
  },
  { key: "notifications" },
  { key: "editIncident", requiredPermissions: [PERMISSIONS.INCIDENT.UPDATE] },
  {
    key: "deletedAudit",
    requiredPermissions: [
      PERMISSIONS.REPORT.DELETE,
      PERMISSIONS.USER.DELETE,
    ],
  },
  { key: "firstLogin" },
];
