import { http, HttpResponse } from 'msw';

import type { Material, MaterialDetail, MaterialSearchResult, TopicField } from '@/apis/explore';
import { matchTopics, teamName, topics, type Resource } from '@/pages/about/resources';

// 기술 자료 API(/api/v1/resources/*)의 목 응답이다. 응답 모양은 서버 명세(openapi3.yaml)를 따른다.
// 자료는 소개 페이지에 저장해 둔 실제 자료(pages/about/data/topics.json)를 쓰고, 검색어 매칭도 소개 페이지 규칙을 그대로 쓴다.
// 자료가 저장된 주제는 6개뿐이라 나머지 주제는 매칭만 되고 자료가 비어 있다.

const POPULAR_TOPICS = [
  'CI/CD',
  '모니터링/관측성',
  '인증/인가',
  '웹 동작 원리(브라우저, HTTP)',
  '아키텍처 설계(레이어/헥사고날/DDD)',
  '로깅/로그 수집',
  '서버 구성(EC2, 네트워크)',
  'DB 설계/정규화',
  'Java 언어/JVM',
  'Android',
];

/** 분야별 주제 이름. 프로젝트 탐색 목도 이 분류를 쓴다. */
export const TOPIC_FIELDS: { name: string; side: string; topics: string[] }[] = [
  {
    name: '백엔드',
    side: 'BACKEND',
    topics: [
      'DB 설계/정규화',
      'Java 언어/JVM',
      'DB 마이그레이션',
      'JPA/Hibernate',
      '인덱스/쿼리 최적화',
      '동시성 제어/락',
      '예외 처리/입력 검증',
      '트랜잭션/데드락',
      'Spring 동작 원리(빈, AOP, 프록시)',
      '커넥션, 스레드 풀 튜닝',
      '캐시(Redis 등)',
      'API 문서화',
      'DB 레플리케이션/샤딩',
      'N+1 문제',
      'QueryDSL/쿼리 빌더',
      '쿼리 작성(조인, JPQL)',
      'NoSQL/검색 엔진',
      '메시지 큐/이벤트',
      '배치/스케줄링',
      '비동기 처리',
      '분산 시스템/장애 격리',
      'Kotlin 서버',
      '리액티브(WebFlux)',
    ],
  },
  {
    name: '프론트엔드',
    side: 'FRONTEND',
    topics: [
      '웹 성능 최적화',
      '번들러/빌드 최적화',
      '프론트 테스트',
      'React 렌더링/훅',
      '컴포넌트 설계',
      'JavaScript 언어',
      '스타일링(CSS-in-JS 등)',
      '서버 상태(React Query 등)',
      'Next.js/SSR',
      'TypeScript',
      '상태 관리',
      '디자인 시스템/컴포넌트 라이브러리',
      '접근성',
      '렌더링 최적화',
      'PWA/웹 푸시',
    ],
  },
  {
    name: '공통',
    side: 'COMMON',
    topics: [
      'CI/CD',
      '모니터링/관측성',
      '인증/인가',
      '웹 동작 원리(브라우저, HTTP)',
      '아키텍처 설계(레이어/헥사고날/DDD)',
      '로깅/로그 수집',
      '서버 구성(EC2, 네트워크)',
      '기획/사용자 테스트',
      '객체지향/디자인 패턴',
      '사용자 피드백/데이터 기반 개선',
      'REST API 설계',
      '브랜치 전략/협업',
      '부하/성능 테스트',
      'CS 기초(OS, 자료구조, 알고리즘)',
      '통합/인수 테스트',
      '보안/HTTPS',
      '파일 업로드/이미지 처리',
      'LLM/AI 연동',
      '테스트 격리/테스트컨테이너',
      '무중단 배포',
      '실시간 통신(WebSocket/SSE)',
      '정적 분석/코드 품질',
      '소셜 로그인/OAuth',
      '알림/푸시',
      '단위 테스트(JUnit 등)',
      '컨테이너(Docker 등)',
      'Nginx/리버스 프록시',
      'Git/버전 관리',
      '페이지네이션/무한 스크롤',
      '패키지 구조/계층 설계',
      '성능 개선',
      '멱등성/중복 요청 방지',
      '빌드 도구(Gradle, npm)',
      '결제 연동',
      '이미지 최적화',
      'CDN/브라우저 캐시',
      '장애 대응/운영',
    ],
  },
  {
    name: '모바일',
    side: 'MOBILE',
    topics: ['Android', 'iOS'],
  },
];

