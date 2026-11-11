import { useSuspenseQueries } from '@tanstack/react-query';
import type { ReactNode } from 'react';

import { popularTopicsQuery, recentMaterialsQuery, topicFieldsQuery } from '@/apis/explore';
import { chipClass } from '@/components/explore/ExploreHero';
import { MaterialList, TechTalkList } from '@/components/explore/MaterialList';
import { cn } from '@/utils/cn';

const SIDE_LABELS: Record<string, string> = {
  COMMON: '공통',
  BACKEND: '백엔드',
  FRONTEND: '프론트엔드',
  MOBILE: '모바일',
};

/** 검색 전 탐색 화면. 추천 콘텐츠와 키워드 목록 */
export function ExploreHome({ onSearch }: { onSearch: (query: string) => void }) {
  const [
    { data: recentBlogs },
    { data: recentTalks },
    { data: popularTopics },
    { data: topicFields },
  ] = useSuspenseQueries({
    queries: [
      recentMaterialsQuery('BLOG', 4),
      recentMaterialsQuery('TALK', 2),
      popularTopicsQuery(),
      topicFieldsQuery(),
    ],
  });
  // 서버는 주제가 없는 분야도 빈 배열로 준다.
  const fields = topicFields.filter((field) => field.topics.length > 0);
  const topicCount = fields.reduce((sum, field) => sum + field.topics.length, 0);

  return (
    <>
      <section aria-labelledby="explore-recommend" className="mt-11 md:mt-14">
        <h2 id="explore-recommend" className="text-xl font-bold">
          이런 컨텐츠는 어때요?
        </h2>
        <div className="mt-6 grid gap-8 md:grid-cols-3 md:gap-10">
          <div className="min-w-0 md:col-span-2">
            <h3 className="mb-5 text-sm font-bold text-gray-700">최근 올라온 크루 블로그</h3>
            <MaterialList materials={recentBlogs} />
          </div>
          <div className="min-w-0">
            <h3 className="mb-5 text-sm font-bold text-gray-700">최근 올라온 테코톡</h3>
            <TechTalkList talks={recentTalks} columns={1} />
          </div>
        </div>
      </section>

      <div className="mt-11 flex flex-col gap-12 md:mt-14">
        <KeywordSection
          id="explore-popular"
          title="많이 다룬 키워드"
          description="크루들의 블로그·팀 문서·테코톡에서 가장 많이 다룬 주제예요."
        >
          <ol className="mt-4 grid gap-x-10 md:grid-cols-2">
            {popularTopics.map((topic, index) => (
              <li key={topic.topicId} className="border-t border-gray-100">
                <button
                  type="button"
                  className="focus-visible:outline-primary-600 group flex w-full cursor-pointer items-baseline py-3 text-left focus-visible:outline-2"
                  onClick={() => onSearch(topic.name)}
                >
                  <span
                    className={cn(
                      'w-7 shrink-0 font-mono text-sm',
                      index < 3 ? 'text-primary-600' : 'text-gray-400',
                    )}
                  >
                    {index + 1}
                  </span>
                  <span className="min-w-0 flex-1 text-base font-semibold text-gray-900 group-hover:underline">
                    {topic.name}
                  </span>
                  <span aria-hidden="true" className="text-gray-400">
                    ›
                  </span>
                </button>
              </li>
            ))}
          </ol>
        </KeywordSection>

        <KeywordSection
          id="explore-fields"
          title="분야별 키워드"
          description={`전체 ${topicCount}개 주제를 분야별로 모았어요.`}
        >
          <div className="mt-5 grid gap-x-10 gap-y-8 md:grid-cols-2">
            {fields.map((field) => (
              <div key={field.side} className="flex min-w-0 flex-col gap-3">
                <h3 className="pb-2 text-sm font-bold">
                  {SIDE_LABELS[field.side] ?? field.side}{' '}
                  <span className="font-medium text-gray-400">{field.topics.length}</span>
                </h3>
                <ul className="flex flex-wrap gap-1.5">
                  {field.topics.map((topic) => (
                    <li key={topic.topicId}>
                      <button
                        type="button"
                        className={chipClass}
                        onClick={() => onSearch(topic.name)}
                      >
                        {topic.name}
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </KeywordSection>
      </div>
    </>
  );
}

function KeywordSection({
  id,
  title,
  description,
  children,
}: {
  id: string;
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <section aria-labelledby={id}>
      <h2 id={id} className="text-xl leading-snug font-bold">
        {title}
      </h2>
      <p className="mt-1 text-sm text-gray-500">{description}</p>
      {children}
    </section>
  );
}
