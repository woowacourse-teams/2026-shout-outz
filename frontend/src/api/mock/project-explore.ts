import { http, HttpResponse } from 'msw';

import type { Material } from '@/apis/explore';
import type {
  MaterialJobRole,
  ProjectCareerCard,
  ProjectResourceTopics,
  ProjectTopicGroup,
  ProjectTopicGroupCode,
  ProjectTopicResources,
  ProjectTopics,
} from '@/apis/project-explore';
import { collectMaterials, isBlog, isTalk, TOPIC_FIELDS, topicIdOf } from '@/api/mock/explore';
import projects from '@/api/mock/projects.json';

// 프로젝트 기술 주제(/projects/topics), 경력 카드(/career-cards), 기술 이야기(/resource-topics, /resources)의 목 응답이다.
// 응답 모양은 서버 명세(openapi3.yaml)를 따른다.
// 자료는 탐색 목과 같은 실제 자료(pages/about/data/topics.json)를 쓰고, 프로젝트마다 다룬 주제를 임의로 정했다.

/** 목 topicId를 주제 이름으로 되돌린다. */
export const topicNameOf = (topicId: string) => decodeURIComponent(topicId);

const GROUP_BY_FIELD: Record<
  string,
  { code: ProjectTopicGroupCode; name: string; shortName: string }
> = {
  공통: { code: 'COMMON', name: '공통', shortName: '공통' },
  백엔드: { code: 'BE', name: '백엔드', shortName: 'BE' },
  프론트엔드: { code: 'FE', name: '프론트엔드', shortName: 'FE' },
  모바일: { code: 'AN', name: '안드로이드', shortName: 'AN' },
};

const groupOf = (topic: string) => {
  const field = TOPIC_FIELDS.find((item) => item.topics.includes(topic));
  return field ? GROUP_BY_FIELD[field.name] : undefined;
};

/** 프로젝트마다 다룬 기술 주제 */
const PROJECT_TOPICS: Record<string, string[]> = {
  dropit: ['React 렌더링/훅', '번들러/빌드 최적화', '웹 성능 최적화', 'CI/CD', '동시성 제어/락'],
  'shout-outz': [
    '인덱스/쿼리 최적화',
    '동시성 제어/락',
    'N+1 문제',
    '무중단 배포',
    '실시간 통신(WebSocket/SSE)',
    'React 렌더링/훅',
    'DB 설계/정규화',
    'JPA/Hibernate',
    '트랜잭션/데드락',
    '인증/인가',
    'CI/CD',
  ],
  'code-review-bot': ['LLM/AI 연동', 'CI/CD', '로깅/로그 수집', 'Android'],
  'jachwi-sunbae': ['동시성 제어/락', 'JPA/Hibernate', '인덱스/쿼리 최적화'],
  devica: ['동시성 제어/락', '인덱스/쿼리 최적화', '웹 성능 최적화'],
  knot: ['동시성 제어/락', 'JPA/Hibernate', 'CI/CD'],
  jarihana: ['동시성 제어/락', '실시간 통신(WebSocket/SSE)', 'React 렌더링/훅'],
  yeogidam: ['동시성 제어/락', 'Android'],
  ssok: ['동시성 제어/락', '번들러/빌드 최적화', '무중단 배포'],
  artin: ['동시성 제어/락', '웹 성능 최적화', 'React 렌더링/훅'],
  'gachi-gacha': ['동시성 제어/락', 'JPA/Hibernate', 'DB 설계/정규화'],
  mohaeng: ['Android'],
  jupjup: ['인증/인가', 'DB 설계/정규화'],
};

const topicsOf = (slug: string) => PROJECT_TOPICS[slug] ?? [];

/** 이 주제를 다룬 프로젝트인지 */
export const projectHasTopic = (slug: string, topic: string) => topicsOf(slug).includes(topic);

