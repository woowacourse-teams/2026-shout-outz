#!/usr/bin/env bash

set -Eeuo pipefail

readonly SERVICE_NAME="shout-outz-backend.service"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! systemctl cat "$SERVICE_NAME" >/dev/null 2>&1; then
  echo "[stop] 최초 배포이므로 중지할 서비스가 없습니다."
  exit 0
fi

# 검사에 실패하면 기존 앱을 끄지 않고 배포만 멈춘다.
bash "${SCRIPT_DIR}/preflight.sh"

if systemctl is-active --quiet "$SERVICE_NAME"; then
  systemctl stop "$SERVICE_NAME"
  echo "[stop] 기존 서비스를 중지했습니다."
else
  echo "[stop] 서비스가 이미 중지되어 있습니다."
fi
