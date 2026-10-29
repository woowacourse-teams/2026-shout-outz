// autofix가 끝난 뒤 카드 상태를 정리한다.
// - 아직 `AI 처리중`인 카드: AI가 마무리하지 못하고 끝난 것이므로 `실패`로 바꾸고 이유를 댓글로 남긴다.
// - `실패`인 카드: AI가 남긴 댓글에 더해, 이번 실행에서 거부된 명령이 있으면 댓글로 남긴다.
// 사용: node fail-stuck-qa-cards.mjs <카드 ID...>
import { existsSync, readFileSync } from "node:fs";

const { NOTION_TOKEN, RUN_URL, EXECUTION_FILE } = process.env;
const STATUS_PROPERTY = "AI 작업 상태";
// Notion 댓글의 텍스트 하나는 2000자까지 들어간다.
const MAX_COMMENT_LENGTH = 2000;
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

const comment = (id, lines) => {
  const content = lines.filter(Boolean).join("\n");
  const text =
    content.length > MAX_COMMENT_LENGTH
      ? `${content.slice(0, MAX_COMMENT_LENGTH - 20)}\n...(이하 생략)`
      : content;
  return notion("comments", "POST", {
    parent: { page_id: id },
    rich_text: [{ text: { content: text } }],
  });
};

// claude-code-action의 실행 기록(JSON 배열)에서 결과 메시지의 거부된 도구 호출을 꺼낸다.
// 파일이 없거나 형식이 다르면 빈 목록으로 두고 상태 정리는 계속한다.
const readPermissionDenials = () => {
  if (!EXECUTION_FILE || !existsSync(EXECUTION_FILE)) return [];
  try {
    const messages = JSON.parse(readFileSync(EXECUTION_FILE, "utf8"));
    const result = messages.findLast?.((message) => message.type === "result");
    return result?.permission_denials ?? [];
  } catch (error) {
    console.log(`실행 기록을 읽지 못함: ${error.message}`);
    return [];
  }
};

const formatDenial = ({ tool_name, tool_input }) => {
  const detail = tool_input?.command ?? tool_input?.file_path ?? JSON.stringify(tool_input);
  return `- ${tool_name}: ${detail}`;
};

const denials = readPermissionDenials().map(formatDenial);
const denialLines = denials.length > 0 && [
  "거부된 명령 (이번 실행 전체 기준이라 다른 카드에서 거부된 명령이 섞여 있을 수 있습니다):",
  ...denials,
];

for (const id of cardIds) {
  const page = await notion(`pages/${id}`, "GET");
  const property = page.properties[STATUS_PROPERTY];
  // 선택(select)과 상태(status) 타입 모두 처리한다.
  const current = property?.[property.type]?.name;

  if (current === "AI 처리중") {
    await notion(`pages/${id}`, "PATCH", {
      properties: { [STATUS_PROPERTY]: { [property.type]: { name: "실패" } } },
    });
    await comment(id, [
      "AI 작업이 이 카드를 마무리하지 못하고 종료되어 실패 처리했습니다. (실행 시간·턴 한도 초과, 오류 등)",
      "만들다 만 브랜치나 이슈, PR이 남아 있을 수 있습니다. 확인한 뒤 다시 시도하려면 상태를 `AI 대기`로 바꿔 주세요.",
      ...(denialLines || []),
      RUN_URL && `실행 로그: ${RUN_URL}`,
    ]);
    console.log(`${id}: AI 처리중 → 실패`);
    continue;
  }

  if (current === "실패" && denialLines) {
    await comment(id, [...denialLines, RUN_URL && `실행 로그: ${RUN_URL}`]);
    console.log(`${id}: 실패 카드에 거부된 명령 ${denials.length}건 기록`);
    continue;
  }

  console.log(`${id}: ${current ?? "상태 없음"} → 그대로 둠`);
}
