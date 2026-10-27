#!/usr/bin/env bash
# Ensure a Kafka broker is reachable on localhost:9092 (local process runs).
set -euo pipefail
HOST="${KAFKA_HOST:-127.0.0.1}"
PORT="${KAFKA_PORT:-9092}"

kafka_up() {
  timeout 2 bash -c "echo > /dev/tcp/${HOST}/${PORT}" >/dev/null 2>&1
}

if kafka_up; then
  echo "Kafka is up at ${HOST}:${PORT}"
  exit 0
fi

echo "Kafka is not reachable at ${HOST}:${PORT}."
if ! command -v docker >/dev/null 2>&1; then
  echo "Start Kafka (docker compose up kafka -d) or set KAFKA_ENABLED=false to use HTTP audit fallback."
  exit 1
fi

echo "Starting Kafka via Docker..."
docker run -d --name renko-kafka-dev \
  -p "${PORT}:9092" \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:${PORT} \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e CLUSTER_ID=4L6g3nShT-eMCtK--X86sw \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
  apache/kafka:3.9.1 >/dev/null 2>&1 || docker start renko-kafka-dev >/dev/null 2>&1 || true

for i in $(seq 1 30); do
  if kafka_up; then
    echo "Kafka ready at ${HOST}:${PORT}"
    exit 0
  fi
  sleep 2
done

echo "Timed out waiting for Kafka. Start it with: docker compose up kafka -d"
echo "Or set KAFKA_ENABLED=false to keep HTTP audit forwarding."
exit 1
