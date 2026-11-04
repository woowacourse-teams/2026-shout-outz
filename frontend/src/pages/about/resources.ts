import topicData from '@/pages/about/data/topics.json';

// 프로토타입(PROTOTYPE_URL)의 소개 화면 로직을 옮긴 것이다. 데이터는 data/ 아래에 프로토타입 응답을 그대로 저장했다.
// 실제 API가 생기면 data/ 대신 응답을 넣고, 이 파일의 매칭·병합 규칙은 그대로 쓰면 된다.

export type ResourceSource = 'blog' | 'pr' | 'doc' | 'talk';

export interface Resource {
  id: string;
  source: ResourceSource;
  title: string;
  url: string;
  date?: string | null;
  line?: string | null;
  project?: string | null;
  slug?: string | null;
}

export type ResourceGroups = Partial<Record<ResourceSource, Resource[]>>;

export interface Topic {
  topic: string;
  synonyms: string[];
  /** 소개 화면에 미리 담아 둔 주제만 자료가 있다. 나머지는 이름과 동의어로 매칭만 한다. */
  groups?: ResourceGroups;
}

export const topics = topicData as Topic[];

export type StepSource = 'blog' | 'pr' | 'talk';

export const STEPS: { src: StepSource; also?: ResourceSource[]; title: string; sub: string }[] = [
  { src: 'blog', title: '블로그 읽기', sub: '프로젝트 경험과 시행착오 살펴보기' },
  // 팀 문서(ADR·트러블슈팅·성능 테스트)는 PR 칸에 합쳐 PR 다음에 보여 준다.
  {
    src: 'pr',
    also: ['doc'],
    title: 'PR 작업 확인하기',
    sub: 'PR과 팀 문서로 구현과 결정을 살펴보기',
  },
  { src: 'talk', title: '테코톡으로 개념 학습하기', sub: '기초 다지기' },
];

const squash = (text: string) => text.toLowerCase().replace(/[\s()/·.,_-]+/g, '');

/** 검색어 → 기술 주제. 주제 이름·동의어와 가장 잘 맞는 주제를 최대 3개 고른다(예: 블루그린 → 무중단 배포). */
export function matchTopics(query: string): Topic[] {
  const key = squash(query);
  if (key.length < 2) return [];
  const scored = topics
    .map((topic) => {
      const names = [topic.topic, ...topic.topic.split(/[/()]/), ...topic.synonyms]
        .map(squash)
        .filter((name) => name.length >= 2);
      const score = Math.max(
        0,
        ...names.map((name) =>
          name === key
            ? 3
            : name.length >= 3 && key.includes(name)
              ? 2
              : name.includes(key)
                ? 1
                : 0,
        ),
      );
      return { topic, score };
    })
    .filter((item) => item.score > 0);
  const best = Math.max(0, ...scored.map((item) => item.score));
  return scored
    .filter((item) => item.score === best)
    .slice(0, 3)
    .map((item) => item.topic);
}

export interface MergedResource extends Resource {
  step: StepSource;
  topics: string[];
  order: number;
}

export type MergedResources = Record<StepSource, MergedResource[]>;

/** 여러 주제의 자료를 단계(블로그 / PR·팀 문서 / 테코톡)별로 모으고 중복을 없앤다. */
export function mergeTopics(list: { label: string; groups: ResourceGroups }[]): MergedResources {
  const byId = new Map<string, MergedResource>();
  let order = 0;
  for (const { label, groups } of list) {
    for (const { src, also = [] } of STEPS) {
      for (const from of [src, ...also]) {
        for (const item of groups[from] ?? []) {
          const current = byId.get(item.id) ?? {
            ...item,
            source: from,
            step: src,
            topics: [],
            order: order++,
          };
          if (!current.topics.includes(label)) current.topics.push(label);
          byId.set(item.id, current);
        }
      }
    }
  }
  const merged: MergedResources = { blog: [], pr: [], talk: [] };
  for (const item of byId.values()) merged[item.step].push(item);
  // 같은 칸 안에서는 원래 소스(PR → 팀 문서)를 먼저, 그다음 여러 키워드에 걸친 자료 순
  const sourceRank = (item: MergedResource) => (item.source === item.step ? 0 : 1);
  for (const items of Object.values(merged)) {
    items.sort(
      (a, b) =>
        sourceRank(a) - sourceRank(b) || b.topics.length - a.topics.length || a.order - b.order,
    );
  }
  return merged;
}

/** 자료가 가장 많이 채워진 칸 수를 먼저, 같으면 자료 수를 비교하는 점수 */
export function fillScore(merged: MergedResources) {
  const counts = STEPS.map(({ src }) => merged[src].length);
  return counts.filter(Boolean).length * 1000 + counts.reduce((sum, count) => sum + count, 0);
}

// 제목 끝에 붙은 블로그 이름·작성자(" - 수리", " — Minty Box")와 남는 기호를 뗀다.
// " - 개발 서버 만들기"처럼 띄어쓴 한글 부제는 제목의 일부라 남긴다.
function stripTitleSuffix(title: string) {
  let out = title;
  for (let i = 0; i < 2; i += 1) {
    const match = /\s+[-–—|]\s+([^-–—|]+)$/.exec(out);
    if (!match) break;
    const tail = match[1]!.trim();
    const hangul = /[가-힣]/.test(tail);
    const nameLike =
      tail.length <= 20 && (!hangul ? !/^\d/.test(tail) : !/\s/.test(tail) && tail.length <= 6);
    if (!nameLike) break;
    out = out.slice(0, match.index);
  }
  return out.replace(/[\s\-–—|:]+$/, '').trim() || title;
}

/** 제목 앞 [RFC]·feat: 같은 접두어와 끝의 #태그, 블로그 이름을 떼고 본문만 남긴다. */
export function cleanTitle(title: string) {
  let text = title.trim();
  for (let match; (match = /^\[([^\]]{1,30})\]\s*/.exec(text));) text = text.slice(match[0].length);
  const prefix =
    /^(feat|fix|refactor|chore|docs|test|style|perf|design|build|ci|hotfix)(\([^)]*\))?\s*[:-]\s*/i.exec(
      text,
    );
  if (prefix) text = text.slice(prefix[0].length);
  text = text
    .replace(/^[\s:|\-–]+/, '')
    .replace(/\s+#[^\s#]+/g, '')
    .trim();
  return stripTitleSuffix(text) || title;
}

/**
 * 2026-09-24 → 2026년 9월 24일
 *
 * 저장한 자료의 날짜는 모두 이 형식이다. 시간대에 따라 달라지는 `Date` 변환은 하지 않아
 * 정적 HTML과 브라우저 렌더링 결과가 같다.
 */
export function formatKoreanDate(value?: string | null) {
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value ?? '');
  return match ? `${match[1]}년 ${Number(match[2])}월 ${Number(match[3])}일` : '';
}

/** 레포 이름 그대로 온 팀 이름의 연도 접두어(2024-, 2021_)를 뗀다. */
export const teamName = (name?: string | null) => (name ?? '').replace(/^20\d\d[-_]/, '');

/**
 * 화면에 보이는 글의 가운뎃점(Kotlin·Spring)을 쉼표(Kotlin, Spring)로 바꾼다. 프로토타입과 같은 표기 규칙이다.
 * 데이터는 원문 그대로 두고 그릴 때만 바꾼다. 이력서 미리보기는 원문 표기를 유지한다.
 */
export const undot = (text: string) => text.replace(/\s*·\s*/g, ', ');

export const youtubeId = (url: string) => /[?&]v=([\w-]{6,})/.exec(url)?.[1];
