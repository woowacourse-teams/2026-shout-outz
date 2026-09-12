#!/usr/bin/env bash

set -Eeuo pipefail

readonly SERVICE_NAME="shout-outz-backend.service"
readonly HEALTH_URL="http://127.0.0.1:8080/actuator/health"
readonly TIMEOUT_SECONDS=180
readonly RETRY_INTERVAL_SECONDS=5

deadline=$((SECONDS + TIMEOUT_SECONDS))

while (( SECONDS < deadline )); do
  if ! systemctl is-active --quiet "$SERVICE_NAME"; then
    echo "[validate] ERROR: 서비스가 실행 중이 아닙니다." >&2
    journalctl -u "$SERVICE_NAME" -n 100 --no-pager >&2 || true
    exit 1
  fi

  response="$(curl --silent --show-error --fail --max-time 3 "$HEALTH_URL" 2>/dev/null || true)"

  if grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' <<<"$response"; then
    echo "[validate] Actuator 상태가 UP입니다."
    exit 0
  fi

  sleep "$RETRY_INTERVAL_SECONDS"
done

echo "[validate] ERROR: ${TIMEOUT_SECONDS}초 안에 Actuator가 UP이 되지 않았습니다." >&2
journalctl -u "$SERVICE_NAME" -n 100 --no-pager >&2 || true
exit 1
