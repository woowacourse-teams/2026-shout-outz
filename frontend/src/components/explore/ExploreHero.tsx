import { IconSearch } from '@tabler/icons-react';
import { useState } from 'react';

import { getButtonStyles } from '@/components/Button';
import { Tab, tabItemBaseStyle, tabItemSizeStyles, tabItemStyles } from '@/components/Tab';
import { cn } from '@/utils/cn';

const HERO_LINES = ['프로젝트를 만들 때,', '취업을 준비할 때,', '프로젝트를 만들 때,'];

export type ExploreTab = 'keyword' | 'job' | 'project';

/** 탭마다 검색창 문구와 추천 검색어가 다르다. */
const SEARCH_BY_TAB: Record<
  ExploreTab,
  { label: string; placeholder: string; suggestionLabel: string; suggestions: string[] }
> = {
  keyword: {
    label: '기술 이야기 검색',
    placeholder: '예: LLM, 동시성, 무중단 배포',
    suggestionLabel: '이런 주제를 찾아보세요',
    // 인기 검색어 API를 불러오기 전에 보여 줄 목록. 서버가 지금 주는 정해 둔 목록과 같다.
    suggestions: [
      'LLM',
      '동시성',
      '부하 테스트',
      '무중단 배포',
      '쿼리 최적화',
      '모니터링',
      '웹 성능',
      '인증/인가',
    ],
  },
  job: {
    label: '채용공고 검색',
    placeholder: '회사, 직무, 기술로 검색 (예: 토스, 프론트엔드, Kafka)',
    suggestionLabel: '이런 공고를 찾아보세요',
    suggestions: ['토스뱅크', '토스증권', 'Kotlin', 'Spring', 'TypeScript'],
  },
  project: {
    label: '프로젝트 검색',
    placeholder: '프로젝트 이름, 소개, 기술로 찾아보세요',
    suggestionLabel: '이런 주제를 찾아보세요',
    suggestions: [
      'JPA/Hibernate',
      '동시성 제어/락',
      '인덱스/쿼리 최적화',
      'DB 설계/정규화',
      '트랜잭션/데드락',
      '웹 성능 최적화',
    ],
  },
};

export const chipClass = cn(tabItemBaseStyle, tabItemSizeStyles.sm, tabItemStyles.chip);

interface ExploreHeroProps {
  tab: ExploreTab;
  onTabChange: (tab: ExploreTab) => void;
  query: string;
  onSearch: (query: string) => void;
  /** 추천 검색어 중 눌린 것. 없으면 검색어와 같은 것을 눌린 것으로 본다. */
  activeSuggestion?: string | null;
  /** 추천 검색어를 눌렀을 때. 없으면 그 말로 검색한다. */
  onSuggestionSelect?: (suggestion: string) => void;
  /** 추천 검색어. 없으면 탭마다 정해 둔 목록을 쓴다. */
  suggestions?: string[];
}

/** 탐색 화면 첫 영역. 문구, 찾는 방법 탭, 검색창, 추천 검색어 */
export function ExploreHero({
  tab,
  onTabChange,
  query,
  onSearch,
  activeSuggestion = query,
  onSuggestionSelect = onSearch,
  suggestions,
}: ExploreHeroProps) {
  const search = SEARCH_BY_TAB[tab];
  const suggestionItems = suggestions ?? search.suggestions;

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
        value={tab}
        onChange={(value) => onTabChange(value as ExploreTab)}
        aria-label="찾는 방법"
        className="mt-7 max-w-full overflow-x-auto rounded-xl bg-gray-50 p-1"
      >
        <Tab.Item value="keyword" className="data-[status=active]:font-semibold">
          키워드
        </Tab.Item>
        <Tab.Item value="job" className="data-[status=active]:font-semibold">
          채용공고
        </Tab.Item>
        <Tab.Item value="project" className="data-[status=active]:font-semibold">
          프로젝트
        </Tab.Item>
      </Tab>

      {/* 탭이나 검색어가 바뀌면 입력창을 새 값으로 다시 채운다. */}
      <SearchForm
        key={`${tab}:${query}`}
        query={query}
        label={search.label}
        placeholder={search.placeholder}
        onSearch={onSearch}
      />

      <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
        <span className="mr-1 text-xs font-medium text-gray-500">{search.suggestionLabel}</span>
        <ul aria-label="추천 검색어" className="flex flex-wrap gap-2">
          {suggestionItems.map((suggestion) => (
            <li key={suggestion}>
              <button
                type="button"
                data-status={suggestion === activeSuggestion ? 'active' : 'inactive'}
                aria-pressed={suggestion === activeSuggestion}
                className={chipClass}
                onClick={() => onSuggestionSelect(suggestion)}
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

interface SearchFormProps {
  query: string;
  label: string;
  placeholder: string;
  onSearch: (query: string) => void;
}

function SearchForm({ query, label, placeholder, onSearch }: SearchFormProps) {
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
          aria-label={label}
          value={input}
          onChange={(event) => setInput(event.target.value)}
          placeholder={placeholder}
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
