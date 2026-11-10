import { http, HttpResponse } from 'msw';

import type {
  ExploreHome,
  ExploreSearchResult,
  Material,
  MaterialDetail,
  MaterialKind,
  TopicField,
} from '@/apis/explore';
import { blogFaviconOf } from '@/components/explore/MaterialParts';
import { matchTopics, teamName, topics, youtubeId, type Resource } from '@/pages/about/resources';

// MOCK-API: 탐색 API가 생기기 전까지 쓰는 목 응답이다.
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

const TOPIC_FIELDS: TopicField[] = [
  {
    name: '백엔드',
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
    topics: ['Android', 'iOS'],
  },
];

const KIND_BY_SOURCE: Record<Resource['source'], MaterialKind> = {
  blog: 'BLOG',
  pr: 'PR',
  doc: 'TEAM_DOC',
  talk: 'TECH_TALK',
};

const toMaterial = (resource: Resource): Material => {
  const kind = KIND_BY_SOURCE[resource.source];
  const videoId = kind === 'TECH_TALK' ? youtubeId(resource.url) : undefined;
  const team = teamName(resource.project);

  return {
    id: resource.id,
    kind,
    title: resource.title,
    url: resource.url,
    publishedAt: resource.date ?? null,
    summary: resource.line ?? null,
    project: kind === 'TECH_TALK' || !team ? null : { name: team, logoUrl: null },
    faviconUrl: kind === 'BLOG' ? blogFaviconOf(resource.url) : null,
    speaker: kind === 'TECH_TALK' ? (/\]\s*(.+?)의\s/.exec(resource.title)?.[1] ?? null) : null,
    thumbnailUrl: videoId ? `https://i.ytimg.com/vi/${videoId}/mqdefault.jpg` : null,
  };
};

const byNewest = (a: Material, b: Material) =>
  (b.publishedAt ?? '').localeCompare(a.publishedAt ?? '');

/** 주제 여러 개의 자료를 중복 없이 모은다. */
export const collectMaterials = (names: string[]) => {
  const byId = new Map<string, Material>();
  for (const topic of topics.filter((item) => names.includes(item.topic))) {
    for (const resources of Object.values(topic.groups ?? {})) {
      for (const resource of resources) byId.set(resource.id, toMaterial(resource));
    }
  }
  return [...byId.values()].sort(byNewest);
};

const allMaterials = collectMaterials(topics.map((topic) => topic.topic));

/** 채용공고 추천 자료의 resourceId(long) 목. 전체 자료 목록에서의 순서(1부터)를 쓴다. */
export const mockResourceIdOf = (materialId: string) =>
  allMaterials.findIndex((item) => item.id === materialId) + 1;

export const getExploreHome = (): ExploreHome => ({
  recentBlogs: allMaterials.filter((material) => material.kind === 'BLOG').slice(0, 4),
  recentTechTalks: allMaterials.filter((material) => material.kind === 'TECH_TALK').slice(0, 2),
  popularTopics: POPULAR_TOPICS,
  topicFields: TOPIC_FIELDS,
});

export const searchExplore = (query: string): ExploreSearchResult => {
  const matched = matchTopics(query).map((topic) => topic.topic);
  const materials = collectMaterials(matched);

  return {
    query,
    topics: matched,
    blogs: materials.filter((material) => material.kind === 'BLOG'),
    documents: materials.filter(
      (material) => material.kind === 'PR' || material.kind === 'TEAM_DOC',
    ),
    techTalks: materials.filter((material) => material.kind === 'TECH_TALK'),
  };
};

type DetailFields = Pick<MaterialDetail, 'techTags' | 'learnings' | 'experiences'>;

// 요약(기술 태그·배울 점·문제 해결 경험)은 저장된 자료에 없다. 프로토타입의 예시를 종류별로 돌려준다.
const SAMPLE_DETAILS: Record<Exclude<MaterialKind, 'TECH_TALK'>, DetailFields> = {
  BLOG: {
    techTags: [],
    learnings: [
      'MySQL 엔진과 InnoDB 스토리지 엔진의 락 종류와 역할을 구분해요.',
      '레코드 락, 갭 락, 넥스트 키 락의 인덱스 잠금 범위를 알아봐요.',
      '세션별 쿼리 예시로 공유 락과 배타 락의 호환성과 대기 상황을 살펴봐요.',
    ],
    experiences: [],
  },
  TEAM_DOC: {
    techTags: ['JPA', 'PostgreSQL', '비관적 잠금', 'Testcontainers', 'pg_stat_activity'],
    learnings: [
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
        result:
          '변경 후 동시 정정 10회 모두 원장, 보유 32주와 스냅샷 3,200원이 일치했고 동시성 인수 테스트 20개가 통과했습니다. 미국 거래 연동 브랜치에서도 기존 테스트를 완화하지 않고 모두 통과했으며, 전체 894개 중 893개 통과, 기존 조건부 테스트 1개 건너뜀, 실패 0개로 완료했습니다.',
      },
    ],
  },
  PR: {
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
    learnings: [
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
        result:
          '시작 직후 뒷정리하고 2초 뒤 상태를 확인하는 임시 테스트에서 큐를 비우지 않으면 실패하고 비우면 통과했습니다. 레이싱게임 통합 테스트와 Spotless도 통과했습니다.',
      },
    ],
  },
};

export const getMaterialDetail = (materialId: string): MaterialDetail | undefined => {
  // 채용공고 추천 자료는 숫자 resourceId로 요약을 연다.
  const material = /^[1-9]\d*$/.test(materialId)
    ? allMaterials[Number(materialId) - 1]
    : allMaterials.find((item) => item.id === materialId);
  if (!material) return undefined;
  if (material.kind === 'TECH_TALK')
    return { ...material, techTags: [], learnings: [], experiences: [] };
  return { ...material, ...SAMPLE_DETAILS[material.kind] };
};

export const exploreHandlers = [
  http.get('/api/v1/explore', () =>
    HttpResponse.json({ status: 'success', data: getExploreHome() }),
  ),
  http.get('/api/v1/explore/search', ({ request }) => {
    const query = new URL(request.url).searchParams.get('q')?.trim() ?? '';
    return HttpResponse.json({ status: 'success', data: searchExplore(query) });
  }),
  http.get('/api/v1/explore/materials/:materialId', ({ params }) => {
    const detail = getMaterialDetail(decodeURIComponent(String(params.materialId)));
    if (!detail)
      return HttpResponse.json(
        { status: 'error', code: 'RESOURCE_NOT_FOUND', message: '요청한 리소스를 찾을 수 없음' },
        { status: 404 },
      );
    return HttpResponse.json({ status: 'success', data: detail });
  }),
];