export const getProjectTopics = (): ProjectTopics => {
  const groups = new Map<ProjectTopicGroupCode, ProjectTopicGroup & { slugs: Set<string> }>();
  const countByTopic = new Map<string, number>();

  for (const project of projects) {
    for (const topic of topicsOf(project.id)) {
      const group = groupOf(topic);
      if (!group) continue;
      const entry = groups.get(group.code) ?? {
        ...group,
        projectCount: 0,
        topics: [],
        slugs: new Set(),
      };
      entry.slugs.add(project.id);
      groups.set(group.code, entry);
      countByTopic.set(topic, (countByTopic.get(topic) ?? 0) + 1);
    }
  }

  const order: ProjectTopicGroupCode[] = ['COMMON', 'BE', 'FE', 'AN'];
  return {
    totalCount: projects.filter((project) => topicsOf(project.id).length > 0).length,
    groups: order.flatMap((code) => {
      const entry = groups.get(code);
      if (!entry) return [];
      const { slugs, ...group } = entry;
      return {
        ...group,
        projectCount: slugs.size,
        topics: [...countByTopic]
          .filter(([topic]) => groupOf(topic)?.code === code)
          .map(([name, projectCount]) => ({ topicId: topicIdOf(name), name, projectCount }))
          .sort((a, b) => b.projectCount - a.projectCount),
      };
    }),
  };
};

// 프로토타입(시안 D · 경험 목록)에 있는 샤라웃 팀의 경험이다.
let sourceId = 9000;
const sourceCard = (source: string, title: string, url: string): Material => ({
  resourceId: (sourceId += 1),
  source,
  title,
  url,
  summary: null,
  publishedAt: null,
  platform: source === 'blog-post' ? 'velog' : null,
  team: { name: '샤라웃', projectSlug: 'shout-outz', logoMediaId: null, logoUrl: null },
});
const pr = (title: string, number: number) =>
  sourceCard(
    'github-pr',
    title,
    `https://github.com/woowacourse-teams/2026-shout-outz/pull/${number}`,
  );
const doc = (title: string) =>
  sourceCard('github-teamdoc', title, 'https://github.com/woowacourse-teams/2026-shout-outz/wiki');
const blog = (title: string) => sourceCard('blog-post', title, 'https://velog.io/@shout-outz');

