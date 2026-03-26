#!/bin/sh

set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)

if [ -f "$ROOT_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$ROOT_DIR/.env"
  set +a
fi

cd "$ROOT_DIR"

IMAGES=$(docker compose config --images | sort -u)

for IMAGE in $IMAGES; do
  if docker image inspect "$IMAGE" >/dev/null 2>&1; then
    echo "Using local image: $IMAGE"
  else
    echo "Local image not found, Docker will pull: $IMAGE"
  fi
done

exec docker compose up -d "$@"
