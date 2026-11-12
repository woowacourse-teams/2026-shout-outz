import { Link } from '@tanstack/react-router';

import { AsyncBoundary } from '@/components/AsyncBoundary';
import type { ExploreProjectFilter } from '@/apis/project-explore';
import { ProjectExploreList } from '@/components/project-explore/ProjectExploreList';
import { ProjectTopicField } from '@/components/project-explore/ProjectTopicSheet';
import { ProjectTopicNav } from '@/components/project-explore/ProjectTopicNav';
import type { ProjectSort } from '@/types/project';

interface ProjectExploreProps {
  filter: ExploreProjectFilter;
  onTopicChange: (topic: string | null) => void;
  onSortChange: (sort: ProjectSort) => void;
}

/** 탐색 화면 프로젝트 탭. 데스크톱은 왼쪽에 기술 주제, 모바일은 목록 위 필드에서 바텀시트로 고른다. */
export function ProjectExplore({ filter, onTopicChange, onSortChange }: ProjectExploreProps) {
  return (
    <section
      aria-label="프로젝트 찾기"
      className="mx-auto mt-10 max-w-5xl text-left lg:mt-14 lg:flex lg:gap-12"
    >
      <div className="hidden w-46 shrink-0 lg:block">
        <AsyncBoundary fallback={null}>
          <ProjectTopicNav topic={filter.topic} onTopicChange={onTopicChange} />
        </AsyncBoundary>
      </div>
      <div className="min-w-0 flex-1">
        <div className="mb-6 lg:hidden">
          <ProjectTopicField topic={filter.topic} onTopicChange={onTopicChange} />
        </div>
        <AsyncBoundary>
          <ProjectExploreList filter={filter} onSortChange={onSortChange} />
        </AsyncBoundary>
        {/* 비로그인이면 등록 화면이 로그인 안내를 보여 준다. */}
        <p className="mt-10 border-t border-gray-100 pt-7 text-center text-sm text-gray-500">
          찾는 프로젝트가 없나요?{' '}
          <Link
            to="/projects/new"
            className="focus-visible:outline-primary-600 rounded-sm font-bold whitespace-nowrap text-gray-900 underline decoration-gray-200 underline-offset-3 hover:decoration-gray-500 focus-visible:outline-2"
          >
            우리 팀 프로젝트 등록하기 →
          </Link>
        </p>
      </div>
    </section>
  );
}
