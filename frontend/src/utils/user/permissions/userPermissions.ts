// Helpers d'autorisation utilisateur : compare l'utilisateur courant avec la fiche consultee.

export const isCurrentUser = (
  viewedUserId: string | undefined,
  currentUserId: string | undefined,
): boolean => {
  if (!viewedUserId || !currentUserId) {
    return false;
  }
  return viewedUserId === currentUserId;
};

// Autorise une action destructive uniquement sur un autre utilisateur.
export const canPerformDestructiveAction = (
  viewedUserId: string | undefined,
  currentUserId: string | undefined,
): boolean => {
  return !isCurrentUser(viewedUserId, currentUserId);
};
