import { IconSearch } from '@tabler/icons-react';
import { useState } from 'react';

import { getButtonStyles } from '@/components/Button';
import { Tab, tabItemBaseStyle, tabItemSizeStyles, tabItemStyles } from '@/components/Tab';
import { cn } from '@/utils/cn';

const HERO_LINES = ['프로젝트를 만들 때,', '취업을 준비할 때,', '프로젝트를 만들 때,'];

export const SUGGESTED_QUERIES = [
  'LLM',
  '동시성',
  '부하 테스트',
  '무중단 배포',
  '쿼리 최적화',
  '모니터링',
  '웹 성능',
  '인증/인가',
];

export const chipClass = cn(tabItemBaseStyle, tabItemSizeStyles.sm, tabItemStyles.chip);

interface ExploreHeroProps {
  query: string;
  onSearch: (query: string) => void;
}

/** 탐색 화면 첫 영역. 문구, 찾는 방법 탭, 검색창, 추천 검색어 */
export function ExploreHero({ query, onSearch }: ExploreHeroProps) {
  return (
    <section className="text-center">
      {/* DESIGN-TOKEN: 데스크톱에서 화면 너비에 따라 26px~40px로 바뀌는 제목 크기는 토큰에 없다. 소개 페이지 히어로와 같은 값이다. */}
      <h1 className="text-2xl leading-[1.4] font-extrabold tracking-tight text-gray-900 md:text-[clamp(26px,3.4vw,40px)]">
        <span className="sr-only">
          프로젝트를 만들거나 취업을 준비할 때, 우테코 크루들은 어떻게 했을까요?
        </span>
        <span aria-hidden="true" className="block h-[1.4em] overflow-hidden">
          <span className="animate-about-hero-slot block leading-[1.4] motion-reduce:animate-none">
            {HERO_LINES.map((line, index) => (
              <span key={index} className="block h-[1.4em] whitespace-nowrap">
                <span className="from-primary-800 to-primary-500 inline-block bg-linear-to-r bg-clip-text text-transparent">
                  {line}
                </span>
              </span>
            ))}
          </span>
        </span>
        <span aria-hidden="true" className="block">
          우테코 크루들은 어떻게 했을까요?
        </span>
      </h1>
      <p className="mx-auto mt-5 max-w-2xl text-base leading-relaxed text-gray-600">
        우테코 크루들의 프로젝트 경험과 취업 과정을 모았어요.
      </p>

      <Tab
        variant="weak"
        value="keyword"
        aria-label="찾는 방법"
        className="mt-7 max-w-full overflow-x-auto rounded-xl bg-gray-50 p-1"
      >
        <Tab.Item value="keyword" className="data-[status=active]:font-semibold">
          키워드
        </Tab.Item>
        {/* 채용공고·프로젝트 탭은 아직 화면이 없어 보이기만 하고 누를 수 없게 둔다. */}
        <Tab.Item value="job" disabled className="disabled:cursor-not-allowed disabled:opacity-50">
          채용공고
        </Tab.Item>
        <Tab.Item
          value="project"
          disabled
          className="disabled:cursor-not-allowed disabled:opacity-50"
        >
          프로젝트
        </Tab.Item>
      </Tab>

      {/* 검색어가 바뀌면 입력창을 새 검색어로 다시 채운다. */}
      <SearchForm key={query} query={query} onSearch={onSearch} />

      <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
        <span className="mr-1 text-xs font-medium text-gray-500">이런 주제를 찾아보세요</span>
        <ul aria-label="추천 검색어" className="flex flex-wrap gap-2">
          {SUGGESTED_QUERIES.map((suggestion) => (
            <li key={suggestion}>
              <button
                type="button"
                data-status={suggestion === query ? 'active' : 'inactive'}
                aria-pressed={suggestion === query}
                className={chipClass}
                onClick={() => onSearch(suggestion)}
              >
                {suggestion}
              </button>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

function SearchForm({ query, onSearch }: ExploreHeroProps) {
  const [input, setInput] = useState(query);

  return (
    <form
      role="search"
      className="mx-auto mt-6 flex max-w-2xl min-w-0 items-center gap-2"
      onSubmit={(event) => {
        event.preventDefault();
        if (input.trim()) onSearch(input.trim());
      }}
    >
      <div className="focus-within:border-primary-400 focus-within:ring-primary-100 bg-background flex h-12 min-w-0 flex-1 items-center gap-2 rounded-xl border border-gray-200 px-4 shadow-sm focus-within:ring-4">
        <IconSearch className="size-5 shrink-0 text-gray-500" aria-hidden="true" />
        <input
          type="search"
          name="q"
          aria-label="기술 이야기 검색"
          value={input}
          onChange={(event) => setInput(event.target.value)}
          placeholder="예: LLM, 동시성, 무중단 배포"
          className="min-w-0 flex-1 bg-transparent text-base text-gray-900 outline-none placeholder:text-gray-400"
        />
      </div>
      <button
        type="submit"
        className={getButtonStyles({
          variant: 'primary',
          size: 'lg',
          className: 'shrink-0 px-4.5 md:px-6',
        })}
      >
        검색
      </button>
    </form>
  );
}