const SHOUT_OUTZ_EXPERIENCES: Omit<ProjectCareerCard, 'cardId' | 'sections' | 'techs'>[] = [
  {
    jobRole: 'BE',
    category: '장애 대응',
    title: '저사양 EC2의 JVM 메모리 반복 종료 대응',
    resumeBullets: [
      '1GB EC2에서 비율 기반 힙 설정이 물리 메모리를 초과하는 문제에 대응해 전체 903MB의 메모리 예산을 산정하고 힙 상한을 -Xmx320m으로 변경함',
      'Metaspace 부족으로 1분마다 종료되던 dev 서버의 상한을 128MB에서 256MB로 조정하고 배포용 서비스 파일에 반영해 이미지 업로드 후에도 종료되지 않음을 확인함',
      'DEV 서버에 swap과 서비스 비활성화를 적용하고, disable 상태에서도 요청으로 재실행되는 fwupd를 mask로 변경해 메모리 확보 조치를 보완함',
    ],
    sources: [
      pr('[BE/infra] 힙 메모리 한도 조정으로 백엔드 강제 종료 방지 (#193)', 194),
      pr('[BE/hotfix] Metaspace 부족으로 서버가 1분마다 꺼지는 문제 해결 (#292)', 293),
    ],
  },
  {
    jobRole: 'BE',
    category: '쿼리 최적화',
    title: '피드 조회 최적화와 동시성 정합성 검증',
    resumeBullets: [
      '피드 인기순 조회의 좋아요, 댓글 전체 집계를 반정규화와 인덱스 조회로 대체해 로컬 첫 페이지 쿼리를 피드 3,000개에서 3.86ms → 0.11ms, 피드 15,000개에서 18.34ms → 0.32ms로 줄임',
      '사용자 반응 조회의 전체 스캔을 사용자 기준 인덱스로 개선해 같은 쿼리를 3.5ms → 1.2ms로 줄이고, CONCURRENTLY와 executeInTransaction=false로 운영 중 쓰기를 차단하지 않도록 구성함',
      'DB 원자적 증감과 실제 변경 여부에 따른 집계 갱신을 8개 스레드의 동시 요청 테스트 7개로 검증하고, 인덱스 생성 중단 후 정상, INVALID 인덱스가 남은 재실행과 Flyway 복구 절차를 마련함',
    ],
    sources: [
      pr('[BE/feat] 피드 인기순 조회를 반정규화 집계로 개선 (#401)', 402),
      pr('[BE/feat] 사용자 반응 조회 인덱스 추가 (#433)', 434),
    ],
  },
  {
    jobRole: 'BE',
    category: '장애 대응',
    title: '운영 링크 미리보기 오수집과 캐시 복구',
    resumeBullets: [
      '로컬에서 재현되지 않는 유튜브 미리보기 오수집을 운영 curl 응답으로 분석해 봇 확인 페이지가 성공으로 캐시되는 원인을 규명하고, oEmbed 우선 수집으로 운영에서 정상 제목과 썸네일 반환을 확인함',
      '새 수집 로직만으로 갱신되지 않는 잘못된 캐시를 Flyway 마이그레이션으로 조건부 PENDING 전환해 재수집하도록 함',
      '링크 미리보기 작업을 막던 PostgreSQL의 Instant 타입 추론 오류를 Timestamp.from 변환으로 수정하고, 저장소 통합 테스트로 수정 전 운영과 동일한 오류를 재현함',
    ],
    sources: [
      pr('[BE/fix] 유튜브 링크 미리보기를 oEmbed로 먼저 수집 (#512)', 513),
      pr('[BE/fix] 잘못 수집된 미리보기 캐시 재수집 (#520)', 521),
      blog('운영에서만 깨지는 링크 미리보기 추적기'),
    ],
  },
  {
    jobRole: 'BE',
    category: '설계',
    title: 'AI 추천의 비동기 처리와 장애 대체 설계',
    resumeBullets: [
      '질문 작성 부담 인터뷰와 약 97.3%의 제출 구간 이탈률을 근거로 Outbox, SQS, 기존 Spring 작업자와 외부 LLM API의 비동기 추천 구조를 제안하고, Gemini 실패 시 OpenAI 대체와 지수 백오프, 지터를 구현함',
      '사용된 추천의 반복 노출을 조건부 UPDATE와 사용 기록, Outbox 요청의 단일 트랜잭션 저장으로 해결하고, SQS와 Worker로 미사용 추천이 5개가 되도록 부족한 개수만 대체 생성하도록 구현함',
      '추천 노출 상태와 작업 임대를 분리해 생성에 수 초~수십 초가 걸리거나 실패해도 남은 추천 4개를 유지하고, 세대별 대체 생성 총량을 기본 5로 제한함',
    ],
    sources: [
      pr('[BE/feat] 질문 추천 비동기 생성 (#540)', 541),
      pr('[BE/feat] LLM 장애 시 대체 모델 호출 (#548)', 549),
      pr('[BE/feat] 사용한 추천 반복 노출 방지 (#556)', 557),
      doc('ADR: 질문 추천을 Outbox와 SQS로 비동기 처리'),
    ],
  },
  {
    jobRole: 'BE',
    category: '보안',
    title: '세션 인증과 OAuth, 배포 보안 강화',
    resumeBullets: [
      '월 60달러 AWS 예산과 단일 서버, 웹 브라우저 요구사항을 기준으로 서버 세션을 선택하고 state 검증, PKCE S256, 세션 ID 회전, CSRF 검증으로 인증 흐름을 보호함',
      '잘못된 OAuth 콜백이 정상 로그인을 중단하지 않도록 state 일치 시에만 로그인 시도를 소비하고, 완료 주소 허용 목록 검증과 익명 작성자의 정확한 기수 제거로 리다이렉트와 정보 노출을 제한함',
      "Cross-Site 운영 쿠키를 SameSite=None, Secure=true로 분리하고 nginx, Let's Encrypt로 HTTPS를 구성해 브라우저 차단에 대응하며, 공유 보안 그룹 제약 아래 루프백 바인딩으로 앱 외부 접근을 제한하도록 설정함",
    ],
    sources: [
      pr('[BE/feat] GitHub OAuth 로그인 (#61)', 62),
      pr('[BE/feat] PKCE와 state 검증 추가 (#88)', 89),
      pr('[BE/fix] 잘못된 OAuth 콜백 처리 (#120)', 121),
      pr('[BE/infra] 운영 쿠키 SameSite 설정 (#130)', 131),
      pr('[BE/infra] HTTPS 적용 (#140)', 141),
      doc('인증 방식 결정: 서버 세션'),
    ],
  },
  {
    jobRole: 'BE',
    category: '배포 자동화',
    title: '경로 기반 백엔드 배포 트리거 분리',
    resumeBullets: [
      '프론트엔드 변경에도 백엔드가 재배포되어 약 50초간 요청이 끊기는 문제를 분석하고, GitHub 버전 1 소스 유지 제약에 맞춰 GitHub Actions와 배포 전용 브랜치로 트리거를 분리함',
      'backend/**, appspec.yml, buildspec.yml 변경 시에만 develop과 main의 최신 커밋을 deploy/dev와 deploy/prod에 반영하는 워크플로우를 추가하고 실제 배포는 기존 AWS 파이프라인에 유지하도록 구성함',
    ],
    sources: [
      pr('[BE/infra] 백엔드 변경 시에만 배포 (#310)', 311),
      doc('배포 파이프라인 구성'),
      doc('브랜치 전략'),
    ],
  },
  {
    jobRole: 'FE',
    category: '성능 개선',
    title: '정적 페이지 생성으로 첫 화면 표시 시간 단축',
    resumeBullets: [
      '공개 페이지를 빌드 시점에 HTML로 만들어 첫 화면이 자바스크립트 실행 전에 보이도록 바꿈',
      '사용자별 기능은 브라우저에서만 그리도록 분리해 정적 HTML과 화면이 어긋나지 않게 함',
    ],
    sources: [pr('[FE/feat] 공개 페이지 정적 생성 (#350)', 351), doc('정적 생성 도입 기록')],
  },
  {
    jobRole: 'FE',
    category: '설계',
    title: '모달을 overlay-kit으로 통일',
    resumeBullets: [
      '화면마다 다르게 열고 닫던 모달을 overlay-kit의 함수 호출로 바꿔 상태 관리 코드를 줄임',
      '데스크톱 모달과 모바일 바텀시트를 같은 내용 컴포넌트로 보여 주도록 정리함',
    ],
    sources: [pr('[FE/refactor] 모달 overlay-kit으로 통일 (#420)', 421)],
  },
];

