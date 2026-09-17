import { type UserFeedItem, type UserProfile, type UserProjectCard } from '@/types/user';

/** 실제 서버가 준비되기 전까지 MSW 핸들러가 내려줄 프로필 데이터. 백엔드가 뜨면 이 파일은 사라진다. */
const PROFILE: UserProfile = {
  userId: 10,
  handle: 'woojin',
  displayName: '정우진',
  userType: 'WOOWACOURSE_CREW',
  track: 'BACKEND',
  cohort: 6,
  bio: '대규모 트래픽 분산 처리와 데이터 정합성에 집착하는 백엔드 개발자입니다.',
  avatarImageId: null,
  githubProfileUrl: 'https://github.com/woojin-dev',
  blogUrl: 'https://woojin.log',
  counts: { projects: 2, posts: 18 },
};

const PROJECTS: UserProjectCard[] = [
  {
    projectId: 1,
    slug: 'moamoa',
    title: '모아모아 (MoaMoa)',
    teamName: '모아모아팀',
    tagline: '사진 한 장으로 영수증 내역을 자동 분리하고 맞춤 정산하는 웹 서비스',
    serviceStatus: 'OPERATING',
    thumbnailMediaId: null,
    techTags: [
      { techTagId: 1, slug: 'react', displayName: 'React' },
      { techTagId: 3, slug: 'spring-boot', displayName: 'Spring Boot' },
    ],
  },
  {
    projectId: 2,
    slug: 'dropit',
    title: '드랍잇 (Dropit)',
    teamName: '드랍잇팀',
    tagline: '팀 회고를 한곳에 모아 공유하는 협업 도구',
    serviceStatus: 'CLOSED',
    thumbnailMediaId: null,
    techTags: [{ techTagId: 2, slug: 'typescript', displayName: 'TypeScript' }],
  },
];

const FEEDS: UserFeedItem[] = [
  {
    postId: 101,
    content: '영수증 OCR 파싱 작업에서 멀티스레드 비동기 큐를 적용해 응답 시간을 단축했습니다.',
    author: {
      userId: 10,
      handle: 'woojin',
      displayName: '정우진',
      userType: 'WOOWACOURSE_CREW',
      track: 'BACKEND',
      cohort: 6,
      avatarUrl: null,
    },
    categories: [{ categoryId: 1, slug: 'backend', displayName: '백엔드' }],
    media: [],
    reactionCounts: { LIKE: 42 },
    viewerReactionTypes: [],
    commentCount: 8,
    createdAt: '2026-08-27T12:45:00+09:00',
    updatedAt: '2026-08-27T12:45:00+09:00',
  },
  {
    postId: 102,
    content: 'Redis 분산락과 Redisson 라이브러리의 Watchdog 메커니즘을 정리했습니다.',
    author: {
      userId: 10,
      handle: 'woojin',
      displayName: '정우진',
      userType: 'WOOWACOURSE_CREW',
      track: 'BACKEND',
      cohort: 6,
      avatarUrl: null,
    },
    categories: [],
    media: [],
    reactionCounts: { LIKE: 12 },
    viewerReactionTypes: [],
    commentCount: 5,
    createdAt: '2026-08-24T09:00:00+09:00',
    updatedAt: '2026-08-24T09:00:00+09:00',
  },
];

export function getUserProfile(handle: string): UserProfile | undefined {
  return handle === PROFILE.handle ? PROFILE : undefined;
}

export function getUserProjects(handle: string): UserProjectCard[] {
  return handle === PROFILE.handle ? PROJECTS : [];
}

export function getUserFeeds(handle: string): UserFeedItem[] {
  return handle === PROFILE.handle ? FEEDS : [];
}
