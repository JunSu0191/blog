#!/usr/bin/env bash

set -euo pipefail

if [ "$#" -lt 2 ]; then
  echo "usage: $0 <info|validate|migrate|repair> <env-file>" >&2
  exit 1
fi

ACTION="$1"
ENV_FILE="$2"
TASK_NAME=""

case "${ACTION}" in
  info)
    TASK_NAME="flywayInfoManual"
    ;;
  validate)
    TASK_NAME="flywayValidateManual"
    ;;
  migrate)
    TASK_NAME="flywayMigrateManual"
    ;;
  repair)
    TASK_NAME="flywayRepairManual"
    ;;
  *)
    echo "unsupported action: ${ACTION}" >&2
    exit 1
    ;;
esac

if [ ! -f "${ENV_FILE}" ]; then
  echo "env file not found: ${ENV_FILE}" >&2
  exit 1
fi

while IFS= read -r line || [ -n "$line" ]; do
  case "$line" in
    ''|'#'*)
      continue
      ;;
  esac

  key="${line%%=*}"
  value="${line#*=}"

  if [ -z "$key" ]; then
    continue
  fi

  export "${key}=${value}"
done < "${ENV_FILE}"

./gradlew --no-daemon "${TASK_NAME}"
