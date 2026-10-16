// 부하 테스트 읽기 시나리오
//
// 10/7 prod 앱 로그의 GET 요청 비율(admin, 알림 개수 폴링, 미디어 상태 조회 제외)을 따라 요청을 보낸다.
// 테스트 데이터의 id, slug, handle은 setup()에서 목록 API로 모아 쓰므로, 시드 규칙에 의존하지 않는다.
//
// 실행
//   k6 run -e BASE_URL=http://<테스트 서버> -e PHASE=verify loadtest/k6/read-scenario.js
//
// PHASE
//   smoke   초당 5건, 30초 (스크립트 동작 확인용)
//   warmup  초당 50건, 2분 (판정 제외)
//   verify  초당 400건, 10분 (합격선 판정)
//   limit   초당 400건에서 3분마다 +100건. 5xx·실패율 1% 초과 또는 p95 1초 초과면 중단

import http from 'k6/http';
import { check, fail } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PHASE = __ENV.PHASE || 'verify';
const LOGIN_USERS = Number(__ENV.LOGIN_USERS || 100);
const ANONYMOUS_SESSIONS = Number(__ENV.ANONYMOUS_SESSIONS || 1000);
// 로그인 전용 API가 아닌 요청 중 로그인 상태로 보낼 비율. 전체 요청의 약 35%가 로그인 상태가 되도록 잡은 추정값이다.
const PUBLIC_LOGIN_RATIO = Number(__ENV.PUBLIC_LOGIN_RATIO || 0.15);
// 비로그인 요청 중 쿠키 없이 들어오는 새 방문자 비율.
// 피크 1분에 1.7만 명 × 1.66% ≈ 초당 4.7명이 새로 들어온다고 보고, 초당 400건 대비 약 1.2%로 잡았다.
const NEW_VISITOR_RATIO = Number(__ENV.NEW_VISITOR_RATIO || 0.012);
// 피드 상세 요청 중 인기 글(상위 10%)로 보낼 비율. 10/7 상위 10개 글이 상세 조회의 약 55%였다.
const HOT_FEED_RATIO = Number(__ENV.HOT_FEED_RATIO || 0.55);

const SESSION_COOKIE = 'JSESSIONID';
const VISITOR_COOKIE = 'VISITOR_ID';
const MAX_VUS = 300;

const PHASES = {
  smoke: {
    executor: 'constant-arrival-rate',
    rate: 5,
    timeUnit: '1s',
    duration: '30s',
    preAllocatedVUs: 5,
    maxVUs: 20,
  },
  warmup: {
    executor: 'constant-arrival-rate',
    rate: 50,
    timeUnit: '1s',
    duration: '2m',
    preAllocatedVUs: 20,
    maxVUs: MAX_VUS,
  },
  verify: {
    executor: 'constant-arrival-rate',
    rate: 400,
    timeUnit: '1s',
    duration: '10m',
    preAllocatedVUs: 50,
    maxVUs: MAX_VUS,
  },
  limit: {
    executor: 'ramping-arrival-rate',
    startRate: 400,
    timeUnit: '1s',
    preAllocatedVUs: 50,
    maxVUs: MAX_VUS,
    stages: [400, 500, 600, 700, 800, 900, 1000, 1100, 1200].flatMap((rate) => [
      { target: rate, duration: '10s' },
      { target: rate, duration: '2m50s' },
    ]),
  },
};

const THRESHOLDS = {
  smoke: { checks: ['rate==1'] },
  warmup: {},
  verify: {
    // 서버 응답 시간(p95 50ms, p99 150ms)은 CloudWatch에서 따로 본다.
    'http_req_duration{expected_response:true}': ['p(95)<100'],
    http_req_failed: ['rate<0.001'],
    dropped_iterations: ['count<1'],
  },
  limit: {
    http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '30s' }],
    'http_req_duration{expected_response:true}': [
      { threshold: 'p(95)<1000', abortOnFail: true, delayAbortEval: '30s' },
    ],
  },
};

if (!PHASES[PHASE]) {
  throw new Error(`PHASE는 ${Object.keys(PHASES).join(', ')} 중 하나여야 합니다: ${PHASE}`);
}

export const options = {
  scenarios: { [PHASE]: { ...PHASES[PHASE], exec: 'request' } },
  thresholds: THRESHOLDS[PHASE],
  setupTimeout: '5m',
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  // 부하 요청은 상태 코드만 확인하므로 응답 본문을 메모리에 남기지 않는다. (본문이 필요한 setup 요청만 따로 받는다)
  discardResponseBodies: true,
};