const SERVER_SOURCE: Record<Resource['source'], string> = {
  blog: 'blog-post',
  pr: 'github-pr',
  doc: 'github-teamdoc',
  talk: 'youtube-tekotalk',
};

// 서버 resourceId 대신 소개 페이지 자료에 1부터 번호를 붙인다.
const resourceIds = new Map<string, number>();
for (const topic of topics) {
  for (const resources of Object.values(topic.groups ?? {})) {
    for (const resource of resources) {
      if (!resourceIds.has(resource.id)) resourceIds.set(resource.id, resourceIds.size + 1);
    }
  }
}

/** 주제 이름의 목 topicId */
export const topicIdOf = (name: string) => encodeURIComponent(name);

const toMaterial = (resource: Resource): Material => {
  const source = SERVER_SOURCE[resource.source];
  const team = teamName(resource.project);

  return {
    resourceId: resourceIds.get(resource.id)!,
    source,
    title: resource.title,
    url: resource.url,
    // 서버는 원문 게시 시각을 UTC ISO 문자열로 준다.
    publishedAt: resource.date ? `${resource.date}T00:00:00Z` : null,
    summary: resource.line ?? null,
    platform:
      source === 'blog-post' ? (resource.url.includes('tistory.com') ? 'tistory' : null) : null,
    team:
      team && source !== 'youtube-tekotalk'
        ? { name: team, projectSlug: resource.slug ?? null, logoMediaId: null, logoUrl: null }
        : undefined,
  };
};

const byNewest = (a: Material, b: Material) =>
  (b.publishedAt ?? '').localeCompare(a.publishedAt ?? '');

/** 주제 여러 개의 자료를 중복 없이 모은다. */
export const collectMaterials = (names: string[]) => {
  const byId = new Map<number, Material>();
  for (const topic of topics.filter((item) => names.includes(item.topic))) {
    for (const resources of Object.values(topic.groups ?? {})) {
      for (const resource of resources) {
        const material = toMaterial(resource);
        byId.set(material.resourceId, material);
      }
    }
  }
  return [...byId.values()].sort(byNewest);
};

const allMaterials = collectMaterials(topics.map((topic) => topic.topic));

export const isTalk = (material: Material) => material.source === 'youtube-tekotalk';
export const isBlog = (material: Material) => material.source === 'blog-post';

const searchMaterials = (keyword: string): MaterialSearchResult => {
  const materials = collectMaterials(matchTopics(keyword).map((topic) => topic.topic));
  return {
    blogs: materials.filter(isBlog),
    documents: materials.filter((material) => material.source.startsWith('github-')),
    talks: materials.filter(isTalk),
  };
};

type DetailFields = Pick<MaterialDetail, 'techTags' | 'keyPoints' | 'experiences'>;

