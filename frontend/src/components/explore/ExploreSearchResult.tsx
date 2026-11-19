import { useInfiniteQuery, useSuspenseQuery } from '@tanstack/react-query';
import { useRef, type ReactNode } from 'react';

import {
  materialSearchQuery,
  materialSectionQuery,
  type Material,
  type MaterialSearchResult,
  type MaterialSection,
  type MaterialSectionKind,
} from '@/apis/explore';
import { ExploreMoreButton } from '@/components/explore/ExploreMoreButton';
import { MatchedProjects } from '@/components/explore/MatchedProjects';
import { MaterialList, TechTalkList } from '@/components/explore/MaterialList';
import { Tab } from '@/components/Tab';
import { analytics } from '@/utils/analytics';
import { getApiErrorMessage } from '@/utils/error';

/** 검색 결과의 자료 종류. 주소의 kind 값과 같다. */
export type MaterialKind = 'BLOG' | 'DOCUMENT' | 'TALK';
export type SearchResultKind = MaterialKind | 'PROJECT';

const ALL = 'ALL';

// '전체'에서는 종류마다 앞의 몇 개만 보여 준다.
const PREVIEW_COUNT: Record<MaterialKind, number> = { BLOG: 5, DOCUMENT: 5, TALK: 6 };

const KIND_TITLE: Record<MaterialKind, string> = {
  BLOG: '블로그',
  DOCUMENT: 'PR과 팀 문서',
  TALK: '테코톡',
};

// 화면에 놓는 순서
const KIND_ORDER: readonly MaterialKind[] = ['BLOG', 'TALK', 'DOCUMENT'];

interface ExploreSearchResultProps {
  query: string;
  /** 주소에 남긴 자료 종류. null이면 전체 */
  kind: SearchResultKind | null;
  onKindChange: (kind: SearchResultKind | null) => void;
}

/** 검색어로 찾은 블로그·테코톡·PR과 팀 문서 */
export function ExploreSearchResult({ query, kind, onKindChange }: ExploreSearchResultProps) {
  const { data } = useSuspenseQuery(materialSearchQuery(query));
  const summaryRef = useRef<HTMLElement>(null);

  const itemsOf = (target: MaterialKind) =>
    target === 'BLOG'
      ? data.blogs.items
      : target === 'DOCUMENT'
        ? data.documents.items
        : data.talks;
  // 자료가 없는 종류는 칩도 보여 주지 않는다.
  const kinds = KIND_ORDER.filter((target) => itemsOf(target).length > 0);

  if (kinds.length === 0 && data.matchedProjects.length === 0) {
    return (
      <EmptyResult title={`'${query}'에 맞는 자료를 아직 못 찾았어요.`}>
        다른 검색어나 추천 검색어로 찾아보세요.
      </EmptyResult>
    );
  }

  // 종류가 하나뿐이면 '전체'를 두지 않고 그 종류만 보여 준다. 주소의 종류가 비어 있으면 전체로 본다.
  const selected: SearchResultKind | typeof ALL =
    kinds.length === 1 && data.matchedProjects.length === 0
      ? kinds[0]!
      : kind && (kind === 'PROJECT' ? data.matchedProjects.length > 0 : kinds.includes(kind))
        ? kind
        : ALL;

  const changeKind = (next: SearchResultKind | typeof ALL) => {
    analytics.track({
      name: 'explore_result_filter_changed',
      filter: next,
      queryLength: query.length,
    });
    onKindChange(next === ALL ? null : next);
  };

  return (
    <>
      <section ref={summaryRef} aria-label="검색 요약" className="scroll-mt-24">
        <p className="text-sm text-gray-600">
          {/* 서버 검색은 주제가 아니라 검색어로 찾으므로 검색어를 보여 준다. */}
          <strong className="text-primary-600 font-bold">{query}</strong> 검색 결과
        </p>
        {(kinds.length > 1 || (kinds.length > 0 && data.matchedProjects.length > 0)) && (
          <Tab
            variant="chip"
            size="sm"
            value={selected}
            onChange={(value) => changeKind(value as SearchResultKind | typeof ALL)}
            aria-label="자료 종류"
            className="mt-3 flex-wrap"
          >
            <Tab.Item value={ALL}>전체</Tab.Item>
            {data.matchedProjects.length > 0 && <Tab.Item value="PROJECT">프로젝트</Tab.Item>}
            {kinds.map((target) => (
              <Tab.Item key={target} value={target}>
                {KIND_TITLE[target]}
              </Tab.Item>
            ))}
          </Tab>
        )}
      </section>

      <div className="mt-5">
        {(selected === ALL || selected === 'PROJECT') && data.matchedProjects.length > 0 && (
          <MatchedProjects
            projects={selected === ALL ? data.matchedProjects.slice(0, 3) : data.matchedProjects}
            onMore={
              selected === ALL && data.matchedProjects.length > 3
                ? () => {
                    changeKind('PROJECT');
                    summaryRef.current?.scrollIntoView?.({ block: 'start' });
                  }
                : undefined
            }
          />
        )}
        {selected === ALL ? (
          kinds.map((target, index) => {
            const items = itemsOf(target);
            const preview = items.slice(0, PREVIEW_COUNT[target]);
            const section = sectionOf(data, target);
            const hasMore = items.length > preview.length || Boolean(section?.nextCursor);
            return (
              <KindSection key={target} kind={target} className={index > 0 ? 'mt-12' : undefined}>
                <KindItems kind={target} items={preview} />
                {hasMore && (
                  <ExploreMoreButton
                    icon="right"
                    onClick={() => {
                      changeKind(target);
                      // 앞 종류가 사라져 자리가 바뀌므로 결과 맨 위에서 이어 보게 한다.
                      summaryRef.current?.scrollIntoView?.({ block: 'start' });
                    }}
                  >
                    {KIND_TITLE[target]} 모두 보기
                  </ExploreMoreButton>
                )}
              </KindSection>
            );
          })
        ) : selected === 'PROJECT' ? null : selected === 'TALK' ? (
          <KindSection kind="TALK">
            <KindItems kind="TALK" items={data.talks} />
          </KindSection>
        ) : (
          <KindSection kind={selected}>
            <SectionItems query={query} kind={selected} firstPage={sectionOf(data, selected)!} />
          </KindSection>
        )}
      </div>
    </>
  );
}

