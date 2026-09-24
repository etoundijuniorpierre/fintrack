#!/bin/bash
set -euo pipefail

# Restauration des sauvegardes PostgreSQL et MongoDB générées par scripts/backup.sh.
# Utilisation : ./scripts/restore.sh <horodatage>

if [ "${1:-}" = "" ]; then
  echo "Utilisation : ./scripts/restore.sh <horodatage>"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/volumes/backups}"
TIMESTAMP="$1"

if [ -f "$ROOT_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$ROOT_DIR/.env"
  set +a
fi

DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-root}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-fintrack-postgresql}"
MONGO_CONTAINER="${MONGO_CONTAINER:-fintrack-mongodb}"
MONGO_PORT="${MONGO_PORT:-27019}"
MONGO_AUTH_ARGS=()
if [ -n "${MONGO_ROOT_USER:-}" ] && [ -n "${MONGO_ROOT_PASSWORD:-}" ]; then
  MONGO_AUTH_ARGS=(--username "$MONGO_ROOT_USER" --password "$MONGO_ROOT_PASSWORD" --authenticationDatabase admin)
fi

POSTGRES_ARCHIVE="$BACKUP_DIR/postgres_$TIMESTAMP.sql"
MONGO_ARCHIVE="$BACKUP_DIR/mongo_$TIMESTAMP.archive.gz"

if [ ! -f "$POSTGRES_ARCHIVE" ]; then
  echo "Archive PostgreSQL introuvable : $POSTGRES_ARCHIVE"
  exit 1
fi

if [ ! -f "$MONGO_ARCHIVE" ]; then
  echo "Archive MongoDB introuvable : $MONGO_ARCHIVE"
  exit 1
fi

echo "Restauration de PostgreSQL depuis $POSTGRES_ARCHIVE..."
docker exec -i -e PGPASSWORD="$DB_PASSWORD" "$POSTGRES_CONTAINER" \
  psql -U "$DB_USER" -d postgres < "$POSTGRES_ARCHIVE"

echo "Restauration de MongoDB depuis $MONGO_ARCHIVE..."
docker exec -i "$MONGO_CONTAINER" \
  mongorestore --port "$MONGO_PORT" "${MONGO_AUTH_ARGS[@]}" --archive --gzip --drop < "$MONGO_ARCHIVE"

echo "Restauration terminée"
