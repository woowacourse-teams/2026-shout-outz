import { useSuspenseQuery } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';

import { materialSearchQuery, type Material } from '@/apis/explore';
import { MaterialList, TechTalkList } from '@/components/explore/MaterialList';
import { analytics } from '@/utils/analytics';
import { Tab } from '@/components/Tab';

type KindFilter = 'ALL' | 'BLOG' | 'DOCUMENT' | 'TECH_TALK';

// '전체'에서는 종류마다 앞의 몇 개만 보여 준다.
const PREVIEW_COUNT = { BLOG: 8, DOCUMENT: 8, TECH_TALK: 6 } as const;

/** 검색어로 찾은 블로그·테코톡·PR과 팀 문서 */
export function ExploreSearchResult({ query }: { query: string }) {
  const { data } = useSuspenseQuery(materialSearchQuery(query));
  const [filter, setFilter] = useState<KindFilter>('ALL');

  if (data.blogs.length + data.documents.length + data.talks.length === 0) {
    return (
      <EmptyResult title={`'${query}'에 맞는 자료를 아직 못 찾았어요.`}>
        다른 검색어나 추천 검색어로 찾아보세요.
      </EmptyResult>
    );
  }

  const groups: { kind: Exclude<KindFilter, 'ALL'>; title: string; materials: Material[] }[] = [
    { kind: 'BLOG', title: '블로그', materials: data.blogs },
    { kind: 'TECH_TALK', title: '테코톡', materials: data.talks },
    { kind: 'DOCUMENT', title: 'PR과 팀 문서', materials: data.documents },
  ];
  const total = groups.reduce((sum, group) => sum + group.materials.length, 0);
  const visible = groups.filter(
    (group) => group.materials.length > 0 && (filter === 'ALL' || filter === group.kind),
  );

  return (
    <>
      <section aria-label="검색 요약">
        <p className="text-sm text-gray-600">
          {/* 서버 검색은 주제가 아니라 검색어로 찾으므로 검색어를 보여 준다. */}
          <strong className="text-primary-600 font-bold">{query}</strong> 검색 결과
        </p>
        <Tab
          variant="chip"
          size="sm"
          value={filter}
          onChange={(value) => {
            analytics.track({
              name: 'explore_result_filter_changed',
              filter: value,
              queryLength: query.length,
            });
            setFilter(value as KindFilter);
          }}
          aria-label="자료 종류"
          className="mt-3 flex-wrap"
        >
          <Tab.Item value="ALL">
            전체
            <Count>{total}</Count>
          </Tab.Item>
          <Tab.Item value="BLOG">
            블로그
            <Count>{data.blogs.length}</Count>
          </Tab.Item>
          <Tab.Item value="DOCUMENT">
            PR과 팀 문서
            <Count>{data.documents.length}</Count>
          </Tab.Item>
          <Tab.Item value="TECH_TALK">
            테코톡
            <Count>{data.talks.length}</Count>
          </Tab.Item>
        </Tab>
      </section>

      <div className="mt-5">
        {visible.map((group, index) => {
          const materials =
            filter === 'ALL'
              ? group.materials.slice(0, PREVIEW_COUNT[group.kind])
              : group.materials;

          return (
            <section
              key={group.kind}
              aria-labelledby={`explore-result-${group.kind}`}
              className={index > 0 ? 'mt-12' : undefined}
            >
              <div className="mb-5">
                <h2 id={`explore-result-${group.kind}`} className="text-sm font-bold text-gray-700">
                  {group.title}{' '}
                  <span className="font-medium text-gray-400">{group.materials.length}</span>
                </h2>
              </div>
              {group.kind === 'TECH_TALK' ? (
                <TechTalkList talks={materials} columns={3} />
              ) : (
                <MaterialList materials={materials} />
              )}
            </section>
          );
        })}
      </div>
    </>
  );
}

function Count({ children }: { children: ReactNode }) {
  return <span className="ml-1.5 text-xs opacity-60">{children}</span>;
}

function EmptyResult({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="rounded-2xl border border-dashed border-gray-200 px-4 py-10 text-center">
      <p className="font-semibold text-gray-900">{title}</p>
      <p className="mt-2 text-sm text-gray-500">{children}</p>
    </div>
  );
}