export const getProjectCareerCards = (slug: string): ProjectCareerCard[] =>
  slug === 'shout-outz'
    ? SHOUT_OUTZ_EXPERIENCES.map((experience, index) => ({
        ...experience,
        cardId: index + 1,
        sections: [],
        techs: [],
      }))
    : [];

/** 자료 탭에서 주제를 묶는 분야 */
const MATERIAL_FIELD_OF: Record<string, string> = {
  'DB 설계/정규화': '데이터',
  '인덱스/쿼리 최적화': '데이터',
  'N+1 문제': '데이터',
  'JPA/Hibernate': '데이터',
  '동시성 제어/락': '백엔드 프레임워크',
  '트랜잭션/데드락': '백엔드 프레임워크',
  '인증/인가': '아키텍처, 설계',
  'CI/CD': '인프라, DevOps',
  '무중단 배포': '인프라, DevOps',
  '실시간 통신(WebSocket/SSE)': '아키텍처, 설계',
  'React 렌더링/훅': '프론트엔드',
};

const jobRolesOf = (topic: string): MaterialJobRole[] => {
  const code = groupOf(topic)?.code;
  if (code === 'COMMON') return ['BE', 'FE'];
  return code === 'BE' || code === 'FE' || code === 'AN' ? [code] : [];
};

