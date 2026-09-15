#!/usr/bin/env bash
# Ensure MySQL is reachable and all six service databases exist (local dev).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOST="${MYSQL_HOST:-127.0.0.1}"
PORT="${MYSQL_PORT:-3306}"
USER="${MYSQL_USER:-root}"
PASS="${MYSQL_PASSWORD:-password}"

export MYSQL_HOST="$HOST"
export MYSQL_PORT="$PORT"
export MYSQL_USER="$USER"
export MYSQL_PASSWORD="$PASS"

mysql_ping() {
  mysql -h"$HOST" -P"$PORT" -u"$USER" -p"$PASS" -e "SELECT 1" >/dev/null 2>&1
}

if mysql_ping; then
  echo "MySQL is up at ${HOST}:${PORT}"
else
  echo "MySQL is not reachable at ${HOST}:${PORT}."
  if command -v docker >/dev/null 2>&1; then
    echo "Starting MySQL via Docker (init creates pos_auth … pos_report)..."
    docker run -d --name renko-mysql-dev \
      -e MYSQL_ROOT_PASSWORD="$PASS" \
      -e MYSQL_DATABASE=pos_auth \
      -p "${PORT}:3306" \
      -v "$ROOT/scripts/mysql-init:/docker-entrypoint-initdb.d:ro" \
      mysql:8.4 >/dev/null 2>&1 || docker start renko-mysql-dev >/dev/null 2>&1 || true
    for i in $(seq 1 60); do
      if mysql_ping; then
        echo "MySQL ready."
        break
      fi
      sleep 2
      if [ "$i" -eq 60 ]; then
        echo "Timed out waiting for MySQL. Start it manually or run: docker compose up mysql -d"
        exit 1
      fi
    done
  else
    echo "Install MySQL 8+ or Docker, then create databases:"
    echo "  mysql -h127.0.0.1 -uroot -p < scripts/mysql-init/01-databases.sql"
    exit 1
  fi
fi

mysql -h"$HOST" -P"$PORT" -u"$USER" -p"$PASS" < "$ROOT/scripts/mysql-init/01-databases.sql"
echo "Databases pos_auth, pos_store, pos_catalog, pos_sales, pos_billing, pos_report ensured."
