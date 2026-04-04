#!/usr/bin/env bash

set -euo pipefail

APP_DIR="${1:-/home/ubuntu/app}"
SERVICE_NAME="${2:-blog}"

if [ ! -f "${APP_DIR}/app.jar" ]; then
  echo "app.jar not found: ${APP_DIR}/app.jar" >&2
  exit 1
fi

if [ ! -f "${APP_DIR}/.env" ]; then
  echo ".env not found: ${APP_DIR}/.env" >&2
  exit 1
fi

sudo systemctl daemon-reload
sudo systemctl restart "${SERVICE_NAME}.service"
sudo systemctl --no-pager --full status "${SERVICE_NAME}.service"
