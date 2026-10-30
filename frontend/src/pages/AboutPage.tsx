import { useState, type ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import { IconChevronRight, IconSearch } from '@tabler/icons-react';

import { getButtonStyles } from '@/components/Button';
import { tabItemBaseStyle, tabItemSizeStyles, tabItemStyles } from '@/components/Tab';
import { ProjectCardLink } from '@/components/projects/ProjectCardLink';
import { AboutMoreLink, CareerPreview, MORE_LINK_CLASS } from '@/pages/about/CareerPreview';
import featuredProjects from '@/pages/about/data/featured-projects.json';
import jobData from '@/pages/about/data/job.json';
import { prototypeLinks } from '@/pages/about/links';
import { ResourcePath } from '@/pages/about/ResourcePath';
import {
  fillScore,
  matchTopics,
  mergeTopics,
  type ResourceGroups,
  type Topic,
  undot,
} from '@/pages/about/resources';
import { TechNotes } from '@/pages/about/TechNotes';
import { TechStory } from '@/pages/about/TechStory';
import { cn } from '@/utils/cn';

const SUGGESTED_QUERIES = ['동시성', 'N+1', '무중단 배포', '인덱스', 'React 렌더링', 'WebSocket'];
const DEFAULT_QUERY = '동시성';
const HERO_LINES = ['프로젝트를 만들 때,', '취업을 준비할 때,', '프로젝트를 만들 때,'];

interface JobKeyword {
  keyword: string;
  reason: string;
  groups: ResourceGroups;
}

const job = jobData as {
  id: string;
  company: string;
  title: string;
  logo?: string | null;
  keywords: JobKeyword[];
};
const mergeKeyword = (keyword: JobKeyword) =>
  mergeTopics([{ label: keyword.keyword, groups: keyword.groups }]);
// 처음 보여 줄 역량은 세 칸이 가장 많이 채워진 것으로 고른다(같으면 자료가 많은 쪽).
const FIRST_KEYWORD = job.keywords.reduce(
  (best, keyword, index) =>
    fillScore(mergeKeyword(keyword)) > fillScore(mergeKeyword(job.keywords[best]!)) ? index : best,
  0,
);

const chipClass = cn(tabItemBaseStyle, tabItemSizeStyles.sm, tabItemStyles.chip);

export function AboutPage() {
  const [input, setInput] = useState(DEFAULT_QUERY);
  const [query, setQuery] = useState(DEFAULT_QUERY);
  const [keywordIndex, setKeywordIndex] = useState(FIRST_KEYWORD);

  const search = (value: string) => {
    if (value.trim()) setQuery(value.trim());
  };

  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 pt-6 pb-12 break-keep text-gray-800 md:pt-10 md:pb-20">
      <title>서비스 소개 | shout-outz</title>
      <meta name="description" content="우테코 크루들의 프로젝트 경험과 취업 과정을 모았어요." />
      <link rel="canonical" href="https://shout-ou.tz/about" />

      <section className="border-b border-gray-200 pt-8 pb-12 md:pt-12 md:pb-16">
        <div className="mx-auto max-w-4xl text-center">
          <h1 className="text-[clamp(26px,3.4vw,40px)] leading-[1.4] font-extrabold tracking-tight text-gray-900">
            <span className="sr-only">
              프로젝트를 만들거나 취업을 준비할 때, 우테코 크루들은 어떻게 했을까요?
            </span>
            <span aria-hidden="true" className="block h-[1.4em] overflow-hidden text-gray-700">
              <span className="animate-about-hero-slot block leading-[1.4] motion-reduce:animate-none">
                {HERO_LINES.map((line, index) => (
                  <span key={index} className="block h-[1.4em] whitespace-nowrap">
                    {line}
                  </span>
                ))}
              </span>
            </span>
            <span aria-hidden="true" className="block">
              우테코 크루들은 어떻게 했을까요?
            </span>
          </h1>
          <p className="mx-auto mt-5 max-w-2xl text-sm leading-7 text-gray-600 md:text-base">
            우테코 크루들의 프로젝트 경험과 취업 과정을 모았어요.
          </p>
          <form
            role="search"
            className="mx-auto mt-7 flex max-w-2xl min-w-0 items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault();
              search(input);
            }}
          >
            <div className="focus-within:border-primary-400 focus-within:ring-primary-100 flex h-12 min-w-0 flex-1 items-center gap-2 rounded-xl border border-gray-200 bg-white px-4 shadow-sm focus-within:ring-4">
              <IconSearch className="size-5 shrink-0 text-gray-500" aria-hidden="true" />
              <input
                type="search"
                aria-label="기술 이야기 검색"
                value={input}
                onChange={(event) => setInput(event.target.value)}
                placeholder="예: 동시성, N+1, 무중단 배포"
                className="min-w-0 flex-1 bg-transparent text-base text-gray-900 outline-none placeholder:text-gray-400"
              />
            </div>
            <button
              type="submit"
              className={getButtonStyles({
                variant: 'primary',
                size: 'lg',
                className: 'h-12 shrink-0',
              })}
            >
              검색
            </button>
          </form>
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
                    onClick={() => {
                      setInput(suggestion);
                      search(suggestion);
                    }}
                  >
                    {suggestion}
                  </button>
                </li>
              ))}
            </ul>
          </div>
          <Link to="/projects" className={cn(MORE_LINK_CLASS, 'mx-auto mt-5 font-semibold')}>
            프로젝트 둘러보기 <IconChevronRight className="size-4" aria-hidden="true" />
          </Link>
        </div>
      </section>

      <AboutSection
        id="about-search"
        title="우테코 경험 전체 보기"
        copy="시행착오와 문제를 파고든 과정을 확인해요."
      >
        <div aria-live="polite">
          <TopicResults query={query} />
        </div>
        <AboutMoreLink href={prototypeLinks.search(query)}>검색 화면에서 더 찾아보기</AboutMoreLink>
      </AboutSection>

      <AboutSection
        id="about-projects"
        title="우아한테크코스 정규 프로젝트"
        copy="프로젝트 탭에서 크루들의 트러블슈팅과 시행착오를 살펴볼 수 있어요."
      >
        <ul
          aria-label="추천 프로젝트"
          className="grid grid-cols-1 gap-x-5 gap-y-8 sm:grid-cols-2 xl:grid-cols-3"
        >
          {featuredProjects.map((project) => (
            <li key={project.slug} className="min-w-0">
              <ProjectCardLink project={project} surface="about" />
            </li>
          ))}
        </ul>
        <Link to="/projects" className={MORE_LINK_CLASS}>
          프로젝트 전체 둘러보기 <IconChevronRight className="size-4" aria-hidden="true" />
        </Link>
      </AboutSection>

      <AboutSection
        id="about-career"
        title="이력서나 포트폴리오가 필요하다면?"
        copy="크루들의 프로젝트 경험을 이력서와 포트폴리오 형태로 살펴보세요."
      >
        <CareerPreview />
      </AboutSection>

      <AboutSection
        id="about-jobs"
        title="채용 공고에 맞는 프로젝트 사례 찾기"
        copy="채용 공고가 요구하는 역량을 우테코 크루들의 프로젝트와 문제 해결 사례에서 살펴보세요."
      >
        <JobKeywords selected={keywordIndex} onSelect={setKeywordIndex} />
        <AboutMoreLink href={prototypeLinks.job(job.id)}>이 공고 자세히 보기</AboutMoreLink>
      </AboutSection>

      <TechStory />

      <section className="py-12 text-center md:py-16">
        <h2 className="text-xl font-bold tracking-tight text-gray-900 md:text-2xl">
          지금 궁금한 주제부터 찾아보세요
        </h2>
        <p className="mt-3 text-sm leading-7 text-gray-600 md:text-base">
          77개 우테코 팀의 프로젝트와 취업 경험을 둘러보세요.
        </p>
        <div className="mt-7 flex flex-col justify-center gap-3 sm:flex-row">
          <Link to="/projects" className={getButtonStyles({ variant: 'primary', size: 'lg' })}>
            프로젝트 둘러보기
          </Link>
          <a
            href={prototypeLinks.jobs()}
            className={getButtonStyles({ variant: 'outline', size: 'lg' })}
          >
            공고 키워드로 찾기
          </a>
        </div>
      </section>

      <TechNotes />
    </main>
  );
}

