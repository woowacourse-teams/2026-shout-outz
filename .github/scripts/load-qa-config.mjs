// .github/qa-autofix/config.jsonc를 읽어 워크플로우에서 쓸 값을 GITHUB_OUTPUT에 기록한다.
// 기록하는 값: model, max_turns, job_timeout_minutes, ai_timeout_minutes, allowed_tools(쉼표 구분)
import { appendFileSync, readFileSync } from "node:fs";

const CONFIG_PATH = ".github/qa-autofix/config.jsonc";
const { GITHUB_OUTPUT } = process.env;

// 줄 전체가 `//` 주석인 줄만 지우고 JSON으로 읽는다.
const source = readFileSync(CONFIG_PATH, "utf8")
  .split("\n")
  .filter((line) => !line.trim().startsWith("//"))
  .join("\n");
const config = JSON.parse(source);

const outputs = {
  model: config.model,
  max_turns: config.maxTurns,
  job_timeout_minutes: config.jobTimeoutMinutes,
  ai_timeout_minutes: config.aiTimeoutMinutes,
  allowed_tools: config.allowedTools.join(","),
};

for (const [key, value] of Object.entries(outputs)) {
  if (value === undefined || value === "") {
    console.error(`${CONFIG_PATH}에 ${key} 값이 없습니다.`);
    process.exit(1);
  }
}

const lines = Object.entries(outputs).map(([key, value]) => `${key}=${value}`);
console.log(lines.join("\n"));
// 로컬 실행처럼 GITHUB_OUTPUT이 없으면 출력 파일 기록을 건너뛴다.
if (GITHUB_OUTPUT) {
  appendFileSync(GITHUB_OUTPUT, `${lines.join("\n")}\n`);
}
