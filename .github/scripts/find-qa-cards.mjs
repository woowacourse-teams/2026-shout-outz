// 처리 대상 QA 카드(AI 작업 상태=AI 대기, 작업 범위=frontend)를 오래된 순으로 최대 3개 조회한다.
// 결과는 GITHUB_OUTPUT에 card_ids(공백 구분), count로 기록한다.
import { appendFileSync } from "node:fs";

const MAX_CARDS = 3;
const { NOTION_TOKEN, NOTION_QA_DATABASE_ID, GITHUB_OUTPUT } = process.env;

if (!NOTION_TOKEN || !NOTION_QA_DATABASE_ID) {
  console.error("NOTION_TOKEN, NOTION_QA_DATABASE_ID가 필요합니다.");
  process.exit(1);
}

const res = await fetch(
  `https://api.notion.com/v1/databases/${NOTION_QA_DATABASE_ID}/query`,
  {
    method: "POST",
    headers: {
      Authorization: `Bearer ${NOTION_TOKEN}`,
      "Notion-Version": "2022-06-28",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      filter: {
        and: [
          { property: "AI 작업 상태", select: { equals: "AI 대기" } },
          { property: "작업 범위", select: { equals: "frontend" } },
        ],
      },
      sorts: [{ timestamp: "created_time", direction: "ascending" }],
      page_size: MAX_CARDS,
    }),
  },
);

if (!res.ok) {
  console.error(`Notion API 오류 ${res.status}: ${await res.text()}`);
  process.exit(1);
}

const { results } = await res.json();
const ids = results.map((page) => page.id);

console.log(`대상 카드 ${ids.length}건: ${ids.join(", ") || "없음"}`);
// 로컬 실행처럼 GITHUB_OUTPUT이 없으면 출력 파일 기록을 건너뛴다.
if (GITHUB_OUTPUT) {
  appendFileSync(GITHUB_OUTPUT, `card_ids=${ids.join(" ")}\ncount=${ids.length}\n`);
}