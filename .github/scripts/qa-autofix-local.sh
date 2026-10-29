#!/usr/bin/env bash
# QA Autofix를 GitHub Actions(qa-autofix.yml)와 같은 조건으로 로컬에서 실행한다.
# 로컬 드라이런에 개인 Claude 설정(권한, 모델)이나 다른 Node 버전이 섞이면 CI에서만 나는 문제를 놓치기 때문이다.
#
# 사용: .github/scripts/qa-autofix-local.sh [카드 ID...]
#   카드 ID를 생략하면 CI처럼 find-qa-cards.mjs로 대상 카드를 조회한다.
#   실제 Notion 카드 상태를 바꾸고 이슈·브랜치·PR을 만든다. 테스트용 카드로 실행할 것.
#
# 필요한 환경 변수
#   NOTION_TOKEN             Notion 통합 토큰
#   CLAUDE_CODE_OAUTH_TOKEN  `claude setup-token`으로 발급 (또는 ANTHROPIC_API_KEY)
#   NOTION_QA_DATABASE_ID    카드 ID를 생략할 때만 필요
# 선택 환경 변수
#   QA_AUTOFIX_REF    작업 폴더를 만들 원격 브랜치 (기본 develop). 스킬·워크플로우 수정 브랜치를 시험할 때 지정
#   QA_AUTOFIX_MODEL  사용할 모델 (기본: CI 실행 로그에 찍힌 claude-sonnet-5-5)
#
# CI와 다른 점: GitHub 인증은 봇 토큰이 아니라 로컬 gh 로그인 계정을 쓴다. 토큰 권한 문제는 여기서 재현되지 않는다.
set -euo pipefail

REF="${QA_AUTOFIX_REF:-develop}"
MODEL="${QA_AUTOFIX_MODEL:-claude-sonnet-5-5}"
REPO_ROOT="$(git rev-parse --show-toplevel)"

if [ -z "${NOTION_TOKEN:-}" ]; then
  echo "NOTION_TOKEN이 필요합니다." >&2
  exit 1
fi
if [ -z "${CLAUDE_CODE_OAUTH_TOKEN:-}" ] && [ -z "${ANTHROPIC_API_KEY:-}" ]; then
  echo "CLAUDE_CODE_OAUTH_TOKEN(\`claude setup-token\`으로 발급) 또는 ANTHROPIC_API_KEY가 필요합니다." >&2
  echo "개인 Claude 로그인 정보는 개인 설정과 함께 차단하므로 쓰지 않습니다." >&2
  exit 1
fi

# 실행 기록은 작업 폴더를 지운 뒤에도 남도록 바깥에 둔다.
RUN_DIR="$(mktemp -d "${TMPDIR:-/tmp}/qa-autofix-local.XXXXXX")"
WORKTREE="$RUN_DIR/repo"
STREAM_FILE="$RUN_DIR/claude-stream.jsonl"
EXECUTION_FILE="$RUN_DIR/claude-execution-output.json"

cleanup() {
  git -C "$REPO_ROOT" worktree remove --force "$WORKTREE" 2>/dev/null || true
  echo "실행 기록: $RUN_DIR"
}
trap cleanup EXIT

# CI의 checkout처럼 원격 브랜치 기준의 깨끗한 작업 폴더를 만든다. 추적하지 않는 개인 파일(settings.local.json 등)은 들어오지 않는다.
git -C "$REPO_ROOT" fetch --quiet origin "$REF"
git -C "$REPO_ROOT" worktree add --quiet --detach "$WORKTREE" "origin/$REF"
cd "$WORKTREE"

# CI의 setup-node처럼 frontend/.nvmrc의 Node 버전을 쓴다.
REQUIRED_NODE="$(tr -d '[:space:]' < frontend/.nvmrc)"
if [ "$(node -v 2>/dev/null)" != "v$REQUIRED_NODE" ]; then
  NVM_SH="${NVM_DIR:-$HOME/.nvm}/nvm.sh"
  if [ ! -s "$NVM_SH" ]; then
    echo "Node v$REQUIRED_NODE이 필요합니다. 현재: $(node -v 2>/dev/null || echo 없음)" >&2
    exit 1
  fi
  set +u
  # shellcheck disable=SC1090
  . "$NVM_SH"
  nvm use "$REQUIRED_NODE" >/dev/null
  set -u
fi
echo "Node $(node -v), npm $(npm -v)"

npm ci --prefix frontend --no-audit --no-fund

# 워크플로우의 "Exclude QA autofix temp files" 단계와 같다. 작업 폴더끼리 공유하는 exclude 파일이라 이미 있으면 다시 넣지 않는다.
EXCLUDE_FILE="$(git rev-parse --git-common-dir)/info/exclude"
grep -qxF ".qa-autofix-tmp/" "$EXCLUDE_FILE" 2>/dev/null || echo ".qa-autofix-tmp/" >> "$EXCLUDE_FILE"

if [ "$#" -gt 0 ]; then
  CARD_IDS="$*"
