#!/bin/sh

set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
API_DIR="$ROOT_DIR/app"
MODE="${1:-h2}"

if [ -f "$ROOT_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$ROOT_DIR/.env"
  set +a
fi

REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD:-change-me-redis-password}
AUTH_SESSION_STORE=${AUTH_SESSION_STORE:-memory}

export REDIS_HOST
export REDIS_PORT
export REDIS_PASSWORD
export AUTH_SESSION_STORE
export SPRING_DATA_REDIS_HOST="$REDIS_HOST"
export SPRING_DATA_REDIS_PORT="$REDIS_PORT"
export SPRING_DATA_REDIS_PASSWORD="$REDIS_PASSWORD"

JAVA_HOME=$(/usr/libexec/java_home -v 21)
export JAVA_HOME
PATH="$JAVA_HOME/bin:$PATH"
export PATH

cd "$API_DIR"

case "$MODE" in
  h2)
    AUTH_SESSION_STORE=memory
    export AUTH_SESSION_STORE
    exec mvn spring-boot:run
    ;;
  redis)
    AUTH_SESSION_STORE=redis
    export AUTH_SESSION_STORE
    exec mvn spring-boot:run
    ;;
  mysql)
    AUTH_SESSION_STORE=redis
    export AUTH_SESSION_STORE
    exec mvn spring-boot:run -Dspring-boot.run.profiles=mysql
    ;;
  *)
    echo "Usage: sh ./scripts/run-api.sh [h2|redis|mysql]" >&2
    exit 1
    ;;
esac
