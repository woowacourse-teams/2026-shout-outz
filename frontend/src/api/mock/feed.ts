import { type Feed, type FeedAuthor, type FeedSort, type FeedType } from '@/types/feed';

/** 실제 서버가 준비되기 전까지 MSW 핸들러가 내려줄 피드 데이터. 백엔드가 뜨면 이 파일은 사라진다. */
/** handle로 고정된 userId를 만든다. mock끼리 같은 사람이 같은 번호를 갖게만 하면 된다. */
const userIdOf = (handle: string) =>
  [...handle].reduce((id, char) => (id * 31 + char.charCodeAt(0)) % 100_000, 7);

const crew = (
  handle: string,
  displayName: string,
  track: 'BACKEND' | 'FRONTEND',
  cohort: number,
): FeedAuthor => ({
  userId: userIdOf(handle),
  handle,
  displayName,
  userType: 'WOOWACOURSE_CREW',
  track,
  isCurrent: cohort === 8,
  avatarUrl: null,
});

const LIKE_COUNTS: Record<number, number> = { 1: 42, 2: 35, 3: 19, 4: 87, 5: 3, 6: 24, 7: 15 };

const feed = (feedId: number, author: FeedAuthor, content: string, createdAt: string): Feed => ({
  feedId,
  title: content.slice(0, 30),
  content,
  feedType: [3, 6, 7].includes(feedId) ? 'QUESTION' : 'POST',
  isAnonymous: feedId === 1,
  author,
  categories: [],
  media: [],
  likeCount: LIKE_COUNTS[feedId] ?? 0,
  likedByMe: false,
  bookmarkCount: 0,
  bookmarkedByMe: false,
  commentCount: feedId === 3 ? 0 : feedId + 1,
  createdAt,
  updatedAt: createdAt,
});

// 최신순은 배열 순서(createdAt 내림차순), 인기순은 LIKE_COUNTS 기준으로 결과가 달라지도록 구성한다.
const FEEDS: Feed[] = [
  feed(
    1,
    { ...crew('woojin', '정우진', 'BACKEND', 8), userId: 1 },
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
      userId: userIdOf('minjun'),
      handle: 'minjun',
      displayName: '최민준',
      userType: 'GENERAL',
      track: null,
      isCurrent: null,
      avatarUrl: null,
    },
    '우테코 크루분들의 프로젝트 구경하러 왔습니다. 좋은 자료 감사합니다!',
    '2026-09-10T12:00:00+09:00',
  ),
  feed(
    6,
    crew('yuna', '김유나', 'FRONTEND', 6),
    '첫 이직을 준비할 때 포트폴리오에는 프로젝트의 어떤 과정을 담는 게 좋을까요?',
    '2026-09-09T11:00:00+09:00',
  ),
  feed(
    7,
    crew('junho', '박준호', 'BACKEND', 6),
    '백엔드 면접에서 장애 대응 경험을 설명할 때 어느 정도까지 상세히 이야기하나요?',
    '2026-09-08T10:00:00+09:00',
  ),
];

export function getFeedList(sort: FeedSort, size: number, feedType?: FeedType): Feed[] {
  const filtered = feedType ? FEEDS.filter((feed) => feed.feedType === feedType) : FEEDS;
  const sorted =
    sort === 'POPULAR'
      ? [...filtered].sort((a, b) => (LIKE_COUNTS[b.feedId] ?? 0) - (LIKE_COUNTS[a.feedId] ?? 0))
      : filtered;

  return sorted.slice(0, size);
}
