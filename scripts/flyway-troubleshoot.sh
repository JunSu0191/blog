#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"

load_env_file() {
  local env_file="$1"
  [[ -f "${env_file}" ]] || return 0

  while IFS= read -r line || [[ -n "${line}" ]]; do
    line="${line%$'\r'}"
    [[ -z "${line}" ]] && continue
    [[ "${line}" =~ ^[[:space:]]*# ]] && continue

    local key="${line%%=*}"
    local value="${line#*=}"
    key="$(echo "${key}" | tr -d '[:space:]')"
    [[ -z "${key}" ]] && continue
    [[ "${key}" =~ [^A-Za-z0-9_] ]] && continue

    if [[ "${value}" =~ ^\".*\"$ ]]; then
      value="${value:1:${#value}-2}"
    elif [[ "${value}" =~ ^\'.*\'$ ]]; then
      value="${value:1:${#value}-2}"
    fi

    export "${key}=${value}"
  done < "${env_file}"
}

load_env_file "${ENV_FILE}"

DB_URL_RAW="${DB_URL:-jdbc:mysql://127.0.0.1:3306/blog}"
DB_USER="${DB_USERNAME:-root}"
DB_PASS="${DB_PASSWORD:-}"

DB_HOST=""
DB_PORT=""
DB_NAME=""

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/flyway-troubleshoot.sh status
  ./scripts/flyway-troubleshoot.sh locks
  ./scripts/flyway-troubleshoot.sh log [lines]
  ./scripts/flyway-troubleshoot.sh kill <mysql_process_id> [--yes]
  ./scripts/flyway-troubleshoot.sh mark-success <version> [--yes]
  ./scripts/flyway-troubleshoot.sh delete-failed <version> [--yes]

Commands:
  status        Show Flyway current/failed versions and connection info.
  locks         Show MySQL metadata-lock/Flyway-lock related sessions.
  log           Tail logs/application.log (default lines: 200).
  kill          Kill one MySQL processlist session id.
  mark-success  Set flyway_schema_history.success=1 for failed version.
  delete-failed Delete flyway_schema_history failed row for version.

Notes:
  - Destructive commands ask confirmation unless --yes is passed.
  - mark-success should be used only after confirming schema changes are already applied.
USAGE
}

parse_db_url() {
  local raw="${1#jdbc:mysql://}"
  raw="${raw%%\?*}"

  local host_port="${raw%%/*}"
  local db_name="${raw#*/}"

  if [[ -z "${host_port}" || -z "${db_name}" || "${db_name}" == "${raw}" ]]; then
    echo "[error] Failed to parse DB_URL: ${DB_URL_RAW}" >&2
    exit 1
  fi

  if [[ "${host_port}" == *:* ]]; then
    DB_HOST="${host_port%%:*}"
    DB_PORT="${host_port##*:}"
  else
    DB_HOST="${host_port}"
    DB_PORT="3306"
  fi
  DB_NAME="${db_name}"
}

mysql_query_table() {
  local sql="$1"
  MYSQL_PWD="${DB_PASS}" mysql \
    -h"${DB_HOST}" \
    -P"${DB_PORT}" \
    -u"${DB_USER}" \
    -D"${DB_NAME}" \
    -e "${sql}"
}

mysql_query_raw() {
  local sql="$1"
  MYSQL_PWD="${DB_PASS}" mysql \
    -h"${DB_HOST}" \
    -P"${DB_PORT}" \
    -u"${DB_USER}" \
    -D"${DB_NAME}" \
    -N -B \
    -e "${sql}"
}

ensure_mysql_access() {
  if ! mysql_query_raw "SELECT 1" >/dev/null 2>&1; then
    cat <<EOF >&2
[error] Cannot connect to MySQL.
  host=${DB_HOST}
  port=${DB_PORT}
  db=${DB_NAME}
  user=${DB_USER}
Check .env values: DB_URL / DB_USERNAME / DB_PASSWORD
EOF
    exit 1
  fi
}

confirm() {
  local message="$1"
  local force="${2:-}"
  if [[ "${force}" == "--yes" ]]; then
    return 0
  fi
  read -r -p "${message} [y/N] " answer
  [[ "${answer}" == "y" || "${answer}" == "Y" ]]
}

validate_id() {
  local value="$1"
  local kind="$2"
  if [[ ! "${value}" =~ ^[0-9]+$ ]]; then
    echo "[error] ${kind} must be numeric: ${value}" >&2
    exit 1
  fi
}

validate_version() {
  local value="$1"
  if [[ ! "${value}" =~ ^[0-9A-Za-z._-]+$ ]]; then
    echo "[error] invalid version value: ${value}" >&2
    exit 1
  fi
}

cmd_status() {
  ensure_mysql_access
  echo "[connection]"
  echo "  host=${DB_HOST} port=${DB_PORT} db=${DB_NAME} user=${DB_USER}"
  echo

  echo "[flyway latest successful version]"
  mysql_query_table "SELECT MAX(CAST(version AS UNSIGNED)) AS latest_success_version FROM flyway_schema_history WHERE success=1 AND version REGEXP '^[0-9]+$';"
  echo

  local failed_count
  failed_count="$(mysql_query_raw "SELECT COUNT(*) FROM flyway_schema_history WHERE success=0;")"
  echo "[flyway failed rows]"
  if [[ "${failed_count}" == "0" ]]; then
    echo "none"
  else
    mysql_query_table "SELECT installed_rank, version, description, script, installed_on, success FROM flyway_schema_history WHERE success=0 ORDER BY installed_rank;"
  fi
}

cmd_locks() {
  ensure_mysql_access
  echo "[flyway/metadata lock related sessions]"
  mysql_query_table "SELECT ID, USER, HOST, DB, COMMAND, TIME, STATE, LEFT(COALESCE(INFO,''), 180) AS INFO FROM information_schema.PROCESSLIST WHERE DB='${DB_NAME}' AND (STATE LIKE '%metadata lock%' OR INFO LIKE '%Flyway--%') ORDER BY TIME DESC;"
  echo
  echo "[long-lived sleep sessions in ${DB_NAME} (>= 120s)]"
  mysql_query_table "SELECT ID, USER, HOST, COMMAND, TIME, STATE FROM information_schema.PROCESSLIST WHERE DB='${DB_NAME}' AND COMMAND='Sleep' AND TIME >= 120 ORDER BY TIME DESC;"
}

cmd_log() {
  local lines="${1:-200}"
  validate_id "${lines}" "lines"
  local log_file="${ROOT_DIR}/logs/application.log"
  if [[ ! -f "${log_file}" ]]; then
    echo "[error] Log file not found: ${log_file}" >&2
    echo "Start the server once, then retry." >&2
    exit 1
  fi
  tail -n "${lines}" "${log_file}"
}

cmd_kill() {
  local process_id="$1"
  local force="${2:-}"
  validate_id "${process_id}" "mysql_process_id"
  ensure_mysql_access

  if ! confirm "Kill MySQL process id=${process_id}?" "${force}"; then
    echo "cancelled"
    exit 1
  fi

  mysql_query_table "KILL ${process_id};"
  echo "killed process id=${process_id}"
}

cmd_mark_success() {
  local version="$1"
  local force="${2:-}"
  validate_version "${version}"
  ensure_mysql_access

  if ! confirm "Mark failed migration version=${version} as success=1?" "${force}"; then
    echo "cancelled"
    exit 1
  fi

  mysql_query_table "UPDATE flyway_schema_history SET success=1 WHERE version='${version}' AND success=0;"
  mysql_query_table "SELECT installed_rank, version, script, success FROM flyway_schema_history WHERE version='${version}' ORDER BY installed_rank;"
}

cmd_delete_failed() {
  local version="$1"
  local force="${2:-}"
  validate_version "${version}"
  ensure_mysql_access

  if ! confirm "Delete failed migration rows for version=${version}?" "${force}"; then
    echo "cancelled"
    exit 1
  fi

  mysql_query_table "DELETE FROM flyway_schema_history WHERE version='${version}' AND success=0;"
  mysql_query_table "SELECT installed_rank, version, script, success FROM flyway_schema_history WHERE version='${version}' ORDER BY installed_rank;"
}

parse_db_url "${DB_URL_RAW}"

COMMAND="${1:-help}"

case "${COMMAND}" in
  status)
    cmd_status
    ;;
  locks)
    cmd_locks
    ;;
  log)
    cmd_log "${2:-200}"
    ;;
  kill)
    if [[ $# -lt 2 ]]; then
      usage
      exit 1
    fi
    cmd_kill "${2}" "${3:-}"
    ;;
  mark-success)
    if [[ $# -lt 2 ]]; then
      usage
      exit 1
    fi
    cmd_mark_success "${2}" "${3:-}"
    ;;
  delete-failed)
    if [[ $# -lt 2 ]]; then
      usage
      exit 1
    fi
    cmd_delete_failed "${2}" "${3:-}"
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    echo "[error] Unknown command: ${COMMAND}" >&2
    usage
    exit 1
    ;;
esac