else
  OUTPUT_FILE="$RUN_DIR/find-output"
  GITHUB_OUTPUT="$OUTPUT_FILE" node .github/scripts/find-qa-cards.mjs
  CARD_IDS="$(sed -n 's/^card_ids=//p' "$OUTPUT_FILE")"
fi
if [ -z "$CARD_IDS" ]; then
  echo "처리할 카드가 없습니다."
  exit 0
fi

# 워크플로우의 claude_args 블록을 그대로 읽어 같은 MCP 설정, 허용 명령, 턴 한도로 실행한다.
CLAUDE_ARGS=()
while IFS= read -r -d '' arg; do
  CLAUDE_ARGS+=("$arg")
done < <(node -e '
  const lines = require("fs").readFileSync(process.argv[1], "utf8").split("\n");
  const start = lines.findIndex((line) => /^\s*claude_args:\s*\|/.test(line));
  if (start === -1) throw new Error("qa-autofix.yml에서 claude_args를 찾지 못했습니다.");
  const indent = lines[start].search(/\S/);
  const args = [];
  for (const line of lines.slice(start + 1)) {
    if (line.trim() === "") continue;
    if (line.search(/\S/) <= indent) break;
    const [, flag, value] = line.trim().match(/^(--\S+)(?:\s+(.*))?$/);
    args.push(flag);
    if (value) args.push(value.replace(/^"(.*)"$/, "$1"));
  }
  process.stdout.write(args.map((arg) => arg + "\0").join(""));
' .github/workflows/qa-autofix.yml)

# 개인 설정(~/.claude)을 쓰지 않도록 빈 설정 폴더를 준다. claude-code-action이 만드는 사용자 설정과 같은 값만 넣는다.
CLAUDE_CONFIG_DIR="$RUN_DIR/claude-config"
mkdir -p "$CLAUDE_CONFIG_DIR"
echo '{"enableAllProjectMcpServers": true}' > "$CLAUDE_CONFIG_DIR/settings.json"

echo "카드: $CARD_IDS"
echo "모델: $MODEL"
echo "인자: ${CLAUDE_ARGS[*]}"

# git config는 원본 저장소와 공유되므로 워크플로우의 커밋 작성자 설정은 환경 변수로 대신한다.
CLAUDE_STATUS=0
CLAUDE_CONFIG_DIR="$CLAUDE_CONFIG_DIR" \
  GIT_AUTHOR_NAME="qa-autofix-bot" GIT_AUTHOR_EMAIL="qa-autofix-bot@users.noreply.github.com" \
  GIT_COMMITTER_NAME="qa-autofix-bot" GIT_COMMITTER_EMAIL="qa-autofix-bot@users.noreply.github.com" \
  claude -p "/qa-autofix $CARD_IDS" "${CLAUDE_ARGS[@]}" \
    --model "$MODEL" --output-format stream-json --verbose \
  | tee "$STREAM_FILE" \
  | node -e '
    // 진행 상황만 짧게 보여준다. 전체 내용은 실행 기록 파일에 남는다.
    const rl = require("readline").createInterface({ input: process.stdin });
    rl.on("line", (line) => {
      let message;
      try { message = JSON.parse(line); } catch { return; }
      if (message.type !== "assistant") return;
      for (const block of message.message?.content ?? []) {
        if (block.type === "text") console.log(`💬 ${block.text.slice(0, 300)}`);
        if (block.type === "tool_use") {
          const detail = block.input?.command ?? block.input?.file_path ?? "";
          console.log(`🔧 ${block.name} ${String(detail).split("\n")[0].slice(0, 200)}`);
        }
      }
    });
  ' || CLAUDE_STATUS=$?

# claude-code-action의 실행 기록과 같은 형식(메시지 배열)으로 바꾸고 결과를 요약한다.
node -e '
  const fs = require("fs");
  const [streamFile, executionFile] = process.argv.slice(1);
  const messages = fs.readFileSync(streamFile, "utf8").split("\n").filter(Boolean).flatMap((line) => {
    try { return [JSON.parse(line)]; } catch { return []; }
  });
  fs.writeFileSync(executionFile, JSON.stringify(messages, null, 2));
  const result = messages.findLast((message) => message.type === "result");
  if (!result) {
    console.log("결과 메시지가 없습니다.");
  } else {
    console.log(`결과: ${result.subtype}, 턴 ${result.num_turns}, 비용 $${result.total_cost_usd}`);
    for (const { tool_name, tool_input } of result.permission_denials ?? []) {
      console.log(`거부: ${tool_name} ${tool_input?.command ?? JSON.stringify(tool_input)}`);
    }
  }
' "$STREAM_FILE" "$EXECUTION_FILE" || echo "실행 기록을 정리하지 못했습니다. 원본: $STREAM_FILE"

# 워크플로우의 "Mark stuck cards as failed" 단계처럼 결과와 관계없이 실행한다.
# shellcheck disable=SC2086
EXECUTION_FILE="$EXECUTION_FILE" node .github/scripts/fail-stuck-qa-cards.mjs $CARD_IDS

exit "$CLAUDE_STATUS"
