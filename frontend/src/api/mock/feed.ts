import { type Feed, type FeedAuthor, type FeedSort } from '@/types/feed';

/** 실제 서버가 준비되기 전까지 MSW 핸들러가 내려줄 피드 데이터. 백엔드가 뜨면 이 파일은 사라진다. */
const crew = (
  handle: string,
  displayName: string,
  track: 'BACKEND' | 'FRONTEND',
  cohort: number,
): FeedAuthor => ({
  handle,
  displayName,
  userType: 'WOOWACOURSE_CREW',
  track,
  cohort,
  avatarImageId: null,
});

const feed = (feedId: number, author: FeedAuthor, content: string, createdAt: string): Feed => ({
  feedId,
  content,
  author,
  categories: [],
  media: [],
  createdAt,
  updatedAt: createdAt,
});

// 최신순은 배열 순서(createdAt 내림차순), 인기순은 LIKE_COUNTS 기준으로 결과가 달라지도록 구성한다.
const FEEDS: Feed[] = [
  feed(
    1,
    crew('hoik', '황호익', 'BACKEND', 6),
    '루프 프로젝트에서 WebSocket 동기화 지연을 Redis Pub/Sub으로 개선하며 겪은 트러블슈팅 과정을 기술 블로그에 공유합니다.',
    '2026-09-15T08:00:00+09:00',
  ),
  feed(
    2,
    crew('dohyun', '김도현', 'FRONTEND', 6),
    'TanStack Query v5의 낙관적 업데이트(Optimistic Update) 적용 후 정산 요청 체감 레이턴시 개선 경험을 공유합니다.',
    '2026-09-15T06:00:00+09:00',
  ),
  feed(
    3,
    crew('jimin', '이지민', 'BACKEND', 6),
    'Server-Sent Events(SSE) 연결 시 Nginx 리버스 프록시 버퍼링 설정(proxy_buffering off) 관련하여 크루분들의 의견을 구합니다.',
    '2026-09-15T04:00:00+09:00',
  ),
  feed(
    4,
    crew('seoyeon', '박서연', 'FRONTEND', 6),
    '웹 접근성 스터디 4주 차 회고: 스크린 리더로 우리 서비스를 직접 사용해 보며 발견한 문제들을 정리했습니다.',
    '2026-09-14T20:00:00+09:00',
  ),
  feed(
    5,
    {
      handle: 'minjun',
      displayName: '최민준',
      userType: 'GENERAL',
      track: null,
      cohort: null,
      avatarImageId: null,
    },
    '우테코 크루분들의 프로젝트 구경하러 왔습니다. 좋은 자료 감사합니다!',
    '2026-09-10T12:00:00+09:00',
  ),
];

const LIKE_COUNTS: Record<number, number> = { 1: 42, 2: 35, 3: 19, 4: 87, 5: 3 };

export function getFeedList(sort: FeedSort, size: number): Feed[] {
  const sorted =
    sort === 'POPULAR'
      ? [...FEEDS].sort((a, b) => (LIKE_COUNTS[b.feedId] ?? 0) - (LIKE_COUNTS[a.feedId] ?? 0))
      : FEEDS;

  return sorted.slice(0, size);
}
