#!/bin/sh

set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)

if [ -f "$ROOT_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$ROOT_DIR/.env"
  set +a
fi

MYSQL_CONTAINER=${MYSQL_CONTAINER_NAME:-fundcat-mysql}
REDIS_CONTAINER=${REDIS_CONTAINER_NAME:-fundcat-redis}
NAMESRV_CONTAINER=${ROCKETMQ_NAMESRV_CONTAINER_NAME:-fundcat-rmq-namesrv}
BROKER_CONTAINER=${ROCKETMQ_BROKER_CONTAINER_NAME:-fundcat-rmq-broker}
DASHBOARD_CONTAINER=${ROCKETMQ_DASHBOARD_CONTAINER_NAME:-fundcat-rmq-dashboard}
WAIT_SECONDS=${INFRA_WAIT_SECONDS:-120}

wait_for_health() {
  container="$1"
  timeout="$2"
  elapsed=0

  while [ "$elapsed" -lt "$timeout" ]; do
    health_status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)
    if [ "$health_status" = "healthy" ] || [ "$health_status" = "running" ]; then
      echo "$container is ready ($health_status)"
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  echo "Timed out waiting for $container to become healthy" >&2
  return 1
}

echo "Starting MySQL and Redis containers..."
sh "$ROOT_DIR/scripts/infra-up.sh" mysql redis

echo "Waiting for infrastructure to become healthy..."
wait_for_health "$MYSQL_CONTAINER" "$WAIT_SECONDS"
wait_for_health "$REDIS_CONTAINER" "$WAIT_SECONDS"

echo "Starting RocketMQ local stack..."
sh "$ROOT_DIR/scripts/start-rocketmq.sh"

echo "Verifying RocketMQ containers..."
wait_for_health "$NAMESRV_CONTAINER" "$WAIT_SECONDS"
wait_for_health "$BROKER_CONTAINER" "$WAIT_SECONDS"
wait_for_health "$DASHBOARD_CONTAINER" "$WAIT_SECONDS"

echo "All required local Docker services are ready:"
echo "- MySQL: $MYSQL_CONTAINER"
echo "- Redis: $REDIS_CONTAINER"
echo "- RocketMQ NameServer: $NAMESRV_CONTAINER"
echo "- RocketMQ Broker(+Proxy): $BROKER_CONTAINER"
echo "- RocketMQ Dashboard: $DASHBOARD_CONTAINER"
