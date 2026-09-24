#!/bin/bash
# =============================================================================
# Script d'exécution de la migration incident_type_configs
# Supporte les environnements: dev, test, prod
#
# Usage: ./run_migration.sh [dev|test|prod]
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV="${1:-dev}"

echo "=== Migration incident_type_configs - Environnement: $ENV ==="

# Configuration par environnement
case "$ENV" in
  dev)
    INCIDENT_DB_HOST="localhost"
    INCIDENT_DB_PORT="5434"
    INCIDENT_DB_NAME="incidentservicedb"
    INCIDENT_DB_USER="postgres"
    INCIDENT_DB_PASSWORD="root"
    ADMIN_DB_HOST="localhost"
    ADMIN_DB_PORT="5433"
    ADMIN_DB_NAME="adminservicedb"
    ADMIN_DB_USER="postgres"
    ADMIN_DB_PASSWORD="root"
    ;;
  test)
    INCIDENT_DB_HOST="${TEST_INCIDENT_DB_HOST:-localhost}"
    INCIDENT_DB_PORT="${TEST_INCIDENT_DB_PORT:-5434}"
    INCIDENT_DB_NAME="${TEST_INCIDENT_DB_NAME:-incidentservicedb_test}"
    INCIDENT_DB_USER="${TEST_INCIDENT_DB_USER:-postgres}"
    INCIDENT_DB_PASSWORD="${TEST_INCIDENT_DB_PASSWORD:-root}"
    ADMIN_DB_HOST="${TEST_ADMIN_DB_HOST:-localhost}"
    ADMIN_DB_PORT="${TEST_ADMIN_DB_PORT:-5433}"
    ADMIN_DB_NAME="${TEST_ADMIN_DB_NAME:-adminservicedb_test}"
    ADMIN_DB_USER="${TEST_ADMIN_DB_USER:-postgres}"
    ADMIN_DB_PASSWORD="${TEST_ADMIN_DB_PASSWORD:-root}"
    ;;
  prod)
    INCIDENT_DB_HOST="${PROD_INCIDENT_DB_HOST:?Variable PROD_INCIDENT_DB_HOST requise}"
    INCIDENT_DB_PORT="${PROD_INCIDENT_DB_PORT:-5432}"
    INCIDENT_DB_NAME="${PROD_INCIDENT_DB_NAME:?Variable PROD_INCIDENT_DB_NAME requise}"
    INCIDENT_DB_USER="${PROD_INCIDENT_DB_USER:?Variable PROD_INCIDENT_DB_USER requise}"
    INCIDENT_DB_PASSWORD="${PROD_INCIDENT_DB_PASSWORD:?Variable PROD_INCIDENT_DB_PASSWORD requise}"
    ADMIN_DB_HOST="${PROD_ADMIN_DB_HOST:?Variable PROD_ADMIN_DB_HOST requise}"
    ADMIN_DB_PORT="${PROD_ADMIN_DB_PORT:-5432}"
    ADMIN_DB_NAME="${PROD_ADMIN_DB_NAME:?Variable PROD_ADMIN_DB_NAME requise}"
    ADMIN_DB_USER="${PROD_ADMIN_DB_USER:?Variable PROD_ADMIN_DB_USER requise}"
    ADMIN_DB_PASSWORD="${PROD_ADMIN_DB_PASSWORD:?Variable PROD_ADMIN_DB_PASSWORD requise}"
    ;;
  *)
    echo "Environnement inconnu: $ENV. Utiliser: dev, test, ou prod"
    exit 1
    ;;
esac

export PGPASSWORD="$ADMIN_DB_PASSWORD"

# Fonction de connexion psql vers admin_db
psql_admin() {
    psql -h "$ADMIN_DB_HOST" -p "$ADMIN_DB_PORT" -U "$ADMIN_DB_USER" -d "$ADMIN_DB_NAME" "$@"
}

# Étape 1: Vérifier la connectivité
echo ""
echo "--- Étape 1: Vérification de la connectivité ---"
psql_admin -c "SELECT 1" > /dev/null && echo "Connexion admin_db: OK" || { echo "Erreur: impossible de se connecter à admin_db"; exit 1; }

# Vérifier que l'extension dblink est disponible
psql_admin -c "CREATE EXTENSION IF NOT EXISTS dblink;" && echo "Extension dblink: OK"

# Étape 2: Point de contrôle - état avant migration
echo ""
echo "--- Étape 2: État avant migration ---"
BEFORE_COUNT=$(psql_admin -t -c "SELECT COUNT(*) FROM incident_type_configs;" | tr -d ' ')
echo "Lignes dans admin_db avant migration: $BEFORE_COUNT"

# Étape 3: Exécution de la migration dans une transaction
echo ""
echo "--- Étape 3: Exécution de la migration ---"

# Remplacer les paramètres de connexion dans le script de migration
MIGRATION_SQL=$(sed \
    -e "s/host=localhost port=5434 dbname=incidentservicedb user=postgres password=root/host=$INCIDENT_DB_HOST port=$INCIDENT_DB_PORT dbname=$INCIDENT_DB_NAME user=$INCIDENT_DB_USER password=$INCIDENT_DB_PASSWORD/g" \
    "$SCRIPT_DIR/migrate_incident_type_configs.sql")

echo "$MIGRATION_SQL" | psql_admin && echo "Migration exécutée avec succès"

# Étape 4: Validation post-migration
echo ""
echo "--- Étape 4: Validation post-migration ---"
AFTER_COUNT=$(psql_admin -t -c "SELECT COUNT(*) FROM incident_type_configs;" | tr -d ' ')
echo "Lignes dans admin_db après migration: $AFTER_COUNT"

VALIDATION_SQL=$(sed \
    -e "s/host=localhost port=5434 dbname=incidentservicedb user=postgres password=root/host=$INCIDENT_DB_HOST port=$INCIDENT_DB_PORT dbname=$INCIDENT_DB_NAME user=$INCIDENT_DB_USER password=$INCIDENT_DB_PASSWORD/g" \
    "$SCRIPT_DIR/validate_migration.sql")

echo "$VALIDATION_SQL" | psql_admin

echo ""
echo "=== Migration terminée avec succès pour l'environnement: $ENV ==="
echo "Lignes migrées: $AFTER_COUNT"
