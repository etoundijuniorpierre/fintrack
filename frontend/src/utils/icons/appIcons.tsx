// Centralisation des icones SVG/React utilisees dans l'application.
import {
  AppstoreOutlined,
  ArrowLeftOutlined,
  AuditOutlined,
  BankOutlined,
  BarChartOutlined,
  BellOutlined,
  BulbOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CommentOutlined,
  CloseOutlined,
  DeleteOutlined,
  DeploymentUnitOutlined,
  DownloadOutlined,
  EditOutlined,
  EyeInvisibleOutlined,
  EyeTwoTone,
  EyeOutlined,
  DashboardOutlined,
  EnvironmentOutlined,
  FileTextOutlined,
  FilterFilled,
  GlobalOutlined,
  HistoryOutlined,
  IeOutlined,
  InfoCircleOutlined,
  KeyOutlined,
  LockOutlined,
  LoginOutlined,
  LogoutOutlined,
  MailOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MoonOutlined,
  PaperClipOutlined,
  PlusOutlined,
  QuestionCircleOutlined,
  ReloadOutlined,
  RollbackOutlined,
  SaveOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  SendOutlined,
  SettingOutlined,
  ScheduleOutlined,
  SolutionOutlined,
  SortAscendingOutlined,
  SunOutlined,
  SwapOutlined,
  TableOutlined,
  TeamOutlined,
  ThunderboltOutlined,
  PhoneOutlined,
  UserOutlined,
  UserSwitchOutlined,
  TagsOutlined,
  UploadOutlined,
  WarningOutlined,
} from "@ant-design/icons";

export const displayModeIcons = {
  stats: <AppstoreOutlined aria-hidden="true" />,
  graphs: <BarChartOutlined aria-hidden="true" />,
  table: <TableOutlined aria-hidden="true" />,
  cards: <AppstoreOutlined aria-hidden="true" />,
} as const;

export const dashboardKpiIcons = {
  activeIncidents: <ThunderboltOutlined aria-hidden="true" />,
  closedIncidents: <SafetyCertificateOutlined aria-hidden="true" />,
  rejectedIncidents: <CloseOutlined aria-hidden="true" />,
  blockedIncidents: <LockOutlined aria-hidden="true" />,
  totalIncidents: <FileTextOutlined aria-hidden="true" />,
  durationHours: <ClockCircleOutlined aria-hidden="true" />,
  assignedToMe: <UserOutlined aria-hidden="true" />,
  transferredByMe: <SwapOutlined aria-hidden="true" />,
  closedByMe: <SafetyCertificateOutlined aria-hidden="true" />,
  activeUsers: <UserOutlined aria-hidden="true" />,
  inactiveUsers: <UserOutlined aria-hidden="true" />,
  topServices: <TeamOutlined aria-hidden="true" />,
} as const;

export const actionIcons = {
  add: <PlusOutlined aria-hidden="true" />,
  configure: <SettingOutlined aria-hidden="true" />,
  edit: <EditOutlined aria-hidden="true" />,
  close: <CloseOutlined aria-hidden="true" />,
  view: <EyeOutlined aria-hidden="true" />,
  download: <DownloadOutlined aria-hidden="true" />,
  save: <SaveOutlined aria-hidden="true" />,
  search: <SearchOutlined aria-hidden="true" />,
  sort: <SortAscendingOutlined aria-hidden="true" />,
  sendEmail: <SendOutlined aria-hidden="true" />,
  toggleUser: <UserSwitchOutlined aria-hidden="true" />,
  upload: <UploadOutlined aria-hidden="true" />,
  delete: <DeleteOutlined aria-hidden="true" />,
  report: <FileTextOutlined aria-hidden="true" />,
  reply: <RollbackOutlined aria-hidden="true" />,
  transfer: <SwapOutlined aria-hidden="true" />,
  validate: <CheckCircleOutlined aria-hidden="true" />,
  reopen: <ReloadOutlined aria-hidden="true" />,
} as const;

