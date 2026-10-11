// autofix가 시간 초과 등으로 중간에 끝났을 때, 아직 `AI 처리중`인 카드를 `실패`로 바꾸고 이유를 댓글로 남긴다.
// 사용: node fail-stuck-qa-cards.mjs <카드 ID...>
const { NOTION_TOKEN, RUN_URL } = process.env;
const STATUS_PROPERTY = "AI 작업 상태";
const cardIds = process.argv.slice(2);

if (!NOTION_TOKEN) {
  console.error("NOTION_TOKEN이 필요합니다.");
  process.exit(1);
}

const notion = async (path, method, body) => {
  const res = await fetch(`https://api.notion.com/v1/${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${NOTION_TOKEN}`,
      "Notion-Version": "2022-06-28",
      "Content-Type": "application/json",
    },
    body: body && JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`Notion API 오류 ${res.status}: ${await res.text()}`);
  return res.json();
};

const message = [
  "AI 작업이 실행 시간 또는 턴 한도를 넘겨 중간에 종료되어 실패 처리했습니다.",
  "만들다 만 브랜치나 이슈가 남아 있을 수 있습니다. 다시 시도하려면 상태를 `AI 대기`로 바꿔 주세요.",
  RUN_URL && `실행 로그: ${RUN_URL}`,
]
  .filter(Boolean)
  .join("\n");

for (const id of cardIds) {
  const page = await notion(`pages/${id}`, "GET");
  const property = page.properties[STATUS_PROPERTY];
  // 선택(select)과 상태(status) 타입 모두 처리한다.
  const current = property?.[property.type]?.name;
  if (current !== "AI 처리중") {
    console.log(`${id}: ${current ?? "상태 없음"} → 그대로 둠`);
    continue;
  }

  await notion(`pages/${id}`, "PATCH", {
    properties: { [STATUS_PROPERTY]: { [property.type]: { name: "실패" } } },
  });
  await notion("comments", "POST", {
    parent: { page_id: id },
    rich_text: [{ text: { content: message } }],
  });
  console.log(`${id}: AI 처리중 → 실패`);
}