// 10/7 GET 요청 23,097건 기준 건수. login이 true면 로그인 상태로만 보낸다.
const ENDPOINTS = [
  { weight: 6616, name: '/api/v1/auth/session', path: () => '/api/v1/auth/session' },
  { weight: 3918, name: '/api/v1/feeds', path: () => '/api/v1/feeds?sort=LATEST&size=20' },
  { weight: 2597, name: '/api/v1/users/me/summary', path: () => '/api/v1/users/me/summary', login: true },
  { weight: 2395, name: '/api/v1/users/me', path: () => '/api/v1/users/me', login: true },
  { weight: 1032, name: '/api/v1/news', path: () => '/api/v1/news?type=ALL&sort=LATEST' },
  { weight: 994, name: '/api/v1/feeds/{id}', path: (d) => `/api/v1/feeds/${pickFeedId(d)}` },
  {
    weight: 976,
    name: '/api/v1/feeds/{id}/comments',
    path: (d) => `/api/v1/feeds/${pickFeedId(d)}/comments?sort=OLDEST&size=20`,
  },
  { weight: 906, name: '/api/v1/home/statistics', path: () => '/api/v1/home/statistics' },
  { weight: 904, name: '/api/v1/home/banners', path: () => '/api/v1/home/banners' },
  { weight: 878, name: '/api/v1/projects', path: () => '/api/v1/projects?sort=LATEST&size=12' },
  { weight: 473, name: '/api/v1/projects/@{slug}', path: (d) => `/api/v1/projects/@${pick(d.slugs)}` },
  {
    weight: 400,
    name: '/api/v1/users/search',
    path: (d) => `/api/v1/users/search?keyword=${encodeURIComponent(pick(d.handles).slice(0, 3))}`,
  },
  {
    weight: 221,
    name: '/api/v1/users/me/verification-request',
    path: () => '/api/v1/users/me/verification-request',
    login: true,
  },
  { weight: 198, name: '/api/v1/users/{handle}/projects', path: (d) => `/api/v1/users/@${pick(d.handles)}/projects` },
  { weight: 188, name: '/api/v1/users/{handle}', path: (d) => `/api/v1/users/@${pick(d.handles)}` },
  { weight: 127, name: '/api/v1/news/{id}', path: (d) => `/api/v1/news/${pick(d.newsIds)}`, needs: 'newsIds' },
  { weight: 109, name: '/api/v1/cohorts', path: () => '/api/v1/cohorts' },
  { weight: 79, name: '/api/v1/tech-tags', path: () => '/api/v1/tech-tags' },
  { weight: 32, name: '/api/v1/categories', path: () => '/api/v1/categories' },
  { weight: 31, name: '/api/v1/notifications', path: () => '/api/v1/notifications?size=30', login: true },
  { weight: 18, name: '/api/v1/users/{handle}/feeds', path: (d) => `/api/v1/users/@${pick(d.handles)}/feeds` },
  { weight: 5, name: '/api/v1/projects/filters', path: () => '/api/v1/projects/filters' },
];

export function setup() {
  const feeds = collectFeeds();
  const hotFeedIds = collectHotFeedIds(Math.max(1, Math.round(feeds.ids.length * 0.1)));
  const slugs = collectProjectSlugs();
  const newsIds = collectNewsIds();
  const loginSessions = createLoginSessions(feeds.handles, LOGIN_USERS);
  const anonymousSessions = Array.from({ length: ANONYMOUS_SESSIONS }, createAnonymousSession);

  if (feeds.ids.length === 0 || slugs.length === 0 || feeds.handles.length === 0) {
    fail('테스트 데이터가 없습니다. 피드·프로젝트·작성자가 있어야 합니다.');
  }
  if (loginSessions.length === 0) {
    fail('로그인 세션을 하나도 만들지 못했습니다. loadtest 프로필과 사용자 상태를 확인하세요.');
  }

  console.log(
    `[setup] 피드 ${feeds.ids.length}, 인기 피드 ${hotFeedIds.length}, 프로젝트 ${slugs.length}, ` +
      `소식 ${newsIds.length}, 작성자 handle ${feeds.handles.length}, ` +
      `로그인 세션 ${loginSessions.length}, 비로그인 세션 ${anonymousSessions.length}`,
  );
  console.log(`[setup] 시작 ${new Date().toISOString()} PHASE=${PHASE}`);

  return {
    feedIds: feeds.ids,
    hotFeedIds,
    slugs,
    newsIds,
    handles: feeds.handles,
    loginSessions,
    anonymousSessions,
  };
}