// Rend l'icone des filtres de tableau en distinguant clairement un filtre actif.
export const tableFilterIcon = (active: boolean) => (
  <FilterFilled
    aria-hidden="true"
    data-filter-active={active}
    style={{ color: active ? "var(--info)" : "currentColor" }}
  />
);

export const incidentIcons = {
  attachment: <PaperClipOutlined aria-hidden="true" />,
  cause: <BulbOutlined aria-hidden="true" />,
  classification: <TagsOutlined aria-hidden="true" />,
  comment: <CommentOutlined aria-hidden="true" />,
  description: <FileTextOutlined aria-hidden="true" />,
  dueDate: <CalendarOutlined aria-hidden="true" />,
  resolution: <CheckCircleOutlined aria-hidden="true" />,
  user: <UserOutlined aria-hidden="true" />,
  createdAt: <ClockCircleOutlined aria-hidden="true" />,
  criticality: <WarningOutlined aria-hidden="true" />,
} as const;

export const formIcons = {
  bank: <BankOutlined aria-hidden="true" />,
  deployment: <DeploymentUnitOutlined aria-hidden="true" />,
  info: <InfoCircleOutlined aria-hidden="true" />,
  lock: <LockOutlined aria-hidden="true" />,
  phone: <PhoneOutlined aria-hidden="true" />,
  roles: <IeOutlined aria-hidden="true" />,
  user: <UserOutlined aria-hidden="true" />,
  warning: <WarningOutlined aria-hidden="true" />,
} as const;

export const authIcons = {
  certificate: <SafetyCertificateOutlined aria-hidden="true" />,
  eyeHidden: <EyeInvisibleOutlined aria-hidden="true" />,
  eyeVisible: <EyeTwoTone aria-hidden="true" />,
} as const;

export const detailIcons = {
  app: <AppstoreOutlined aria-hidden="true" />,
  calendar: <CalendarOutlined aria-hidden="true" />,
  clock: <ClockCircleOutlined aria-hidden="true" />,
  environment: <EnvironmentOutlined aria-hidden="true" />,
  history: <HistoryOutlined aria-hidden="true" />,
  key: <KeyOutlined aria-hidden="true" />,
  login: <LoginOutlined aria-hidden="true" />,
  mail: <MailOutlined aria-hidden="true" />,
  reload: <ReloadOutlined aria-hidden="true" />,
  schedule: <ScheduleOutlined aria-hidden="true" />,
  solution: <SolutionOutlined aria-hidden="true" />,
  team: <TeamOutlined aria-hidden="true" />,
  thunderbolt: <ThunderboltOutlined aria-hidden="true" />,
  user: <UserOutlined aria-hidden="true" />,
} as const;

export const navigationIcons = {
  audit: <AuditOutlined aria-hidden="true" />,
  back: <ArrowLeftOutlined aria-hidden="true" />,
  dashboard: <DashboardOutlined aria-hidden="true" />,
  incidents: <WarningOutlined aria-hidden="true" />,
  menuFold: <MenuFoldOutlined aria-hidden="true" />,
  menuUnfold: <MenuUnfoldOutlined aria-hidden="true" />,
  reports: <FileTextOutlined aria-hidden="true" />,
  settings: <SettingOutlined aria-hidden="true" />,
  superAdmin: <SafetyCertificateOutlined aria-hidden="true" />,
  users: <UserOutlined aria-hidden="true" />,
} as const;

export const headerIcons = {
  bell: <BellOutlined aria-hidden="true" />,
  global: <GlobalOutlined aria-hidden="true" />,
  logout: <LogoutOutlined aria-hidden="true" />,
  user: <UserOutlined aria-hidden="true" />,
  help: <QuestionCircleOutlined aria-hidden="true" />,
  moon: <MoonOutlined aria-hidden="true" />,
  sun: <SunOutlined aria-hidden="true" />,
} as const;
