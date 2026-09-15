#!/usr/bin/env bash
# Stop all locally started Renko microservices (ports 5001–5006).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

kill_tree() {
  local pid="$1"
  if ! kill -0 "$pid" 2>/dev/null; then
    return 0
  fi
  local children
  children="$(pgrep -P "$pid" 2>/dev/null || true)"
  for c in $children; do
    kill_tree "$c"
  done
  kill "$pid" 2>/dev/null || true
  sleep 0.2
  kill -9 "$pid" 2>/dev/null || true
}

# Kill by recorded PIDs (mvnw parents + children)
for f in "$ROOT"/scripts/.pid-*; do
  [ -f "$f" ] || continue
  pid="$(cat "$f" 2>/dev/null || true)"
  name="$(basename "$f" | sed 's/^\.pid-//')"
  if [ -n "${pid:-}" ] && kill -0 "$pid" 2>/dev/null; then
    echo "Stopping $name (pid $pid + children)"
    kill_tree "$pid"
  fi
  rm -f "$f"
done

# Free ports even if PID files were stale (previous failed starts)
for port in 5001 5002 5003 5004 5005 5006; do
  pids="$(ss -tlnp 2>/dev/null | awk -v p=":$port" '$4 ~ p"$" {print}' | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | sort -u)"
  if [ -z "$pids" ]; then
    # fallback: fuser
    pids="$(fuser ${port}/tcp 2>/dev/null | tr ' ' '\n' | grep -E '^[0-9]+$' || true)"
  fi
  for pid in $pids; do
    echo "Freeing port $port (pid $pid)"
    kill_tree "$pid"
  done
done

sleep 1
still="$(ss -tln 2>/dev/null | grep -E ':500[1-6]\b' || true)"
if [ -n "$still" ]; then
  echo "Warning — still listening:"
  echo "$still"
else
  echo "Ports 5001–5006 are free."
fi
echo "Done."