export function request(data) {
  const endpoint = pickEndpoint(data);
  const loggedIn = endpoint.login || Math.random() < PUBLIC_LOGIN_RATIO;
  const cookies = loggedIn ? pick(data.loginSessions) : anonymousCookies(data);

  const res = http.get(`${BASE_URL}${endpoint.path(data)}`, {
    cookies,
    tags: { name: endpoint.name, login: String(loggedIn) },
  });
  check(res, { 'status 200': (r) => r.status === 200 }, { name: endpoint.name });
}

export function teardown() {
  console.log(`[teardown] 종료 ${new Date().toISOString()} PHASE=${PHASE}`);
}

function pickEndpoint(data) {
  const available = ENDPOINTS.filter((e) => !e.needs || data[e.needs].length > 0);
  const total = available.reduce((sum, e) => sum + e.weight, 0);
  let r = Math.random() * total;
  for (const e of available) {
    r -= e.weight;
    if (r < 0) return e;
  }
  return available[available.length - 1];
}

function pickFeedId(data) {
  return Math.random() < HOT_FEED_RATIO ? pick(data.hotFeedIds) : pick(data.feedIds);
}

function anonymousCookies(data) {
  // 새 방문자는 쿠키 없이 들어와 서버에 새 세션이 생긴다.
  return Math.random() < NEW_VISITOR_RATIO ? {} : pick(data.anonymousSessions);
}

function pick(items) {
  return items[Math.floor(Math.random() * items.length)];
}

function getJson(path) {
  const res = http.get(`${BASE_URL}${path}`, {
    jar: new http.CookieJar(),
    responseType: 'text',
    tags: { name: 'setup' },
  });
  if (res.status !== 200) {
    fail(`[setup] ${path} 응답 ${res.status}`);
  }
  return res.json();
}

// 커서를 따라 끝까지 모은다.
function collectAll(basePath, toItems) {
  const items = [];
  let cursor = null;
  do {
    const body = getJson(cursor ? `${basePath}&cursor=${encodeURIComponent(cursor)}` : basePath);
    items.push(...toItems(body.data));
    cursor = body.meta && body.meta.hasNext ? body.meta.nextCursor : null;
  } while (cursor);
  return items;
}

function collectFeeds() {
  const feeds = collectAll('/api/v1/feeds?sort=LATEST&size=100', (data) => data);
  const handles = new Set();
  for (const feed of feeds) {
    if (!feed.isAnonymous && feed.author && feed.author.handle) {
      handles.add(feed.author.handle.replace(/^@/, ''));
    }
  }
  return { ids: feeds.map((feed) => feed.feedId), handles: [...handles] };
}

function collectHotFeedIds(count) {
  const ids = [];
  let cursor = null;
  do {
    const path = `/api/v1/feeds?sort=POPULAR&size=100${cursor ? `&cursor=${encodeURIComponent(cursor)}` : ''}`;
    const body = getJson(path);
    ids.push(...body.data.map((feed) => feed.feedId));
    cursor = body.meta && body.meta.hasNext ? body.meta.nextCursor : null;
  } while (cursor && ids.length < count);
  return ids.slice(0, count);
}

function collectProjectSlugs() {
  return collectAll('/api/v1/projects?sort=LATEST&size=50', (data) => data.map((project) => project.slug));
}

function collectNewsIds() {
  return collectAll('/api/v1/news?type=ALL&sort=LATEST', (data) => data.map((news) => news.id));
}

function createLoginSessions(handles, count) {
  const sessions = [];
  for (const handle of handles) {
    if (sessions.length >= count) break;
    const res = http.post(`${BASE_URL}/loadtest/login`, JSON.stringify({ handle }), {
      headers: { 'Content-Type': 'application/json' },
      // 이미 로그인된 쿠키로 다시 부르면 그 세션의 사용자가 바뀌므로, 매번 빈 쿠키로 보낸다.
      jar: new http.CookieJar(),
      tags: { name: 'setup' },
    });
    // 정지·탈퇴·관리자(403)는 건너뛴다.
    if (res.status !== 200) continue;
    sessions.push(sessionCookies(res));
  }
  return sessions;
}

function createAnonymousSession() {
  const res = http.get(`${BASE_URL}/api/v1/auth/session`, { jar: new http.CookieJar(), tags: { name: 'setup' } });
  if (res.status !== 200) {
    fail(`[setup] 비로그인 세션 생성 실패 ${res.status}`);
  }
  return sessionCookies(res);
}

function sessionCookies(res) {
  const cookies = {};
  for (const name of [SESSION_COOKIE, VISITOR_COOKIE]) {
    if (res.cookies[name] && res.cookies[name].length > 0) {
      cookies[name] = res.cookies[name][0].value;
    }
  }
  return cookies;
}
