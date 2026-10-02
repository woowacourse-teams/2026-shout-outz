#!/usr/bin/env bash

set -Eeuo pipefail

readonly SERVICE_USER="shoutoutz"
readonly APP_JAR="/opt/shout-outz/app/app.jar"
readonly ENV_FILE="/opt/shout-outz/shout-outz.env"
readonly SERVICE_FILE="/etc/systemd/system/shout-outz-backend.service"

[[ -f "$APP_JAR" ]] || {
  echo "[after-install] ERROR: 애플리케이션 JAR이 없습니다: ${APP_JAR}" >&2
  exit 1
}

[[ -f "$SERVICE_FILE" ]] || {
  echo "[after-install] ERROR: systemd 서비스 파일이 없습니다: ${SERVICE_FILE}" >&2
  exit 1
}

chown root:"$SERVICE_USER" "$APP_JAR" "$ENV_FILE"
chmod 0640 "$APP_JAR" "$ENV_FILE"

chown root:root "$SERVICE_FILE"
chmod 0644 "$SERVICE_FILE"

systemctl daemon-reload

echo "[after-install] 애플리케이션 파일과 systemd 서비스를 설치했습니다."