function AboutSection({
  id,
  title,
  copy,
  children,
}: {
  id: string;
  title: string;
  copy: string;
  children: ReactNode;
}) {
  return (
    <section
      id={id}
      aria-labelledby={`${id}-title`}
      className="scroll-mt-20 border-b border-gray-200 py-12 md:py-16"
    >
      <div className="mb-7 max-w-3xl">
        <h2
          id={`${id}-title`}
          className="text-xl font-bold tracking-tight text-gray-900 md:text-2xl"
        >
          {title}
        </h2>
        <p className="mt-3 text-sm leading-7 text-gray-600 md:text-base">{copy}</p>
      </div>
      {children}
    </section>
  );
}

const hasGroups = (topic: Topic): topic is Topic & { groups: ResourceGroups } => !!topic.groups;

/** 검색어와 맞는 기술 주제의 자료를 단계별로 보여 준다. */
function TopicResults({ query }: { query: string }) {
  const matched = matchTopics(query);
  const names = undot(matched.map((topic) => topic.topic).join(', '));
  const stored = matched.filter(hasGroups);

  if (!matched.length) {
    return (
      <EmptyResult title={`'${undot(query)}'에 맞는 기술 주제를 아직 못 찾았어요.`}>
        검색 화면에서는 제목과 한 줄 안내까지 글자로 찾아 드려요.
      </EmptyResult>
    );
  }
  if (!stored.length) {
    // 소개 화면에는 추천 검색어의 주제 자료만 담아 두었다. 나머지 주제는 검색 화면에서 보여 준다.
    return (
      <EmptyResult title={`'${names}' 주제 자료는 검색 화면에서 볼 수 있어요.`}>
        아래 링크로 이 주제의 블로그, PR, 테코톡을 모두 살펴보세요.
      </EmptyResult>
    );
  }
  return (
    <>
      <p className="mb-4 text-sm text-gray-600">
        <strong className="text-primary-600 font-bold">{names}</strong> 주제로 찾았어요.
      </p>
      <ResourcePath
        merged={mergeTopics(stored.map((topic) => ({ label: topic.topic, groups: topic.groups })))}
        perStep={2}
      />
    </>
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

function JobKeywords({
  selected,
  onSelect,
}: {
  selected: number;
  onSelect: (index: number) => void;
}) {
  const keyword = job.keywords[selected];
  return (
    <div className="w-full rounded-2xl border border-gray-200 bg-white p-6 md:p-8">
      <div className="flex items-center gap-4">
        {job.logo && (
          <img src={job.logo} alt="" className="size-12 rounded-lg object-contain" loading="lazy" />
        )}
        <div className="min-w-0">
          <p className="text-sm text-gray-500">{undot(job.company)}</p>
          <h3 className="mt-1 text-lg font-bold text-gray-900 md:text-xl">{undot(job.title)}</h3>
        </div>
      </div>
      <p className="mt-6 text-sm font-semibold text-gray-700">이 공고가 찾는 역량</p>
      <ul className="mt-3 flex flex-wrap gap-2">
        {job.keywords.map((item, index) => (
          <li key={item.keyword}>
            <button
              type="button"
              data-status={index === selected ? 'active' : 'inactive'}
              aria-pressed={index === selected}
              className={chipClass}
              onClick={() => onSelect(index)}
            >
              {undot(item.keyword)}
            </button>
          </li>
        ))}
      </ul>
      {keyword && (
        <div className="mt-6 border-t border-gray-100 pt-6" aria-live="polite">
          <p className="rounded-xl bg-gray-50 px-4 py-3 text-sm leading-6 text-gray-700">
            {undot(keyword.reason)}
          </p>
          <div className="mt-4">
            <ResourcePath merged={mergeKeyword(keyword)} perStep={1} />
          </div>
        </div>
      )}
    </div>
  );
}
