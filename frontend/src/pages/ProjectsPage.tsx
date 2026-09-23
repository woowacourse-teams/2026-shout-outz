import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getRouteApi, Link } from '@tanstack/react-router';
import { IconFilter, IconSearch } from '@tabler/icons-react';

import { sessionQuery } from '@/apis/session';
import { Button, getButtonStyles } from '@/components/Button';
import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { ProjectFilterModal } from '@/components/modals/ProjectFilterModal';
import { ProjectList } from '@/components/projects/ProjectList';
import { ProjectListBoundary } from '@/components/projects/ProjectListBoundary';
import { Select } from '@/components/Select';
import { DEFAULT_PROJECT_FILTER, PROJECT_SORTS, PROJECT_SORT_LABELS } from '@/constants/project';
import { useModal } from '@/hooks/useModal';
import { type ProjectFilter, type ProjectSort } from '@/types/project';

const route = getRouteApi('/projects/');

export function ProjectsPage() {
  const search = route.useSearch();
  const navigate = route.useNavigate();

  const filter: ProjectFilter = {
    keyword: search.keyword ?? DEFAULT_PROJECT_FILTER.keyword,
    cohorts: search.cohorts ?? DEFAULT_PROJECT_FILTER.cohorts,
    techTagIds: search.techTagIds ?? DEFAULT_PROJECT_FILTER.techTagIds,
    sort: search.sort ?? DEFAULT_PROJECT_FILTER.sort,
  };

  const apply = (next: ProjectFilter) =>
    void navigate({
      search: {
        keyword: next.keyword.trim() || undefined,
        cohorts: next.cohorts.length ? next.cohorts : undefined,
        techTagIds: next.techTagIds.length ? next.techTagIds : undefined,
        sort: next.sort === DEFAULT_PROJECT_FILTER.sort ? undefined : next.sort,
      },
    });

  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>프로젝트 | shout-outz</title>
      <AppGnb />
      <main
        id="projects-content"
        tabIndex={-1}
        className="mx-auto w-full max-w-6xl flex-1 px-4 pt-6 pb-12 md:pt-10 md:pb-20"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold md:text-2xl">프로젝트</h1>
            <p className="mt-2 text-sm leading-relaxed text-gray-500">
              우아한테크코스 크루들이 제작한 프로젝트 모음
            </p>
          </div>
          <ProjectRegistrationLink />
        </div>

        <ProjectSearchBar filter={filter} onApply={apply} />

        <div className="mt-8">
          <ProjectListBoundary>
            <ProjectList filter={filter} onResetFilter={() => apply(DEFAULT_PROJECT_FILTER)} />
          </ProjectListBoundary>
        </div>
      </main>
      <Footer />
    </div>
  );
}

/** 검색어 입력, 상세 필터 열기, 정렬. 세 가지 모두 결과를 바꾸므로 한 줄에 둔다. */
function ProjectSearchBar({
  filter,
  onApply,
}: {
  filter: ProjectFilter;
  onApply: (next: ProjectFilter) => void;
}) {
  const { open } = useModal();
  const [keyword, setKeyword] = useState(filter.keyword);
  const filterCount = filter.cohorts.length + filter.techTagIds.length;

  const openFilterModal = async () => {
    const next = await open<ProjectFilter>((close) => (
      <ProjectFilterModal initial={filter} onApply={close} onClose={() => close(filter)} />
    ));
    if (next) onApply(next);
  };

  return (
    <div className="mt-6 flex flex-col gap-3 md:flex-row md:items-center">
      <form
        className="flex min-w-0 flex-1 items-center gap-2"
        onSubmit={(event) => {
          event.preventDefault();
          onApply({ ...filter, keyword });
        }}
      >
        <div className="focus-within:outline-primary-600 flex h-11 min-w-0 flex-1 items-center gap-2 rounded-lg bg-gray-100 px-4 focus-within:outline-2">
          <IconSearch className="size-4 shrink-0 text-gray-500" aria-hidden="true" />
          <input
            type="search"
            aria-label="프로젝트 검색"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="서비스 이름, 소개, 제작자, 기술 스택으로 검색"
            className="min-w-0 flex-1 bg-transparent text-sm text-gray-900 outline-none placeholder:text-gray-500"
          />
        </div>
        <Button type="submit" className="shrink-0">
          검색
        </Button>
      </form>

      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          className="shrink-0 gap-1.5"
          onClick={() => void openFilterModal()}
        >
          <IconFilter className="size-4" aria-hidden="true" />
          상세 필터
          {filterCount > 0 && (
            <span className="bg-primary-600 rounded-full px-1.5 text-xs font-bold text-white">
              {filterCount}
            </span>
          )}
        </Button>

        <div className="w-32 shrink-0">
          <Select
            aria-label="프로젝트 정렬"
            value={filter.sort}
            onValueChange={(value) => onApply({ ...filter, sort: value as ProjectSort })}
          >
            {PROJECT_SORTS.map((sort) => (
              <Select.Item key={sort} value={sort}>
                {PROJECT_SORT_LABELS[sort]}
              </Select.Item>
            ))}
          </Select>
        </div>
      </div>
    </div>
  );
}

function ProjectRegistrationLink() {
  const session = useQuery({ ...sessionQuery, enabled: typeof window !== 'undefined' });
  if (session.data?.status !== 'AUTHENTICATED') return null;

  return (
    <Link to="/projects/new" className={getButtonStyles({ size: 'sm' })}>
      프로젝트 등록
    </Link>
  );
}
