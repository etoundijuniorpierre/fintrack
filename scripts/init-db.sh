#!/bin/sh
set -eu

# Initialisation complémentaire pour le Docker Compose local.
# Les scripts de point d'entrée PostgreSQL ne s'exécutent que sur un volume vide, donc ce script
# vérifie également les bases de données des services pour les volumes déjà initialisés.

echo "En attente des services d'infrastructure..."

DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-root}
MONGO_ROOT_USER=${MONGO_ROOT_USER:-}
MONGO_ROOT_PASSWORD=${MONGO_ROOT_PASSWORD:-}
DATABASES=${POSTGRES_MULTIPLE_DATABASES:-userservicedb,incidentservicedb,documentservicedb,reportingservicedb}
MONGO_AUTH_ARGS=""
if [ -n "$MONGO_ROOT_USER" ] && [ -n "$MONGO_ROOT_PASSWORD" ]; then
    MONGO_AUTH_ARGS="--username $MONGO_ROOT_USER --password $MONGO_ROOT_PASSWORD --authenticationDatabase admin"
fi

export PGPASSWORD=$DB_PASSWORD

echo "Vérification de PostgreSQL..."
until docker exec fintrack-postgresql pg_isready -U "$DB_USER" -d postgres -p 5434 > /dev/null 2>&1; do
    echo "PostgreSQL n'est pas encore prêt..."
    sleep 2
done

echo "Vérification des bases de données PostgreSQL : $DATABASES"
for database in $(echo "$DATABASES" | tr ',' ' '); do
    database="$(echo "$database" | xargs)"
    if [ -z "$database" ]; then
        continue
    fi

    if docker exec fintrack-postgresql psql -U "$DB_USER" -d postgres -p 5434 -tAc "SELECT 1 FROM pg_database WHERE datname = '$database'" | grep -q 1; then
        echo "La base de données PostgreSQL '$database' existe déjà."
    else
        echo "Création de la base de données PostgreSQL '$database'..."
        docker exec fintrack-postgresql createdb -U "$DB_USER" -p 5434 "$database"
    fi
done

echo "Vérification de MongoDB..."
until docker exec fintrack-mongodb sh -c "mongosh --port 27019 $MONGO_AUTH_ARGS --eval 'db.adminCommand(\"ping\")'" > /dev/null 2>&1; do
    echo "MongoDB n'est pas encore prêt..."
    sleep 2
done

echo "Initialisation des collections et des index MongoDB..."

docker exec fintrack-mongodb sh -c "mongosh --port 27019 $MONGO_AUTH_ARGS" <<'MONGO_AUDIT'
db = db.getSiblingDB("auditservicedb");
if (!db.getCollectionNames().includes("audit_logs")) {
    db.createCollection("audit_logs");
    db.audit_logs.createIndex({ timestamp: 1 });
    print("auditservicedb initialized");
}
MONGO_AUDIT

docker exec fintrack-mongodb sh -c "mongosh --port 27019 $MONGO_AUTH_ARGS" <<'MONGO_NOTIFICATION'
db = db.getSiblingDB("notificationservicedb");
if (!db.getCollectionNames().includes("notifications")) {
    db.createCollection("notifications");
    db.notifications.createIndex({ created_at: -1 });
    db.notifications.createIndex({ recipient: 1 });
    db.notifications.createIndex({ incident_id: 1 });
    db.notifications.createIndex({ status: 1 });
    db.notifications.createIndex({ status: 1, next_retry: 1 });
    print("notificationservicedb initialized");
}
MONGO_NOTIFICATION

if [ -d "database/init" ]; then
    echo "Recherche de scripts d'initialisation optionnels dans database/init/..."
    for script in database/init/01-schema.sql database/init/02-data.sql; do
        if [ -s "$script" ]; then
            echo "Exécution de $script sur userservicedb..."
            docker exec -i fintrack-postgresql psql -U "$DB_USER" -d userservicedb -p 5434 < "$script" || echo "Erreur lors de l'exécution de $script"
        fi
    done
fi

echo "Initialisation terminée."
exit 0
