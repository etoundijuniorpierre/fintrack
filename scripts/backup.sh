#!/bin/bash
set -euo pipefail

# Sauvegarde des données PostgreSQL et MongoDB pour la stack Docker Compose actuelle.
# Le script lit DB_USER/DB_PASSWORD depuis .env s'il est présent, puis écrit
# des archives horodatées dans volumes/backups.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/volumes/backups}"
TIMESTAMP="${TIMESTAMP:-$(date +%Y%m%d_%H%M%S)}"

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

mkdir -p "$BACKUP_DIR"

echo "Sauvegarde des bases de données PostgreSQL depuis $POSTGRES_CONTAINER..."
docker exec -e PGPASSWORD="$DB_PASSWORD" "$POSTGRES_CONTAINER" \
  pg_dumpall -U "$DB_USER" > "$BACKUP_DIR/postgres_$TIMESTAMP.sql"

echo "Sauvegarde des bases de données MongoDB depuis $MONGO_CONTAINER..."
docker exec "$MONGO_CONTAINER" \
  mongodump --port "$MONGO_PORT" "${MONGO_AUTH_ARGS[@]}" --archive --gzip > "$BACKUP_DIR/mongo_$TIMESTAMP.archive.gz"

cat > "$BACKUP_DIR/manifest_$TIMESTAMP.txt" <<EOF
timestamp=$TIMESTAMP
postgres_container=$POSTGRES_CONTAINER
mongo_container=$MONGO_CONTAINER
mongo_port=$MONGO_PORT
postgres_archive=postgres_$TIMESTAMP.sql
mongo_archive=mongo_$TIMESTAMP.archive.gz
postgres_databases=userservicedb,incidentservicedb,documentservicedb,reportingservicedb
mongo_databases=auditservicedb,notificationservicedb
EOF

echo "Sauvegarde terminée : $BACKUP_DIR"
echo "Horodatage : $TIMESTAMP"

# --- Nettoyage GFS (Grandfather-Father-Son) ---
echo "Application de la politique de rétention GFS..."
CURRENT_DATE=$(date +%s)
DAY_IN_SECONDS=86400

for file in "$BACKUP_DIR"/*; do
  [ -f "$file" ] || continue
  
  # Récupérer le timestamp Unix du fichier
  FILE_TIMESTAMP=$(stat -c %Y "$file" 2>/dev/null || stat -f %m "$file")
  AGE_DAYS=$(( (CURRENT_DATE - FILE_TIMESTAMP) / DAY_IN_SECONDS ))
  
  # Obtenir le jour de la semaine (0=Dimanche) et le jour du mois (01-31)
  DAY_OF_WEEK=$(date -d "@$FILE_TIMESTAMP" +%w 2>/dev/null || date -r "$FILE_TIMESTAMP" +%w)
  DAY_OF_MONTH=$(date -d "@$FILE_TIMESTAMP" +%d 2>/dev/null || date -r "$FILE_TIMESTAMP" +%d)
  
  KEEP=false
  
  # mise ne place de la Règle d'or ''Grandfather-Father-Son''
  # Son : Garder tous les backups des 7 derniers jours
  if [ "$AGE_DAYS" -le 7 ]; then
    KEEP=true
  # Father : Garder 1 backup par semaine pour le dernier mois (le Dimanche)
    if [ "$DAY_OF_WEEK" -eq 0 ]; then
      KEEP=true
    fi
  elif [ "$AGE_DAYS" -le 365 ]; then
    # Grandfather : Garder 1 backup par mois pour la dernière année (le 1er du mois)
    if [ "$DAY_OF_MONTH" -eq 01 ] || [ "$DAY_OF_MONTH" -eq 1 ]; then
      KEEP=true
    fi
  fi
  
  if [ "$KEEP" = false ]; then
    echo "Suppression (GFS) : $(basename "$file") (Âge: $AGE_DAYS jours)"
    rm -f "$file"
  fi
done
echo "Nettoyage GFS terminé."

# --- Synchronisation Cloud avec Backblaze B2 via rclone ---
if [ -n "${B2_BUCKET_NAME:-}" ]; then
  echo "Synchronisation vers Backblaze B2 (bucket: $B2_BUCKET_NAME)..."
  
  # 1. Installation automatique de rclone si non présent
  if ! command -v rclone >/dev/null 2>&1; then
    echo "rclone n'est pas installé. Installation automatique en cours..."
    if command -v curl >/dev/null 2>&1; then
      curl https://rclone.org/install.sh | sudo bash || bash -c "curl https://rclone.org/install.sh | bash"
    else
      echo "ERREUR: curl n'est pas installé. Impossible d'installer rclone automatiquement."
      exit 1
    fi
  fi

  # 2. Configuration automatique du profil B2 si les clés sont fournies
  if [ -n "${B2_KEY_ID:-}" ] && [ -n "${B2_APPLICATION_KEY:-}" ]; then
    echo "Configuration automatique du profil rclone 'b2'..."
    rclone config create b2 b2 account "$B2_KEY_ID" key "$B2_APPLICATION_KEY" >/dev/null 2>&1
  fi

  # 3. Synchronisation
  # L'utilisation de 'sync' copiera le répertoire à l'identique (effacera sur B2 ce qui a été effacé localement)
  rclone sync "$BACKUP_DIR" "b2:$B2_BUCKET_NAME" --progress
  echo "Synchronisation Cloud terminée."
else
  echo "Information: B2_BUCKET_NAME non défini dans le fichier .env, synchronisation cloud ignorée."
fi
