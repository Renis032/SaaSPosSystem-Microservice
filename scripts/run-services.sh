#!/usr/bin/env bash
# Start all microservices locally (MySQL with six pos_* databases required).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
export JWT_SECRET_KEY="${JWT_SECRET_KEY:-a-very-long-random-secret-key-that-is-at-least-32-bytes-long}"
export MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
export MYSQL_PORT="${MYSQL_PORT:-3306}"
export MYSQL_USER="${MYSQL_USER:-root}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-password}"
# Do not inherit a shared MYSQL_DATABASE=pos from the shell / old .env
unset MYSQL_DATABASE || true

"$ROOT/scripts/stop-services.sh"
"$ROOT/scripts/ensure-mysql.sh"

wait_http() {
  local port="$1"
  local name="$2"
  local path="$3"
  local max="${4:-120}"
  for i in $(seq 1 "$max"); do
    code="$(curl -sS -m 2 -o /dev/null -w '%{http_code}' "http://127.0.0.1:${port}${path}" 2>/dev/null || echo 000)"
    # 200/204/401/403 means the server is up (auth may require token)
    if [[ "$code" =~ ^(200|204|401|403|405)$ ]]; then
      echo "  $name ready on :$port (HTTP $code)"
      return 0
    fi
    sleep 2
  done
  echo "ERROR: $name not ready on :$port — last HTTP=$code"
  echo "---- /tmp/${name}.log (tail) ----"
  tail -50 "/tmp/${name}.log" 2>/dev/null || true
  return 1
}

start_svc() {
  local name="$1"
  local port="$2"
  local db="$3"
  echo "Starting $name on :$port (db=$db) ..."
  (
    cd "$ROOT/services/$name"
    # Force the correct DB even if the parent shell exported MYSQL_DATABASE=pos
    env MYSQL_DATABASE="$db" SERVER_PORT="$port" ./mvnw -q spring-boot:run
  ) >"/tmp/${name}.log" 2>&1 &
  echo $! > "$ROOT/scripts/.pid-$name"
}

mkdir -p "$ROOT/scripts"

start_svc auth-service 5001 pos_auth
wait_http 5001 auth-service /api/dev/demo-info 120

start_svc store-service 5002 pos_store
start_svc catalog-service 5003 pos_catalog
start_svc sales-service 5004 pos_sales
start_svc billing-service 5005 pos_billing
start_svc report-service 5006 pos_report

wait_http 5002 store-service /api/dev/clear-db 120 || wait_http 5002 store-service /api/stores 120
wait_http 5003 catalog-service /api/dev/clear-db 120 || wait_http 5003 catalog-service /api/products 120
wait_http 5004 sales-service /api/dev/clear-db 120 || wait_http 5004 sales-service /api/orders 120
wait_http 5005 billing-service /api/dev/clear-db 120 || wait_http 5005 billing-service /api/billing/subscription/1 120
wait_http 5006 report-service /api/dev/clear-db 120 || wait_http 5006 report-service /api/reports/store/1 120

echo ""
echo "All services are up."
echo "  auth     http://localhost:5001  (pos_auth)"
echo "  store    http://localhost:5002  (pos_store)"
echo "  catalog  http://localhost:5003  (pos_catalog)"
echo "  sales    http://localhost:5004  (pos_sales)"
echo "  billing  http://localhost:5005  (pos_billing)"
echo "  report   http://localhost:5006  (pos_report)"
echo ""
echo "Gateway (if needed):"
echo "  sudo docker run --rm --network host -v \"$ROOT/gateway/nginx.local.conf:/etc/nginx/nginx.conf:ro\" nginx:1.27-alpine"
echo ""
echo "Logs: /tmp/*-service.log"
