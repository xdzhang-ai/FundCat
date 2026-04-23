#!/bin/sh

set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)

if [ -f "$ROOT_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$ROOT_DIR/.env"
  set +a
fi

NAMESRV_CONTAINER=${ROCKETMQ_NAMESRV_CONTAINER_NAME:-fundcat-rmq-namesrv}
BROKER_CONTAINER=${ROCKETMQ_BROKER_CONTAINER_NAME:-fundcat-rmq-broker}
DASHBOARD_CONTAINER=${ROCKETMQ_DASHBOARD_CONTAINER_NAME:-fundcat-rmq-dashboard}
WAIT_SECONDS=${INFRA_WAIT_SECONDS:-120}
ROCKETMQ_TOPIC=${FUNDCAT_ROCKETMQ_TOPIC:-fund_nav_ready_batch}
BROKER_CONF_FILE="$ROOT_DIR/config/rocketmq/broker.conf"

resolve_host_ip() {
  if [ -n "${ROCKETMQ_HOST_IP:-}" ]; then
    printf '%s\n' "$ROCKETMQ_HOST_IP"
    return 0
  fi

  for iface in en0 en1; do
    if ip=$(ipconfig getifaddr "$iface" 2>/dev/null); then
      if [ -n "$ip" ]; then
        printf '%s\n' "$ip"
        return 0
      fi
    fi
  done

  printf '%s\n' "127.0.0.1"
}

wait_for_running() {
  container="$1"
  timeout="$2"
  elapsed=0

  while [ "$elapsed" -lt "$timeout" ]; do
    state=$(docker inspect --format '{{.State.Status}}' "$container" 2>/dev/null || true)
    if [ "$state" = "running" ]; then
      echo "$container is ready ($state)"
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  echo "Timed out waiting for $container to become running" >&2
  return 1
}

echo "Starting RocketMQ namesrv, broker(with proxy) and dashboard..."
HOST_IP=$(resolve_host_ip)
python3 - "$BROKER_CONF_FILE" "$HOST_IP" <<'PY'
from pathlib import Path
import re
import sys

conf_path = Path(sys.argv[1])
host_ip = sys.argv[2]
text = conf_path.read_text()
updated = re.sub(r"^brokerIP1\s*=.*$", f"brokerIP1 = {host_ip}", text, flags=re.MULTILINE)
if updated == text:
    updated = text.rstrip() + f"\nbrokerIP1 = {host_ip}\n"
conf_path.write_text(updated)
print(f"Updated brokerIP1 to {host_ip}")
PY
sh "$ROOT_DIR/scripts/infra-up.sh" rocketmq-namesrv rocketmq-broker rocketmq-dashboard

echo "Waiting for RocketMQ components to become running..."
wait_for_running "$NAMESRV_CONTAINER" "$WAIT_SECONDS"
wait_for_running "$BROKER_CONTAINER" "$WAIT_SECONDS"
wait_for_running "$DASHBOARD_CONTAINER" "$WAIT_SECONDS"

echo "Ensuring local topic exists: $ROCKETMQ_TOPIC"
docker exec "$BROKER_CONTAINER" sh -lc \
  "/home/rocketmq/rocketmq-5.3.2/bin/mqadmin updateTopic -n rocketmq-namesrv:9876 -b $HOST_IP:19011 -t $ROCKETMQ_TOPIC" >/dev/null

echo "RocketMQ local stack is up."
echo "Proxy endpoint: ${FUNDCAT_ROCKETMQ_ENDPOINTS:-127.0.0.1:${ROCKETMQ_PROXY_PORT_8081:-18081}}"
echo "Dashboard URL: http://127.0.0.1:${ROCKETMQ_DASHBOARD_PORT:-18088}"
