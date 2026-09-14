import type { NewsDetail, NewsNavItem, NewsSummary } from '@/types/news';

/** 실제 서버가 준비되기 전까지 MSW 핸들러가 내려줄 소식 데이터. 백엔드가 뜨면 이 파일은 사라진다. */
const NEWS: NewsSummary[] = [
  {
    id: 1,
    type: 'NOTICE',
    title: '우아한테크코스 6기 최종 프로젝트 데모데이 일정 및 참관 안내',
    summary: '6기 크루들이 준비한 최종 프로젝트 데모데이가 오는 9월 진행됩니다.',
    publishedAt: '2026-08-25T10:00:00+09:00',
  },
  {
    id: 2,
    type: 'EVENT',
    title: '6기 프로젝트 아카이빙 챌린지 - 등록 크루 전원 굿즈팩 증정',
    summary:
      '지금 팀 프로젝트를 등록하면 전체 크루 피드백과 함께 우테코 공식 굿즈팩을 선물로 드립니다.',
    publishedAt: '2026-08-25T10:00:00+09:00',
  },
  {
    id: 3,
    type: 'EVENT',
    title: '주간 베스트 기술 회고 피드 선정 - 커피 쿠폰 증정',
    summary: '매주 좋아요 TOP 3 피드 작성자에게 커피 쿠폰을 드립니다.',
    publishedAt: '2026-08-20T10:00:00+09:00',
  },
  {
    id: 4,
    type: 'EVENT',
    title: '[종료] 상반기 크루 스프린트 회고 피드 작성 리워드 이벤트',
    summary: '상반기 동안 우수하게 소통해 준 크루분들에게 감사의 마음을 전했던 이벤트입니다.',
    publishedAt: '2026-07-15T10:00:00+09:00',
  },
];

const BODIES: Record<number, string> = {
  1: `안녕하세요, 우아한테크코스 크루 여러분!

레벨 3, 4 스프린트를 거쳐 완성한 6기 최종 프로젝트 데모데이를 오는 9월 진행합니다.

📌 진행 안내
• 일시: 2026년 9월 12일 오후 2시
• 장소: 우아한테크코스 캠퍼스 및 온라인 라이브
• 참관: 크루, 코치, 외부 참관객 모두 가능`,
  2: `안녕하세요, 우아한테크코스 크루 여러분!

레벨 3, 4 스프린트를 거치며 크루분들이 열정으로 완성한 멋진 프로젝트들을 shout-outz 아카이브에 등록하고, 동료 크루들과 경험을 나누는 '6기 프로젝트 아카이빙 챌린지'를 시작합니다.

📌 참여 혜택 및 안내
• 참여 대상: 6기 프로젝트 팀 전체
• 참여 방법: [프로젝트 등록] 메뉴에서 등록
• 혜택: 전원 한정판 굿즈팩 증정`,
  3: `매주 가장 많은 공감을 받은 기술 회고 피드를 선정합니다.

📌 선정 안내
• 대상: 매주 좋아요 TOP 3 피드
• 혜택: 작성자에게 커피 쿠폰 증정
• 발표: 매주 월요일 소식 게시`,
  4: `상반기 동안 활발하게 회고를 공유해 주신 크루분들께 감사드립니다.

본 이벤트는 2026년 7월 15일자로 종료되었습니다.`,
};

const CTAS: Record<number, { label: string; url: string }> = {
  2: { label: '지금 프로젝트 등록하러 가기 ›', url: '/projects/new' },
};

const AUTHOR = { userId: 1, name: '우아한테크코스 운영진' };

const toNavItem = (news: NewsSummary | undefined): NewsNavItem | null =>
  news ? { id: news.id, title: news.title, publishedAt: news.publishedAt } : null;

export function getNewsList(): NewsSummary[] {
  return NEWS;
}

/**
 * 이전·다음은 분류 구분 없이 전체 소식의 날짜순 기준이다. `NEWS`가 최신순이라
 * 뒤쪽 원소가 이전 글, 앞쪽 원소가 다음 글이다.
 */
export function getNewsDetail(newsId: number): NewsDetail | undefined {
  const index = NEWS.findIndex((news) => news.id === newsId);
  if (index === -1) return undefined;

  const news = NEWS[index]!;

  return {
    id: news.id,
    type: news.type,
    title: news.title,
    publishedAt: news.publishedAt,
    body: BODIES[newsId] ?? '',
    author: AUTHOR,
    cta: CTAS[newsId] ?? null,
    previous: toNavItem(NEWS[index + 1]),
    next: toNavItem(NEWS[index - 1]),
  };
}
