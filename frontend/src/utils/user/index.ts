// Point d'entree principal (export) du module.

export { isCurrentUser, canPerformDestructiveAction } from "./permissions/userPermissions";
export {
  type UserSortPreset,
  USER_SORT_PRESETS,
  resolveUserSortPreset,
} from "./sort/userSort";
