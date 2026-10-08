import type { Feed } from '@/apis/feed';
import type { FeedComment } from '@/apis/feed-comment';

const crew: Feed['author'] = {
  userId: 1,
  handle: 'woojin',
  displayName: '정우진',
  userType: 'WOOWACOURSE_CREW',
  cohort: 8,
  track: 'BACKEND',
  isCurrent: true,
  avatarUrl: null,
};

const cases: { label: string; author: Feed['author']; anonymous?: boolean }[] = [
  { label: '현재 크루', author: { ...crew, userId: 2, handle: 'dohyun', displayName: '김도현' } },
  {
    label: '수료생',
    author: {
      ...crew,
      userId: 3,
      handle: 'jimin',
      displayName: '이지민',
      cohort: 6,
      track: 'FRONTEND',
      isCurrent: false,
    },
  },
  {
    label: '코치',
    author: {
      ...crew,
      userId: 4,
      handle: 'coach_brown',
      displayName: '브라운',
      userType: 'WOOWACOURSE_COACH',
      cohort: null,
      track: null,
      isCurrent: null,
    },
  },
  {
    label: '일반 회원',
    author: {
      ...crew,
      userId: 5,
      handle: 'minjun',
      displayName: '최민준',
      userType: 'GENERAL',
      cohort: null,
      track: null,
      isCurrent: null,
    },
  },
  {
    label: '익명 크루',
    author: { ...crew, userId: 6, handle: 'yuna', displayName: '김유나' },
    anonymous: true,
  },
  {
    label: '익명 수료생',
    author: {
      ...crew,
      userId: 7,
      handle: 'junho',
      displayName: '박준호',
      cohort: 6,
      isCurrent: false,
    },
    anonymous: true,
  },
  {
    label: '익명 코치',
    author: {
      ...crew,
      userId: 8,
      handle: 'coach_java',
      displayName: '자바지기',
      userType: 'WOOWACOURSE_COACH',
      cohort: null,
      track: null,
      isCurrent: null,
    },
    anonymous: true,
  },
  { label: '본인의 익명 글', author: crew, anonymous: true },
  {
    label: '긴 닉네임과 핸들',
    author: {
      ...crew,
      userId: 9,
      handle: 'frontend_developer_woowacourse',
      displayName: '프론트엔드를공부하는우테코크루',
      track: 'FRONTEND',
    },
  },
  {
    label: '기수만 있는 크루',
    author: { ...crew, userId: 10, handle: 'seoyeon', displayName: '박서연', track: null },
  },
  {
    label: '기수 정보 없는 크루',
    author: {
      ...crew,
      userId: 11,
      handle: 'junyoung',
      displayName: '이준영',
      cohort: null,
      track: null,
      isCurrent: null,
    },
  },
  {
    label: '익명 일반 회원',
    author: {
      ...crew,
      userId: 12,
      handle: 'visitor',
      displayName: '방문자',
      userType: 'GENERAL',
      cohort: null,
      track: null,
      isCurrent: null,
    },
    anonymous: true,
  },
];

/** 개발용 목록에서 질문·포스트 각각 모든 작성자 케이스를 비교한다. */
export function createProfileCaseFeeds(template: Feed): Feed[] {
  return (['POST', 'QUESTION'] as const).flatMap((feedType, typeIndex) =>
    cases.map((item, index) => ({
      ...template,
      feedId: 1001 + typeIndex * 100 + index,
      feedType,
      title: `${feedType === 'QUESTION' ? '프로젝트에서 배운 경험을 어떻게 정리하나요?' : '프로젝트를 진행하며 배운 점을 공유합니다'} · ${item.label}`,
      content: '프로젝트를 진행하면서 배운 점과 고민을 나눕니다. 경험을 공유해 주세요.',
      author: item.author,
      isAnonymous: item.anonymous ?? false,
      categories: [],
      linkPreview: undefined,
      createdAt: new Date(Date.UTC(2026, 9, 8, 9, -index)).toISOString(),
      updatedAt: new Date(Date.UTC(2026, 9, 8, 9, -index)).toISOString(),
    })),
  );
}

export function createProfileCaseComments(feedId: number): FeedComment[] {
  const firstId = feedId * 1000;
  const comments = cases.flatMap((item, index) => {
    const author =
      item.anonymous && item.author.userId !== 1
        ? {
            ...item.author,
            userId: null,
            handle: null,
            displayName: null,
            cohort: null,
            track: null,
            avatarUrl: null,
          }
        : item.author;
    const comment: FeedComment = {
      id: firstId + index * 2,
      author,
      content: `${item.label}의 댓글입니다. 프로젝트 경험을 함께 나누고 싶어요.`,
      parentId: null,
      isAnonymous: item.anonymous ?? false,
      createdAt: new Date(Date.UTC(2026, 9, 8, 10, index)).toISOString(),
      updatedAt: new Date(Date.UTC(2026, 9, 8, 10, index)).toISOString(),
      editable: item.author.userId === 1,
      edited: false,
      deleted: false,
      agreeCount: index % 3,
      agreedByMe: false,
    };
    return [
      comment,
      {
        ...comment,
        id: comment.id + 1,
        parentId: comment.id,
        content: `${item.label}의 답글입니다. 같은 표시를 답글에서도 확인할 수 있어요.`,
      },
    ];
  });
  return [
    ...comments.filter((comment) => comment.parentId === null),
    ...comments.filter((comment) => comment.parentId !== null),
  ];
}
