import { type ProjectSummary } from '@/types/project';
import { type Feed } from '@/types/feed';
import { type UserProfile } from '@/types/user';

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
  counts: { projects: 2, feeds: 18 },
};

const crewMember = (userId: number, handle: string, displayName: string, track: string) => ({
  userId,
  handle,
  displayName,
  cohort: 6,
  track,
  avatarImageId: null,
  githubAvatarUrl: null,
  githubProfileUrl: null,
});

const PROJECTS: ProjectSummary[] = [
  {
    id: 1,
    slug: 'moamoa',
    title: '모아모아 (MoaMoa)',
    tagline: '사진 한 장으로 영수증 내역을 자동 분리하고 맞춤 정산하는 웹 서비스',
    cohort: 6,
    thumbnailMediaId: null,
    likeCount: 184,
    commentCount: 14,
    techTags: [
      { id: 1, displayName: 'React' },
      { id: 3, displayName: 'Spring Boot' },
    ],
    members: [crewMember(10, 'woojin', '정우진', 'BE')],
  },
  {
    id: 2,
    slug: 'dropit',
    title: '드랍잇 (Dropit)',
    tagline: '팀 회고를 한곳에 모아 공유하는 협업 도구',
    cohort: 6,
    thumbnailMediaId: null,
    likeCount: 32,
    commentCount: 5,
    techTags: [{ id: 2, displayName: 'TypeScript' }],
    members: [crewMember(10, 'woojin', '정우진', 'BE'), crewMember(11, 'dohyun', '김도현', 'FE')],
  },
];

const feedAuthor = {
  handle: 'woojin',
  displayName: '정우진',
  userType: 'WOOWACOURSE_CREW',
  track: 'BACKEND',
  cohort: 6,
  avatarImageId: null,
};

const FEEDS: Feed[] = [
  {
    feedId: 101,
    content: '영수증 OCR 파싱 작업에서 멀티스레드 비동기 큐를 적용해 응답 시간을 단축했습니다.',
    author: feedAuthor,
    categories: [{ categoryId: 1, slug: 'backend', displayName: '백엔드', type: 'GENERAL' }],
    media: [],
    createdAt: '2026-08-27T12:45:00+09:00',
    updatedAt: '2026-08-27T12:45:00+09:00',
  },
  {
    feedId: 102,
    content: 'Redis 분산락과 Redisson 라이브러리의 Watchdog 메커니즘을 정리했습니다.',
    author: feedAuthor,
    categories: [],
    media: [],
    createdAt: '2026-08-24T09:00:00+09:00',
    updatedAt: '2026-08-24T09:00:00+09:00',
  },
];

export function getUserProfile(handle: string): UserProfile | undefined {
  return handle === PROFILE.handle ? PROFILE : undefined;
}

export function getUserProjects(handle: string): ProjectSummary[] {
  return handle === PROFILE.handle ? PROJECTS : [];
}

export function getUserFeeds(handle: string): Feed[] {
  return handle === PROFILE.handle ? FEEDS : [];
}