// 요약(기술 태그·배울 점·문제 해결 경험)은 저장된 자료에 없다. 프로토타입의 예시를 종류별로 돌려준다.
const SAMPLE_DETAILS: Record<'blog-post' | 'github-pr' | 'github-teamdoc', DetailFields> = {
  'blog-post': {
    techTags: [],
    keyPoints: [
      'MySQL 엔진과 InnoDB 스토리지 엔진의 락 종류와 역할을 구분해요.',
      '레코드 락, 갭 락, 넥스트 키 락의 인덱스 잠금 범위를 알아봐요.',
      '세션별 쿼리 예시로 공유 락과 배타 락의 호환성과 대기 상황을 살펴봐요.',
    ],
    experiences: [],
  },
  'github-teamdoc': {
    techTags: ['JPA', 'PostgreSQL', '비관적 잠금', 'Testcontainers', 'pg_stat_activity'],
    keyPoints: [
      '이력을 읽기 전에 잠가야 JPA 영속성 컨텍스트의 오래된 상태를 피할 수 있어요.',
      'DB의 실제 잠금 대기를 확인해 동시성 오류를 재현하는 방법을 배워요.',
      '외부 API 조회를 트랜잭션 밖으로 분리할 때의 정합성과 실패 정책을 배워요.',
    ],
    experiences: [
      {
        problem:
          '같은 포트폴리오의 서로 다른 거래를 동시에 수정하면 두 요청이 성공해도 원장은 32주, 저장된 보유 수량은 30주로 불일치했습니다.',
        cause:
          '이력을 읽고 변경한 뒤 스냅샷 저장 시점에 잠가서, 다른 요청의 커밋 이전에 읽은 이력 엔티티가 JPA 영속성 컨텍스트에 남았습니다. 보유 정정, 삭제와 V1 등록에도 공통 포트폴리오 잠금 범위가 없었습니다.',
        solution:
          '원장, 보유 조회 전에 포트폴리오 비관적 쓰기 잠금을 획득하고 커밋, 롤백까지 유지했습니다. 회원 잠금이 필요한 경로는 회원 → 포트폴리오 순서를 유지했습니다. 실제 HTTP 요청과 PostgreSQL의 잠금 대기를 이용해 경합을 재현하고 회귀 검증했습니다.',
        techs: [],
        result:
          '변경 후 동시 정정 10회 모두 원장, 보유 32주와 스냅샷 3,200원이 일치했고 동시성 인수 테스트 20개가 통과했습니다. 미국 거래 연동 브랜치에서도 기존 테스트를 완화하지 않고 모두 통과했으며, 전체 894개 중 893개 통과, 기존 조건부 테스트 1개 건너뜀, 실패 0개로 완료했습니다.',
      },
    ],
  },
  'github-pr': {
    techTags: [
      'FutureTask',
      '스레드 인터럽트',
      'Redis 락',
      '동기 이벤트',
      'Outbox',
      '통합 테스트',
      'JdbcTemplate',
      'JPA',
      '이벤트',
      'ThreadPoolTaskScheduler',
      '예약 태스크',
      'Spotless',
    ],
    keyPoints: [
      '태스크의 자기 인터럽트가 동기 이벤트 리스너의 락 획득을 막는 경로를 배워요.',
      '블로킹 작업 유무와 상태 가드를 근거로 인터럽트 없는 취소를 선택하는 법을 배워요.',
      '종료 신호뿐 아니라 실제 저장과 예약 태스크 정리까지 검증하는 방법을 배워요.',
    ],
    experiences: [
      {
        problem:
          '레이싱게임 결과가 저장되지 않아 홈 랭킹의 최단 기록이 비었고, 시즌 정산 Outbox도 함께 유실됐습니다.',
        cause:
          '자동 이동 태스크가 자신의 Future를 cancel(true)로 취소하면서 현재 스레드에 인터럽트를 걸었습니다. 같은 스레드에서 실행된 동기 이벤트 리스너의 Redis 락 획득이 InterruptedException으로 실패했고, 예외는 자동 이동 태스크에서 로그만 남긴 채 처리됐습니다.',
        solution:
          '태스크 취소를 cancel(false)로 변경하고, 자동 이동 태스크 내부에서 취소해도 실행 스레드가 인터럽트되지 않는 도메인 회귀 테스트를 추가했습니다.',
        techs: [],
        result:
          '자기 인터럽트를 유발하던 취소 방식을 제거했습니다. 도메인 회귀 테스트는 수정 전 실행 스레드의 인터럽트를 검출했습니다.',
      },
      {
        problem:
          '기존 완주 통합 테스트는 종료 상태 전송만 확인해 결과 저장이 전부 실패해도 통과했습니다.',
        cause:
          '종료 브로드캐스트가 결과 저장보다 먼저 예약됐습니다. 또한 기존 테스트의 직접 서비스 호출은 저장 리스너가 요구하는 게임 엔티티와 플레이어 스냅샷 생성 경로를 건너뛰었습니다.',
        solution:
          '방을 실제로 저장하고 프로덕션과 같은 게임 시작 준비 이벤트로 테스트를 시작했습니다. 종료 상태 전송뿐 아니라 JdbcTemplate 카운트 쿼리로 결과 저장 건수도 검증했습니다.',
        techs: [],
        result:
          '수정을 되돌리면 통합 테스트가 expected: 4 but was: 0으로 실패해 결과 저장 누락을 검출했습니다.',
      },
      {
        problem:
          '느린 CI에서 시작 상태 검증이 실패하면 테스트 뒷정리 이후에도 예약된 체인이 게임을 시작할 수 있었습니다.',
        cause:
          '자동 이동 Future는 게임 시작 +1초에 대입되지만, 앞선 설명, 준비 예약 태스크의 Future는 보관되지 않았습니다. 그 전에 뒷정리가 실행되면 자동 이동 중지는 아무 작업도 하지 않았습니다.',
        solution:
          '테스트 종료 시 주기 태스크를 먼저 중지한 뒤 ThreadPoolTaskScheduler의 예약 큐를 비웠습니다.',
        techs: [],
        result:
          '시작 직후 뒷정리하고 2초 뒤 상태를 확인하는 임시 테스트에서 큐를 비우지 않으면 실패하고 비우면 통과했습니다. 레이싱게임 통합 테스트와 Spotless도 통과했습니다.',
      },
    ],
  },
};

