// Formulaire d'edition et d'ajout de compte utilisateur.

import {
  Form,
  Input,
  Select,
  Divider,
  Row,
  Col,
  Space,
  InputNumber,
  Tooltip,
  Typography,
  type FormInstance,
} from "antd";
import { FormActions } from "../../../../components/ui";
import { useTranslation } from "react-i18next";
import {
  useMemo,
  useEffect,
  memo,
  useCallback,
  useRef,
  useState,
} from "react";
import { useCreateUser, useUpdateUser } from "../../../../hooks/user/useUsers";
import { useAuth } from "../../../../hooks/auth/useAuth";
import { useRoles } from "../../../../hooks/role/useRoles";
import { useAgencies } from "../../../../hooks/agency/useAgencies";
import { useServices } from "../../../../hooks/service/useServices";
import { usePermissions } from "../../../../hooks/permission/usePermissions";
import { PermissionChecklist } from "./PermissionChecklist";
import type { CreateUserRequest, User } from "../../../../api/user/types";
import { isValidPhone, isValidEmailWithSubdomain } from "../../../../utils/validators/validators";
import {
  formatPhoneNumber,
  parseNumber,
  hyperCaseAllWordsFormItemProps,
} from "../../../../utils/formatters/formatters";
import { useResetTouchedOnNextTick } from "../../../../hooks/ui/useResetTouchedOnNextTick/useResetTouchedOnNextTick";
import {
  PERMISSIONS,
  USER_CREATE_PERMISSIONS,
  hasAnyPermission,
} from "../../../../utils/permissions/permissions";
import {
  ROLE_NAMES,
  normalizeRoleName,
  formatRoleName,
} from "../../../../utils/roles/roles";
import { detailIcons, formIcons } from "../../../../utils/icons/appIcons";
import { resetFormState } from "../../../../utils/form/formReset/formReset";
import styles from "./UserForm.module.scss";

const { Text } = Typography;

const ELEVATED_ROLE_PERMISSIONS = new Set<string>([
  PERMISSIONS.USER.CREATE_ALL_AGENT,
  PERMISSIONS.USER.CREATE_AGENT_AGENCY,
  PERMISSIONS.USER.CREATE_AGENT_SERVICE,
  PERMISSIONS.USER.CREATE_AGENCY_MANAGER,
  PERMISSIONS.USER.CREATE_SERVICE_MANAGER,
  PERMISSIONS.USER.CREATE_ADMIN,
  PERMISSIONS.USER.UPDATE,
  PERMISSIONS.USER.DELETE,
  PERMISSIONS.USER.VIEW_ALL,
  PERMISSIONS.ROLE.CREATE,
  PERMISSIONS.ROLE.UPDATE,
  PERMISSIONS.ROLE.DELETE,
  PERMISSIONS.ROLE.ASSIGN,
]);

// Definit les proprietes attendues par le composant UserForm.
interface UserFormProps {
  form?: FormInstance<CreateUserRequest>;
  onSuccess?: (user: User) => void;
  onCancel?: () => void;
  initialValues?: Partial<User>;
  submitText?: string;
  showButtons?: boolean;
  disabled?: boolean;
}

// Rend le composant SectionDivider pour l'interface utilisateur formulaire.
const SectionDivider = memo(
  ({ icon, title }: { icon: React.ReactNode; title: string }) => (
    <Divider titlePlacement="left" className={styles.sectionDivider}>
      <Space size={8}>
        {icon}
        {title}
      </Space>
    </Divider>
  ),
);