const sectionOf = (data: MaterialSearchResult, kind: MaterialKind): MaterialSection | null =>
  kind === 'BLOG' ? data.blogs : kind === 'DOCUMENT' ? data.documents : null;

/** 한 종류만 볼 때. 검색 응답의 자료 뒤에 이어 읽기 API로 받은 자료를 붙인다. */
function SectionItems({
  query,
  kind,
  firstPage,
}: {
  query: string;
  kind: MaterialSectionKind;
  firstPage: MaterialSection;
}) {
  // 첫 페이지를 검색 응답으로 채워 두므로 기다리지 않고 바로 그린다.
  const result = useInfiniteQuery(materialSectionQuery(query, kind, firstPage));
  const { fetchNextPage, hasNextPage, isFetchingNextPage, isFetchNextPageError } = result;
  const materials = result.data.pages.flatMap((page) => page.items);

  return (
    <>
      <MaterialList materials={materials} />
      {isFetchNextPageError && (
        <p role="alert" className="py-3 text-sm text-red-600">
          {getApiErrorMessage(result.error)}
        </p>
      )}
      {/* 받은 건수가 아니라 서버가 준 다음 커서로만 더 있는지 판단한다. 빈 페이지가 와도 커서가 있으면 버튼을 둔다. */}
      {hasNextPage && (
        <ExploreMoreButton
          icon={isFetchingNextPage || isFetchNextPageError ? undefined : 'down'}
          disabled={isFetchingNextPage}
          onClick={() => void fetchNextPage({ cancelRefetch: false })}
        >
          {isFetchingNextPage
            ? '불러오는 중…'
            : isFetchNextPageError
              ? `${KIND_TITLE[kind]} 다시 불러오기`
              : `${KIND_TITLE[kind]} 더 보기`}
        </ExploreMoreButton>
      )}
    </>
  );
}

function KindSection({
  kind,
  className,
  children,
}: {
  kind: MaterialKind;
  className?: string;
  children: ReactNode;
}) {
  return (
    <section aria-labelledby={`explore-result-${kind}`} className={className}>
      <h2 id={`explore-result-${kind}`} className="mb-5 text-sm font-bold text-gray-700">
        {KIND_TITLE[kind]}
      </h2>
      {children}
    </section>
  );
}

function KindItems({ kind, items }: { kind: MaterialKind; items: Material[] }) {
  return kind === 'TALK' ? (
    <TechTalkList talks={items} columns={3} />
  ) : (
    <MaterialList materials={items} />
  );
}

function EmptyResult({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="rounded-2xl border border-dashed border-gray-200 px-4 py-10 text-center">
      <p className="font-semibold text-gray-900">{title}</p>
      <p className="mt-2 text-sm text-gray-500">{children}</p>
    </div>
  );
}