export const getMaterialDetail = (resourceId: number): MaterialDetail | undefined => {
  const material = allMaterials.find((item) => item.resourceId === resourceId);
  if (!material) return undefined;
  const fields: DetailFields =
    material.source in SAMPLE_DETAILS
      ? SAMPLE_DETAILS[material.source as keyof typeof SAMPLE_DETAILS]
      : { techTags: [], keyPoints: [], experiences: [] };
  return {
    ...material,
    ...fields,
    contentType: null,
    topic: undefined,
    linkPreview: { title: material.title, description: null, imageUrl: null, siteName: null },
  };
};

const notFound = () =>
  HttpResponse.json(
    { status: 'error', code: 'RESOURCE_NOT_FOUND', message: '요청한 리소스를 찾을 수 없음' },
    { status: 404 },
  );

const POPULAR_KEYWORDS = [
  'LLM',
  '동시성',
  '부하 테스트',
  '무중단 배포',
  '쿼리 최적화',
  '모니터링',
  '웹 성능',
  '인증/인가',
];

export const exploreHandlers = [
  http.get('/api/v1/resources/recent', ({ request }) => {
    const searchParams = new URL(request.url).searchParams;
    const size = Number(searchParams.get('size') ?? 4);
    const talk = searchParams.get('kind') === 'TALK';
    const data = allMaterials
      .filter((material) => (talk ? isTalk(material) : isBlog(material)))
      .filter((material) => material.summary && material.publishedAt)
      .slice(0, size);
    return HttpResponse.json({ status: 'success', data });
  }),
  http.get('/api/v1/resources/topics/popular', () =>
    HttpResponse.json({
      status: 'success',
      data: POPULAR_TOPICS.map((name, index) => ({
        topicId: topicIdOf(name),
        name,
        resourceCount: 100 - index * 7,
      })),
    }),
  ),
  http.get('/api/v1/resources/topics', () => {
    const data: TopicField[] = ['COMMON', 'BACKEND', 'FRONTEND', 'MOBILE'].map((side) => ({
      side,
      topics: (TOPIC_FIELDS.find((field) => field.side === side)?.topics ?? [])
        .map((name) => ({ topicId: topicIdOf(name), name }))
        .sort((a, b) => a.name.localeCompare(b.name)),
    }));
    return HttpResponse.json({ status: 'success', data });
  }),
  http.get('/api/v1/resources/popular-keywords', () =>
    HttpResponse.json({
      status: 'success',
      data: POPULAR_KEYWORDS.map((keyword) => ({ keyword })),
    }),
  ),
  http.get('/api/v1/resources/search', ({ request }) => {
    const keyword = new URL(request.url).searchParams.get('keyword')?.trim() ?? '';
    const { blogs, documents, talks } = searchMaterials(keyword);
    return HttpResponse.json({
      status: 'success',
      data: {
        blogs: { items: blogs.slice(0, 20) },
        documents: { items: documents.slice(0, 20) },
        talks: { items: talks.slice(0, 6) },
      },
    });
  }),
  http.get('/api/v1/resources/:resourceId', ({ params }) => {
    const detail = getMaterialDetail(Number(params.resourceId));
    if (!detail) return notFound();
    return HttpResponse.json({ status: 'success', data: detail });
  }),
];
