#!/usr/bin/env bash

set -Eeuo pipefail

readonly SERVICE_NAME="shout-outz-backend.service"

systemctl daemon-reload
systemctl enable "$SERVICE_NAME"

if ! systemctl restart "$SERVICE_NAME"; then
  echo "[start] ERROR: 서비스를 시작하지 못했습니다." >&2
  journalctl -u "$SERVICE_NAME" -n 100 --no-pager >&2 || true
  exit 1
fi

echo "[start] 서비스를 시작했습니다."
