import { getRouteApi } from '@tanstack/react-router';

import { NewsItem, type NewsType } from '@/components/NewsItem';
import { Select } from '@/components/Select';
import { Tab } from '@/components/Tab';

const FILTERS = [
  { value: 'ALL', label: '전체' },
  { value: 'NOTICE', label: '공지사항' },
  { value: 'EVENT', label: '이벤트' },
] as const;

export type NewsFilter = (typeof FILTERS)[number]['value'];

export const DEFAULT_NEWS_FILTER: NewsFilter = 'ALL';

// TODO 서버가 받는 sort 값이 아직 LATEST 하나다. 허용 값이 확정되면 인기순을 추가
const SORTS = [{ value: 'LATEST', label: '최신순' }] as const;

export type NewsSort = (typeof SORTS)[number]['value'];

export const DEFAULT_NEWS_SORT: NewsSort = 'LATEST';

/**
 * URL은 사용자가 직접 편집할 수 있어서 값을 신뢰할 수 없다. 라우트의 `validateSearch`가
 * 이 가드로 걸러 모르는 값을 `undefined`로 덮는다.
 */
export const isNewsFilter = (value: unknown): value is NewsFilter =>
  FILTERS.some((filter) => filter.value === value);

export const isNewsSort = (value: unknown): value is NewsSort =>
  SORTS.some((sort) => sort.value === value);

// TODO `NewsListItem` 중 목록 화면이 쓰는 필드. API를 붙이면 생성된 응답 타입으로 대체
interface NewsSummary {
  id: number;
  type: NewsType;
  title: string;
  summary: string;
  publishedAt: string;
}

// TODO `httpClient`가 머지되면 `useSuspenseQuery(getNewsQuery(filter))`로 교체
// 그때 필터는 클라이언트 필터링이 아니라 `GET /api/v1/news?type=` 쿼리로 넘어간다.
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

  // 기본값은 생략해 /news?type=ALL 같은 군더더기를 URL에 남기지 않는다.
  const updateSearch = (next: Partial<{ type: NewsFilter; sort: NewsSort }>) => {
    navigate({
      search: (previous) => ({
        ...previous,
        ...('type' in next && { type: next.type === DEFAULT_NEWS_FILTER ? undefined : next.type }),
        ...('sort' in next && { sort: next.sort === DEFAULT_NEWS_SORT ? undefined : next.sort }),
      }),
    });
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
          onChange={(value) => isNewsFilter(value) && updateSearch({ type: value })}
          aria-label="소식 분류"
        >
          {FILTERS.map(({ value, label }) => (
            <Tab.Item key={value} value={value}>
              {label}
            </Tab.Item>
          ))}
        </Tab>

        <div className="w-24 shrink-0">
          <Select
            value={sort ?? DEFAULT_NEWS_SORT}
            onValueChange={(value) => isNewsSort(value) && updateSearch({ sort: value })}
            aria-label="소식 정렬"
            className="h-auto px-3 py-1.5 text-xs"
          >
            {SORTS.map(({ value, label }) => (
              <Select.Item key={value} value={value}>
                {label}
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
