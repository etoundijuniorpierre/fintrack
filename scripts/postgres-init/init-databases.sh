#!/bin/sh
set -eu

if [ -z "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
  echo "POSTGRES_MULTIPLE_DATABASES is empty; no extra database to create."
  exit 0
fi

echo "Creating PostgreSQL databases: ${POSTGRES_MULTIPLE_DATABASES}"

for database in $(echo "$POSTGRES_MULTIPLE_DATABASES" | tr ',' ' '); do
  database="$(echo "$database" | xargs)"

  if [ -z "$database" ]; then
    continue
  fi

  case "$database" in
    *[!a-zA-Z0-9_]*)
      echo "Invalid database name '$database'. Only letters, digits and underscores are allowed." >&2
      exit 1
      ;;
  esac

  if psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "${POSTGRES_DB:-postgres}" -tAc "SELECT 1 FROM pg_database WHERE datname = '$database'" | grep -q 1; then
    echo "Database '$database' already exists."
  else
    echo "Creating database '$database'."
    createdb --username "$POSTGRES_USER" "$database"
  fi
done

echo "PostgreSQL database initialization complete."
