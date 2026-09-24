// Centralise les contrats utilisateur echanges avec le backend.

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
// Centralise la logique d'interface liee a base entity.
export interface BaseEntity {
  id: string;
  createdAt: string;
  updatedAt: string;
  modifiedBy?: string;
}

// Modele le resume d'un utilisateur.
export interface UserSummary {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
}

export interface UserSummaryResponse {
  id: string;
  username: string;
  email?: string;
  firstName?: string;
  lastName?: string;
}

// Centralise la logique d'interface liee a connexion credentials.
export interface LoginCredentials {
  username: string;
  password: string;
}

// Centralise la logique d'interface liee a auth response.
export interface AuthResponse {
  token: string;
  id: string;
  username: string;
  roles: string[];
  permissions: string[];
  serviceId?: string;
  agencyId?: string;
  managedServiceIds?: string[];
  managedAgencyId?: string;
  isFirstLogin?: boolean;
  isActive?: boolean;
}

// Centralise la logique d'interface liee a agence.
export interface Agency extends BaseEntity {
  name: string;
  code?: string;
  city?: string;
  address?: string;
  isActive?: boolean;
  headOfAgency?: UserSummary;
}

// Centralise la logique d'interface liee a service.
export interface Service extends BaseEntity {
  name: string;
  description?: string;
  code?: string;
  headOfService?: UserSummary;
}

// Centralise la logique d'interface liee a role.
export interface Role extends BaseEntity {
  name: string;
  displayName?: string;
  description?: string;
  isSystem?: boolean;
  permissions?: Permission[];
}

// Centralise la logique d'interface liee a permission.
export interface Permission extends BaseEntity {
  name: string;
  description: string;
}

// Modele un utilisateur complet retourne par l'API.
export interface User extends BaseEntity {
  username: string;
  email?: string;
  phoneNumber?: number;
  firstName?: string;
  lastName?: string;
  isActive: boolean;
  isFirstLogin?: boolean;
  avatarDocumentId?: string;
  lastLogin?: string;
  roles?: Role[];
  permissions?: Permission[];
  revokedPermissions?: Permission[];
  agency?: Agency;
  service?: Service;
  managedServiceIds?: string[];
  managedAgencyId?: string;
}

// Prepare utilisateur request pour le flux courant.
export interface CreateUserRequest {
  username?: string;
  email?: string;
  phoneNumber?: number;
  firstName?: string;
  lastName?: string;
  isActive?: boolean;
  avatarDocumentId?: string;
  agencyId?: string;
  serviceId?: string;
  managedServiceIds?: string[];
  managedAgencyId?: string;
  roleIds?: string[];
  permissionIds?: string[];
  revokedPermissionIds?: string[];
}

// Centralise la logique d'interface liee a update utilisateur request.
export interface UpdateUserRequest extends Partial<CreateUserRequest> {
  id: string;
}

// Centralise la logique d'interface liee a role request.
export interface RoleRequest {
  name: string;
  displayName?: string;
  description?: string;
  isSystem?: boolean;
  permissionIds: string[];
}

// Centralise la logique d'interface liee a profil update request.
export interface ProfileUpdateRequest {
  username: string;
  email?: string;
  phoneNumber?: number;
  avatarDocumentId?: string;
}

// Centralise la logique d'interface liee a change mot de passe request.
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
