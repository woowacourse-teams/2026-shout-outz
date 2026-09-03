#!/usr/bin/env bash

set -Eeuo pipefail

readonly SERVICE_USER="shoutoutz"
readonly APP_ROOT="/opt/shout-outz"
readonly APP_DIR="${APP_ROOT}/app"
readonly ENV_FILE="${APP_ROOT}/shout-outz.env"
readonly MIN_FREE_KB=$((512 * 1024))

log() {
  printf '[before-install] %s\n' "$*"
}

fail() {
  printf '[before-install] ERROR: %s\n' "$*" >&2
  exit 1
}

for command_name in java curl systemctl; do
  command -v "$command_name" >/dev/null 2>&1 \
    || fail "필수 명령을 찾을 수 없습니다: ${command_name}"
done

java_version="$(java -version 2>&1)"
grep -Eq 'version "21([.]|")' <<<"$java_version" \
  || fail "Java 21이 필요합니다. 현재 버전: $(head -n 1 <<<"$java_version")"

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

[[ -f "$ENV_FILE" ]] || fail "환경 변수 파일이 없습니다: ${ENV_FILE}"

missing_variables=()
required_variables=(
  SPRING_DATASOURCE_URL
  SPRING_DATASOURCE_USERNAME
  SPRING_DATASOURCE_PASSWORD
)

for variable_name in "${required_variables[@]}"; do
  variable_line="$(grep -E "^${variable_name}=" "$ENV_FILE" | tail -n 1 || true)"
  variable_value="${variable_line#*=}"

  if [[ -z "$variable_line" || -z "${variable_value//[[:space:]]/}" ]]; then
    missing_variables+=("$variable_name")
  fi
done

if (( ${#missing_variables[@]} > 0 )); then
  fail "필수 환경 변수가 없거나 비어 있습니다: ${missing_variables[*]}"
fi

chown root:"$SERVICE_USER" "$ENV_FILE"
chmod 0640 "$ENV_FILE"

available_kb="$(df -Pk "$APP_ROOT" | awk 'NR == 2 {print $4}')"
[[ "$available_kb" =~ ^[0-9]+$ ]] || fail "남은 디스크 용량을 확인할 수 없습니다."

if (( available_kb < MIN_FREE_KB )); then
  fail "디스크 여유 공간이 512MB보다 적습니다."
fi

log "사전 검증을 통과했습니다."