const projectNameOf = (slug: string) => projects.find((project) => project.id === slug)?.name;

/** 주제 자료를 이 팀 자료와 다른 팀 자료로 나눈다. 목이라 세 개 중 하나를 이 팀 자료로 본다. */
const splitMaterials = (slug: string, topic: string) => {
  const name = projectNameOf(slug) ?? slug;
  const materials = collectMaterials([topic]);
  const documents = materials.filter((material) => !isTalk(material));
  return {
    own: documents
      .filter((_, index) => index % 3 === 0)
      .map((material) => ({
        ...material,
        team: { name, projectSlug: slug, logoMediaId: null, logoUrl: null },
      })),
    others: documents.filter((_, index) => index % 3 !== 0).slice(0, 6),
    techTalks: materials.filter(isTalk).slice(0, 5),
  };
};

export const getProjectResourceTopics = (
  slug: string,
  jobRole: MaterialJobRole,
): ProjectResourceTopics => {
  const topics = topicsOf(slug)
    .map((topic) => ({ topic, own: splitMaterials(slug, topic).own }))
    .filter(({ own }) => own.length > 0);
  const count = (role: MaterialJobRole) =>
    topics
      .filter(({ topic }) => role === 'ALL' || jobRolesOf(topic).includes(role))
      .reduce((sum, { own }) => sum + own.length, 0);
  const all = topics.flatMap(({ own }) => own);

  const fields = new Map<string, { topicId: string; name: string; count: number }[]>();
  for (const { topic, own } of topics) {
    if (jobRole !== 'ALL' && !jobRolesOf(topic).includes(jobRole)) continue;
    const field = MATERIAL_FIELD_OF[topic] ?? '기타';
    fields.set(field, [
      ...(fields.get(field) ?? []),
      { topicId: topicIdOf(topic), name: topic, count: own.length },
    ]);
  }

  return {
    totalCount: all.length,
    kindCounts: {
      blog: all.filter(isBlog).length,
      pr: all.filter((material) => material.source === 'github-pr').length,
      teamDoc: all.filter(
        (material) => material.source.startsWith('github-') && material.source !== 'github-pr',
      ).length,
    },
    jobRoles: (['ALL', 'BE', 'FE'] as const)
      .map((role) => ({ jobRole: role, count: count(role) }))
      .filter((item) => item.jobRole === 'ALL' || item.count > 0),
    fields: [...fields].map(([name, items]) => ({
      name,
      topics: items.sort((a, b) => b.count - a.count),
    })),
  };
};

export const getProjectTopicResources = (slug: string, topicId: string): ProjectTopicResources => {
  const topic = topicNameOf(topicId);
  const { own, others, techTalks } = splitMaterials(slug, topic);
  return {
    topic: { topicId, name: topic },
    resources: own,
    talks: techTalks,
    otherTeamResources: others,
  };
};

const notFound = () =>
  HttpResponse.json(
    { status: 'error', code: 'PROJECT_NOT_FOUND', message: '프로젝트를 찾을 수 없음' },
    { status: 404 },
  );

const isMaterialJobRole = (value: string | null): value is MaterialJobRole =>
  value === 'ALL' || value === 'BE' || value === 'FE' || value === 'AN';

// 자료 제보 목. 목 세션 사용자(userId 1)가 팀원인 프로젝트만 받는다.
type MockReport = {
  reportId: number;
  kind: 'ADD' | 'REMOVE';
  resourceId: number | null;
  url: string | null;
};
const reportsBySlug = new Map<string, MockReport[]>();
let nextReportId = 1;
const MOCK_VIEWER_ID = 1;

