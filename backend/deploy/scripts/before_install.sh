#!/usr/bin/env bash

set -Eeuo pipefail

readonly SERVICE_USER="shoutoutz"
readonly APP_ROOT="/opt/shout-outz"
readonly APP_DIR="${APP_ROOT}/app"
readonly ENV_FILE="${APP_ROOT}/shout-outz.env"
readonly LOG_DIR="/var/log/shout-outz"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

log() {
  printf '[before-install] %s\n' "$*"
}

if ! id "$SERVICE_USER" >/dev/null 2>&1; then
  log "서비스 사용자 ${SERVICE_USER}를 생성합니다."
  useradd \
    --system \
    --user-group \
    --home-dir "$APP_ROOT" \
    --shell /usr/sbin/nologin \
    "$SERVICE_USER"
fi

install -d -o root -g "$SERVICE_USER" -m 0750 "$APP_ROOT"
install -d -o root -g "$SERVICE_USER" -m 0750 "$APP_DIR"
# 애플리케이션이 직접 로그 파일을 만들고 롤링하므로 서비스 사용자가 소유한다.
install -d -o "$SERVICE_USER" -g "$SERVICE_USER" -m 0750 "$LOG_DIR"

# 최초 배포에는 ApplicationStop이 돌지 않으므로 여기서도 사전 검사를 한다.
bash "${SCRIPT_DIR}/preflight.sh"

chown root:"$SERVICE_USER" "$ENV_FILE"
chmod 0640 "$ENV_FILE"

log "설치 준비를 마쳤습니다."