// Initialise le formulaire de creation ou edition utilisateur.
const UserForm = memo(
  ({
    form: propForm,
    onSuccess,
    onCancel,
    initialValues,
    submitText,
    showButtons = true,
    disabled = false,
  }: UserFormProps) => {
    const [internalForm] = Form.useForm<CreateUserRequest>();
    const form = propForm || internalForm;
    const { t } = useTranslation();

    const { mutate: createUser, isPending: isCreating } = useCreateUser();
    const { mutate: updateUser, isPending: isUpdating } = useUpdateUser();
    const { user, hasPermission } = useAuth();
    const canAssignRoles = useMemo(
      () => hasPermission(PERMISSIONS.ROLE.ASSIGN),
      [hasPermission],
    );
    const canCreateUsers = useMemo(
      () => hasAnyPermission(hasPermission, USER_CREATE_PERMISSIONS),
      [hasPermission],
    );
    const canCreateAdmin = hasPermission(PERMISSIONS.USER.CREATE_ADMIN);
    const canCreateAllAgents = hasPermission(PERMISSIONS.USER.CREATE_ALL_AGENT);
    const canCreateAgencyAgents = hasPermission(PERMISSIONS.USER.CREATE_AGENT_AGENCY);
    const canCreateServiceAgents = hasPermission(PERMISSIONS.USER.CREATE_AGENT_SERVICE);
    const hasInitialRoles = Boolean(initialValues?.roles?.length);
    const hasInitialPermissions = Boolean(initialValues?.permissions?.length);
    const hasInitialAgency = Boolean(initialValues?.agency);
    const hasInitialService = Boolean(
      initialValues?.service ||
      initialValues?.managedServiceIds?.length ||
      initialValues?.managedAgencyId,
    );
    const shouldLoadRoleCatalog =
      !disabled || canAssignRoles || hasInitialRoles;
    const shouldLoadPermissionCatalog =
      canAssignRoles || hasInitialPermissions || hasInitialRoles;
    const shouldLoadAgencyCatalog = !disabled || hasInitialAgency;
    const shouldLoadServiceCatalog = !disabled || hasInitialService;

    const { data: rolesData, isLoading: rolesLoading } = useRoles(
      shouldLoadRoleCatalog,
    );
    const { data: agenciesData, isLoading: agenciesLoading } = useAgencies({
      enabled: shouldLoadAgencyCatalog,
    });
    const { data: servicesData, isLoading: servicesLoading } = useServices({
      enabled: shouldLoadServiceCatalog,
    });
    const { data: permissionsData, isLoading: permissionsLoading } =
      usePermissions(shouldLoadPermissionCatalog);

    const isPending = isCreating || isUpdating;
    const [isTouched, setIsTouched] = useState(false);
    const isEditMode = Boolean(initialValues?.id);
    const isSubmitDisabled = (isEditMode && !isTouched) || isPending;

    // Applique le perimetre organisationnel du createur uniquement aux comptes restreints.
    const isScopedCreate =
      !isEditMode && !hasPermission(PERMISSIONS.USER.VIEW_ALL);
    const creatorIsChefService = isScopedCreate && canCreateServiceAgents;
    const creatorIsChefAgence =
      isScopedCreate &&
      !creatorIsChefService &&
      canCreateAgencyAgents;
    const canSelectRoles = canAssignRoles || (!isEditMode && canCreateUsers);

    const watchedRoleIds: string[] | undefined = Form.useWatch("roleIds", form);
    const revokedPermissionIds: string[] =
      Form.useWatch("revokedPermissionIds", form) ?? [];
    const liveRoleIds = useMemo(
      () => watchedRoleIds ?? initialValues?.roles?.map((r) => r.id) ?? [],
      [watchedRoleIds, initialValues?.roles],
    );
    const previousRolePermissionIdsRef = useRef<Set<string>>(new Set());

    const transformedInitialValues = useMemo(() => {
      if (!initialValues) return undefined;
      return {
        ...initialValues,
        agencyId: initialValues.agency?.id,
        serviceId: initialValues.service?.id,
        roleIds: initialValues.roles?.map((r) => r.id),
        permissionIds: initialValues.permissions?.map((p) => p.id),
        revokedPermissionIds: initialValues.revokedPermissions?.map(
          (p) => p.id,
        ),
      };
    }, [initialValues]);

    // Effet secondaire pour charger ou synchroniser les donnees.
    useEffect(() => {
      if (transformedInitialValues) {
        previousRolePermissionIdsRef.current = new Set();
        form.setFieldsValue(transformedInitialValues);
      } else {
        previousRolePermissionIdsRef.current = new Set();
        form.resetFields();
      }
    }, [initialValues, form, transformedInitialValues]);

    // En mode creation, pre-remplir agence/service du createur une fois les donnees chargees.
    useEffect(() => {
      if (initialValues || (!creatorIsChefService && !creatorIsChefAgence))
        return;
      if (!agenciesData && !servicesData) return;
      const preset: Partial<CreateUserRequest> = {};
      if (creatorIsChefService) {
        if (user?.agencyId) preset.agencyId = user.agencyId;
        if (user?.serviceId) preset.serviceId = user.serviceId;
      } else if (creatorIsChefAgence) {
        if (user?.agencyId) preset.agencyId = user.agencyId;
      }
      if (Object.keys(preset).length > 0) form.setFieldsValue(preset);
    }, [
      initialValues,
      agenciesData,
      servicesData,
      creatorIsChefService,
      creatorIsChefAgence,
      user?.agencyId,
      user?.serviceId,
      form,
    ]);

    useResetTouchedOnNextTick(setIsTouched, [initialValues, form]);

    // Effet secondaire pour charger ou synchroniser les donnees.
    useEffect(() => {
      if (!canAssignRoles || !rolesData || !permissionsData) return;

      const selectedRoleIds = new Set(liveRoleIds);
      const rolePermissionIds = new Set<string>();
      rolesData
        .filter((r) => selectedRoleIds.has(r.id))
        .forEach((r) =>
          r.permissions?.forEach((p) => rolePermissionIds.add(p.id)),
        );

      const currentPermIds: string[] =
        form.getFieldValue("permissionIds") ?? [];

      const updated = [
        ...currentPermIds.filter(
          (id) => !previousRolePermissionIdsRef.current.has(id),
        ),
        ...Array.from(rolePermissionIds),
      ];
      form.setFieldValue("permissionIds", [...new Set(updated)]);

      // Cohérence des révoquées : ne conserver que celles encore héritées d'un role
      // (une permission qui n'est plus accordée par un role ne peut plus etre revoquee).
      const currentRevoked: string[] =
        form.getFieldValue("revokedPermissionIds") ?? [];
      const prunedRevoked = currentRevoked.filter((id) =>
        rolePermissionIds.has(id),
      );
      if (prunedRevoked.length !== currentRevoked.length) {
        form.setFieldValue("revokedPermissionIds", prunedRevoked);
      }

      previousRolePermissionIdsRef.current = rolePermissionIds;
    }, [canAssignRoles, liveRoleIds, rolesData, permissionsData, form]);

    const roleOptions = useMemo(() => {
      const roleSource = rolesData ?? initialValues?.roles ?? [];

      return roleSource
        .filter((role) => {
          const roleName = normalizeRoleName(role.name);

          if (
            roleName === ROLE_NAMES.SUPER_ADMIN &&
            !disabled &&
            !initialValues?.roles?.some((r) => r.id === role.id)
          )
            return false;

          if (!initialValues?.id) {
            if (
              roleName === ROLE_NAMES.AGENT &&
              !canCreateAllAgents &&
              !canCreateAgencyAgents &&
              !canCreateServiceAgents
            )
              return false;
            if (
              roleName === ROLE_NAMES.AGENCY_MANAGER &&
              !hasPermission(PERMISSIONS.USER.CREATE_AGENCY_MANAGER)
            )
              return false;
            if (
              roleName === ROLE_NAMES.SERVICE_MANAGER &&
              !hasPermission(PERMISSIONS.USER.CREATE_SERVICE_MANAGER)
            )
              return false;
            if (
              roleName === ROLE_NAMES.ADMIN &&
              !hasPermission(PERMISSIONS.USER.CREATE_ADMIN)
            )
              return false;

            const isCustomRole = !Object.values(ROLE_NAMES).some(
              (knownRole) => normalizeRoleName(knownRole) === roleName,
            );
            if (
              isCustomRole &&
              !canCreateAllAgents &&
              !canCreateAgencyAgents &&
              !canCreateServiceAgents
            )
              return false;

            const grantsElevatedAuthority =
              role.permissions?.some((permission) =>
                ELEVATED_ROLE_PERMISSIONS.has(permission.name),
              ) ?? false;
            if (isCustomRole && grantsElevatedAuthority && !canCreateAdmin)
              return false;
          }
          return true;
        })
        .map((role) => ({
          label: formatRoleName(role, t),
          value: role.id,
        }));
    }, [
      rolesData,
      initialValues?.roles,
      initialValues?.id,
      disabled,
      hasPermission,
      canCreateAdmin,
      canCreateAllAgents,
      canCreateAgencyAgents,
      canCreateServiceAgents,
      t,
    ]);

    const agencyOptions = useMemo(
      () => agenciesData?.map((a) => ({ label: a.name, value: a.id })) || [],
      [agenciesData],
    );

    const serviceOptions = useMemo(
      () => servicesData?.map((s) => ({ label: s.name, value: s.id })) || [],
      [servicesData],
    );

    const isAgencyRequired = useMemo(() => {
      if (!rolesData && !initialValues?.roles) return false;
      const allRoles = rolesData || initialValues?.roles || [];
      const selectedRoles = allRoles.filter((r) => liveRoleIds.includes(r.id));
      return selectedRoles.some((r) =>
        [
          normalizeRoleName(ROLE_NAMES.AGENT),
          normalizeRoleName(ROLE_NAMES.AGENCY_MANAGER),
          normalizeRoleName(ROLE_NAMES.SERVICE_MANAGER),
        ].includes(normalizeRoleName(r.name)),
      );
    }, [rolesData, initialValues?.roles, liveRoleIds]);

    const isChefService = useMemo(() => {
      if (!rolesData && !initialValues?.roles) return false;
      const allRoles = rolesData || initialValues?.roles || [];
      const selectedRoles = allRoles.filter((r) => liveRoleIds.includes(r.id));
      return selectedRoles.some(
        (r) =>
          normalizeRoleName(r.name) ===
          normalizeRoleName(ROLE_NAMES.SERVICE_MANAGER),
      );
    }, [rolesData, initialValues?.roles, liveRoleIds]);

    const isChefAgence = useMemo(() => {
      if (!rolesData && !initialValues?.roles) return false;
      const allRoles = rolesData || initialValues?.roles || [];
      const selectedRoles = allRoles.filter((r) => liveRoleIds.includes(r.id));
      return selectedRoles.some(
        (r) =>
          normalizeRoleName(r.name) ===
          normalizeRoleName(ROLE_NAMES.AGENCY_MANAGER),
      );
    }, [rolesData, initialValues?.roles, liveRoleIds]);

    const permissionOptions = useMemo(() => {
      const selectedRoleIds = new Set(liveRoleIds);
      const rolePermissionIds = new Set<string>();
      const roleSource = rolesData ?? initialValues?.roles ?? [];
      const permissionSource =
        permissionsData ?? initialValues?.permissions ?? [];

      roleSource
        .filter((r) => selectedRoleIds.has(r.id))
        .forEach((r) =>
          r.permissions?.forEach((p) => rolePermissionIds.add(p.id)),
        );

      return permissionSource.map((p) => ({
        label: String(t(`users.permissions.${p.name}`, p.name)),
        value: p.id,
        desc: String(
          t(`users.permission_descriptions.${p.name}`, p.description ?? ""),
        ),
        name: p.name,
        fromRole: rolePermissionIds.has(p.id),
      }));
    }, [
      permissionsData,
      rolesData,
      initialValues?.permissions,
      initialValues?.roles,
      liveRoleIds,
      t,
    ]);

    const handleCancel = useCallback(() => {
      resetFormState(
        form,
        initialValues
          ? {
              username: initialValues.username,
              email: initialValues.email,
              phoneNumber: initialValues.phoneNumber,
              firstName: initialValues.firstName,
              lastName: initialValues.lastName,
              roleIds: initialValues.roles?.map((r) => r.id),
              permissionIds: initialValues.permissions?.map((p) => p.id),
              agencyId: initialValues.agency?.id,
              serviceId: initialValues.service?.id,
              managedServiceIds: initialValues.managedServiceIds,
              managedAgencyId: initialValues.managedAgencyId,
              isActive: initialValues.isActive,
            }
          : undefined,
      );

      setIsTouched(false);
      onCancel?.();
    }, [form, initialValues, onCancel]);

    const onFinish = useCallback(
      (values: CreateUserRequest) => {
        const data: CreateUserRequest = {
          ...values,
          email: values.email?.trim() || undefined,
        };
        if (!canAssignRoles) {
          delete data.permissionIds;
          delete data.revokedPermissionIds;
        } else {
          const selectedRoleIds = new Set(data.roleIds ?? []);
          const inheritedPermissionIds = new Set<string>();
          rolesData
            ?.filter((role) => selectedRoleIds.has(role.id))
            .forEach((role) =>
              role.permissions?.forEach((permission) =>
                inheritedPermissionIds.add(permission.id),
              ),
            );
          if (data.permissionIds) {
            data.permissionIds = data.permissionIds.filter(
              (id) => !inheritedPermissionIds.has(id),
            );
          }
          data.revokedPermissionIds = (data.revokedPermissionIds ?? []).filter(
            (id) => inheritedPermissionIds.has(id),
          );
        }

        const options = {
          onSuccess: (res: User) => {
            if (!initialValues) form.resetFields();
            onSuccess?.(res);
          },
        };

        if (initialValues?.id) {
          updateUser({ id: initialValues.id, data }, options);
        } else {
          createUser(data, options);
        }
      },
      [
        initialValues,
        canAssignRoles,
        rolesData,
        updateUser,
        createUser,
        onSuccess,
        form,
      ],
    );

    const rules = useMemo(
      () => ({
        email: [
          {
            required: true,
            message: t("users.form.validation.email_required"),
          },
          {
            type: "email" as const,
            message: t("users.form.validation.email_invalid"),
          },
        ],
        phone: [
          {
            validator: (_: unknown, v: unknown) =>
              !v || isValidPhone(String(v))
                ? Promise.resolve()
                : Promise.reject(
                    new Error(t("users.form.validation.phone_invalid")),
                  ),
          },
        ],
        agency: isAgencyRequired
          ? [
              {
                required: true,
                message: t("users.form.validation.agency_required"),
              },
            ]
          : [],
        name: [
          {
            validator: (_: unknown) => {
              const first = form.getFieldValue("firstName");
              const last = form.getFieldValue("lastName");
              if (!first?.trim() && !last?.trim()) {
                return Promise.reject(
                  new Error(t("users.form.validation.name_required")),
                );
              }
              return Promise.resolve();
            },
          },
        ],
      }),
      [t, isAgencyRequired, form],
    );

    const inputIcons = useMemo(
      () => ({
        user: <span className={styles.inputPrefix}>{detailIcons.user}</span>,
        mail: <span className={styles.inputPrefix}>{detailIcons.mail}</span>,
        phone: <span className={styles.inputPrefix}>{formIcons.phone}</span>,
        solution: (
          <span className={styles.inputPrefix}>{detailIcons.solution}</span>
        ),
        bank: formIcons.bank,
        deployment: formIcons.deployment,
      }),
      [],
    );

    return (
      <>
        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          initialValues={transformedInitialValues}
          disabled={disabled}
          className={styles.userForm}
          onFieldsChange={() => setIsTouched(true)}
        >
          <SectionDivider
            icon={formIcons.user}
            title={t("users.form.sections.personal")}
          />

          <Row gutter={24}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="lastName"
                label={t("users.form.labels.lastname")}
                rules={rules.name}
                dependencies={["firstName"]}
                {...hyperCaseAllWordsFormItemProps}
              >
                <Input
                  prefix={inputIcons.user}
                  placeholder={t("users.form.placeholders.lastname")}
                  maxLength={100}
                />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item
                name="firstName"
                label={t("users.form.labels.firstname")}
                rules={rules.name}
                dependencies={["lastName"]}
                {...hyperCaseAllWordsFormItemProps}
              >
                <Input
                  prefix={inputIcons.user}
                  placeholder={t("users.form.placeholders.firstname")}
                  maxLength={100}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={24}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="email"
                label={t("users.form.labels.email")}
                rules={[
                  ...rules.email,
                  {
                    validator: async (_, value) => {
                      if (value && !isValidEmailWithSubdomain(value)) {
                        throw new Error(
                          t("users.form.validation.email_subdomain_invalid"),
                        );
                      }
                      return Promise.resolve();
                    },
                  },
                ]}
              >
                <Input
                  prefix={inputIcons.mail}
                  placeholder={t("users.form.placeholders.email")}
                  maxLength={100}
                />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item
                name="phoneNumber"
                label={t("users.form.labels.phone")}
                rules={rules.phone}
              >
                <InputNumber
                  className={styles.phoneNumberInput}
                  prefix={inputIcons.phone}
                  placeholder={t("users.form.placeholders.phone")}
                  controls={false}
                  formatter={formatPhoneNumber}
                  parser={parseNumber}
                />
              </Form.Item>
            </Col>
          </Row>

          {isEditMode && (
            <Row gutter={24}>
              <Col span={24}>
                <Form.Item
                  name="username"
                  label={t("users.form.labels.username")}
                >
                  <Input
                    prefix={inputIcons.solution}
                    disabled={true}
                    maxLength={100}
                  />
                </Form.Item>
              </Col>
            </Row>
          )}

          <SectionDivider
            icon={formIcons.roles}
            title={t("users.form.sections.roles")}
          />

          <Row gutter={24}>
            <Col span={24}>
              <Form.Item
                name="roleIds"
                label={t("users.form.labels.roles")}
                rules={[
                  {
                    required: true,
                    message: t("users.form.validation.roles_required"),
                  },
                ]}
              >
                <Select
                  mode="multiple"
                  placeholder={t("users.form.placeholders.roles")}
                  options={roleOptions}
                  loading={rolesLoading}
                  disabled={disabled || !canSelectRoles}
                  maxTagCount="responsive"
                  optionFilterProp="label"
                  allowClear
                />
              </Form.Item>
            </Col>
          </Row>

          {(canAssignRoles || disabled || hasInitialPermissions) && (
            <Row gutter={24}>
              <Col span={24}>
                <Form.Item
                  name="permissionIds"
                  label={
                    <Space>
                      {t("users.form.labels.permissions")}
                      <Tooltip
                        title={t("users.form.messages.permissions_hint")}
                      >
                        <span
                          className={styles.permissionsHintIcon}
                          aria-label={t("users.form.messages.permissions_hint")}
                          role="img"
                        >
                          {formIcons.info}
                        </span>
                      </Tooltip>
                    </Space>
                  }
                >
                  <PermissionChecklist
                    options={permissionOptions}
                    disabled={disabled || !canAssignRoles}
                    loading={permissionsLoading}
                    revokedValue={revokedPermissionIds}
                    onRevokeChange={(ids) =>
                      form.setFieldValue("revokedPermissionIds", ids)
                    }
                  />
                </Form.Item>
                <Form.Item name="revokedPermissionIds" hidden noStyle>
                  <input type="hidden" />
                </Form.Item>
              </Col>
            </Row>
          )}

          <SectionDivider
            icon={formIcons.bank}
            title={t("users.form.sections.assignment")}
          />

          <Row gutter={24}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="agencyId"
                label={t("users.form.labels.agency")}
                rules={rules.agency}
              >
                <Select
                  placeholder={t("users.form.placeholders.agency")}
                  suffixIcon={inputIcons.bank}
                  options={agencyOptions}
                  loading={agenciesLoading}
                  disabled={disabled || creatorIsChefService || creatorIsChefAgence}
                  allowClear={!disabled && !creatorIsChefService && !creatorIsChefAgence}
                />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item
                name="serviceId"
                label={t("users.form.labels.service")}
              >
                <Select
                  placeholder={t("users.form.placeholders.service")}
                  suffixIcon={inputIcons.deployment}
                  options={serviceOptions}
                  loading={servicesLoading}
                  disabled={disabled || creatorIsChefService || creatorIsChefAgence}
                  allowClear={!disabled && !creatorIsChefService && !creatorIsChefAgence}
                />
              </Form.Item>
            </Col>
          </Row>

          {isChefService && (
            <Row gutter={24}>
              <Col span={24}>
                <Form.Item
                  name="managedServiceIds"
                  label={t("users.form.labels.managed_services")}
                  preserve={false}
                >
                  <Select
                    mode="multiple"
                    placeholder={t("users.form.placeholders.managed_services")}
                    options={serviceOptions}
                    loading={servicesLoading}
                    disabled={disabled}
                    allowClear={!disabled}
                  />
                </Form.Item>
              </Col>
            </Row>
          )}

          {isChefAgence && (
            <Row gutter={24}>
              <Col span={24}>
                <Form.Item
                  name="managedAgencyId"
                  label={t("users.form.labels.managed_agency")}
                  preserve={false}
                >
                  <Select
                    placeholder={t("users.form.placeholders.managed_agency")}
                    options={agencyOptions}
                    loading={agenciesLoading}
                    disabled={disabled}
                    allowClear={!disabled}
                  />
                </Form.Item>
              </Col>
            </Row>
          )}

          {!initialValues?.id && (
            <div className={styles.passwordSection}>
              <Divider className={styles.passwordSectionDivider}>
                <Space size={8}>
                  {formIcons.lock}
                  {t("users.form.sections.password_management")}
                </Space>
              </Divider>
              <div className={styles.passwordSectionContent}>
                <div className={styles.passwordAutoGeneratedBox}>
                  <span className={styles.passwordAutoGeneratedIcon}>
                    {detailIcons.key}
                  </span>
                  <div>
                    <Text strong>
                      {t("users.form.messages.password_auto_generated")}
                    </Text>
                    <br />
                    <Text type="secondary">
                      {t("users.form.messages.password_auto_generated_info")}
                    </Text>
                  </div>
                </div>
              </div>
            </div>
          )}

          {showButtons && !disabled && (
            <FormActions
              className={styles.formActionFooter}
              onCancel={onCancel ? handleCancel : undefined}
              submitText={submitText || t("users.form.buttons.submit_create")}
              loading={isPending}
              submitDisabled={isSubmitDisabled}
            />
          )}
        </Form>
      </>
    );
  },
);

export default UserForm;