const isMember = (slug: string) =>
  projects
    .find((project) => project.id === slug)
    ?.members.some((m) => m.userId === MOCK_VIEWER_ID) ?? false;

const forbidden = () =>
  HttpResponse.json(
    {
      status: 'error',
      code: 'RESOURCE_REPORT_FORBIDDEN',
      message: '프로젝트 팀원만 제보할 수 있어요.',
    },
    { status: 403 },
  );

const saveReport = (slug: string, report: Omit<MockReport, 'reportId'>) => {
  const reports = reportsBySlug.get(slug) ?? [];
  const saved = { ...report, reportId: nextReportId++ };
  reportsBySlug.set(slug, [saved, ...reports]);
  return saved.reportId;
};

export const projectExploreHandlers = [
  http.get('/api/v1/projects/@:slug/resource-reports/me', ({ params }) => {
    const slug = String(params.slug);
    if (!projectNameOf(slug)) return notFound();
    const data = (reportsBySlug.get(slug) ?? []).map((report) => ({
      ...report,
      status: 'OPEN',
      createdAt: new Date().toISOString(),
    }));
    return HttpResponse.json({ status: 'success', data });
  }),
  http.post('/api/v1/projects/@:slug/resource-reports/additions', async ({ params, request }) => {
    const slug = String(params.slug);
    if (!projectNameOf(slug)) return notFound();
    if (!isMember(slug)) return forbidden();
    const { url } = (await request.json()) as { url: string };
    if (!/^https:\/\/github\.com\/woowacourse-teams\//.test(url)) {
      return HttpResponse.json(
        {
          status: 'error',
          code: 'RESOURCE_REPORT_URL_OUTSIDE_REPOSITORY',
          message: '이 프로젝트 리포지토리 주소만 제보할 수 있어요.',
        },
        { status: 400 },
      );
    }
    const reportId = saveReport(slug, { kind: 'ADD', resourceId: null, url });
    return HttpResponse.json({ status: 'success', data: { reportId } }, { status: 201 });
  }),
  http.post('/api/v1/projects/@:slug/resource-reports/removals', async ({ params, request }) => {
    const slug = String(params.slug);
    if (!projectNameOf(slug)) return notFound();
    if (!isMember(slug)) return forbidden();
    const { resourceIds } = (await request.json()) as { resourceIds: number[] };
    const data = resourceIds.map((resourceId) => ({
      reportId: saveReport(slug, { kind: 'REMOVE', resourceId, url: null }),
      resourceId,
    }));
    return HttpResponse.json({ status: 'success', data }, { status: 201 });
  }),
  http.get('/api/v1/projects/topics', () =>
    HttpResponse.json({ status: 'success', data: getProjectTopics() }),
  ),
  http.get('/api/v1/projects/@:slug/career-cards', ({ params }) => {
    const slug = String(params.slug);
    if (!projectNameOf(slug)) return notFound();
    return HttpResponse.json({ status: 'success', data: getProjectCareerCards(slug) });
  }),
  http.get('/api/v1/projects/@:slug/resource-topics', ({ params, request }) => {
    const slug = String(params.slug);
    if (!projectNameOf(slug)) return notFound();
    const jobRole = new URL(request.url).searchParams.get('jobRole');
    return HttpResponse.json({
      status: 'success',
      data: getProjectResourceTopics(slug, isMaterialJobRole(jobRole) ? jobRole : 'ALL'),
    });
  }),
  http.get('/api/v1/projects/@:slug/resources', ({ params, request }) => {
    const slug = String(params.slug);
    if (!projectNameOf(slug)) return notFound();
    const topicId = new URL(request.url).searchParams.get('topicId') ?? '';
    return HttpResponse.json({ status: 'success', data: getProjectTopicResources(slug, topicId) });
  }),
];
