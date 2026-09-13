import { getRouteApi } from '@tanstack/react-router';

import { NewsItem, type NewsType } from '@/components/NewsItem';
import { Select } from '@/components/Select';
import { Tab } from '@/components/Tab';
import {
  DEFAULT_NEWS_FILTER,
  DEFAULT_NEWS_SORT,
  NEWS_FILTERS,
  NEWS_SORTS,
  type NewsFilter,
  type NewsSort,
} from '@/types/news';

const FILTER_LABELS: Record<NewsFilter, string> = {
  ALL: '전체',
  NOTICE: '공지사항',
  EVENT: '이벤트',
};

const SORT_LABELS: Record<NewsSort, string> = {
  LATEST: '최신순',
};

// TODO `NewsListItem` 중 목록 화면이 쓰는 필드. API를 붙이면 생성된 응답 타입으로 대체
interface NewsSummary {
  id: number;
  type: NewsType;
  title: string;
  summary: string;
  publishedAt: string;
}

// TODO `httpClient`가 머지되면 `useSuspenseQuery(getNewsQuery(filter))`로 교체 -> 필터도 쿼리로
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

const route = getRouteApi('/news/');

export function NewsPage() {
  const { type, sort } = route.useSearch();
  const navigate = route.useNavigate();

  const filter = type ?? DEFAULT_NEWS_FILTER;

  const setSearch = (next: { type?: NewsFilter; sort?: NewsSort }) => {
    navigate({ search: (previous) => ({ ...previous, ...next }) });
  };

  // TODO api 연동 시에 쿼리로 변경
  const news = filter === 'ALL' ? NEWS : NEWS.filter((item) => item.type === filter);

  return (
    <main className="flex flex-col gap-5 px-4 pt-5 pb-7 md:gap-7 md:px-16 md:pt-10 md:pb-20">
      <header className="flex flex-col gap-1 md:gap-2">
        <h1 className="text-lg font-bold tracking-tight text-gray-900 md:text-2xl">소식</h1>
        <p className="text-sm text-gray-600">우아한테크코스 공식 공지사항 및 크루 참여 이벤트</p>
      </header>

      <div className="flex items-center justify-between gap-2">
        <Tab
          variant="chip"
          size="sm"
          value={filter}
          onChange={(value) => setSearch({ type: value as NewsFilter })}
          aria-label="소식 분류"
        >
          {NEWS_FILTERS.map((value) => (
            <Tab.Item key={value} value={value}>
              {FILTER_LABELS[value]}
            </Tab.Item>
          ))}
        </Tab>

        <div className="w-24 shrink-0">
          <Select
            value={sort ?? DEFAULT_NEWS_SORT}
            onValueChange={(value) => setSearch({ sort: value as NewsSort })}
            aria-label="소식 정렬"
            className="h-auto px-3 py-1.5 text-xs"
          >
            {NEWS_SORTS.map((value) => (
              <Select.Item key={value} value={value}>
                {SORT_LABELS[value]}
              </Select.Item>
            ))}
          </Select>
        </div>
      </div>

      <ul className="flex flex-col gap-5 md:grid md:grid-cols-2 md:gap-x-10 md:gap-y-7">
        {news.map(({ id, ...item }) => (
          <li
            key={id}
            className="border-b border-gray-100 pb-4 last:border-b-0 last:pb-0 md:pb-6 md:nth-last-[-n+2]:border-b-0"
          >
            <NewsItem {...item} />
          </li>
        ))}
      </ul>
    </main>
  );
}
