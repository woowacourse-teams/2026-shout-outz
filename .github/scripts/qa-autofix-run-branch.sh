#!/usr/bin/env bash
# 머지하지 않은 브랜치의 qa-autofix.yml을 실제 GitHub Actions에서 실행하고 끝날 때까지 지켜본다.
# 워크플로우·스킬·스크립트 수정을 머지 전에 CI 환경(봇 토큰, 러너)에서 확인할 때 쓴다.
#
# 사용: .github/scripts/qa-autofix-run-branch.sh [브랜치]
#   브랜치를 생략하면 현재 브랜치를 쓴다. 원격에 push된 내용으로 실행된다.
#   실제 Notion QA 시트의 `AI 대기` 카드를 처리하므로, 시험할 카드만 `AI 대기`로 두고 실행할 것.
set -euo pipefail

WORKFLOW="qa-autofix.yml"
BRANCH="${1:-$(git branch --show-current)}"

if [ -z "$BRANCH" ]; then
  echo "브랜치를 지정해 주세요. (detached HEAD)" >&2
  exit 1
fi

git fetch --quiet origin "$BRANCH" || {
  echo "원격에 $BRANCH 브랜치가 없습니다. 먼저 push해 주세요." >&2
  exit 1
}
# 현재 브랜치를 실행할 때 push하지 않은 커밋이 있으면, 실행 결과가 로컬 코드와 달라지므로 멈춘다.
if [ "$BRANCH" = "$(git branch --show-current)" ] && [ "$(git rev-parse HEAD)" != "$(git rev-parse "origin/$BRANCH")" ]; then
  echo "로컬 $BRANCH와 origin/$BRANCH가 다릅니다. push한 뒤 다시 실행해 주세요." >&2
  exit 1
fi

list_run_ids() {
  gh run list --workflow "$WORKFLOW" --branch "$BRANCH" --event workflow_dispatch --limit 20 --json databaseId --jq '.[].databaseId'
}

# workflow run 명령은 실행 ID를 돌려주지 않으므로, 요청 전에 있던 실행과 비교해 새로 생긴 실행을 찾는다.
BEFORE_IDS=" $(list_run_ids | tr '\n' ' ') "
gh workflow run "$WORKFLOW" --ref "$BRANCH"
echo "$BRANCH 브랜치로 $WORKFLOW 실행을 요청했습니다. 실행이 생성되기를 기다립니다..."

RUN_ID=""
for _ in $(seq 1 20); do
  for id in $(list_run_ids); do
    case "$BEFORE_IDS" in
      *" $id "*) ;;
      *) RUN_ID="$id" ;;
    esac
  done
  [ -n "$RUN_ID" ] && break
  sleep 3
done

if [ -z "$RUN_ID" ]; then
  echo "실행을 찾지 못했습니다. Actions 탭에서 확인해 주세요." >&2
  exit 1
fi

gh run view "$RUN_ID" --json url --jq '"실행: \(.url)"'
gh run watch "$RUN_ID" --exit-status
