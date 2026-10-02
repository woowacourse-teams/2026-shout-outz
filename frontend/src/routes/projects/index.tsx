import { createFileRoute } from '@tanstack/react-router';

import { isProjectSort } from '@/constants/project';
import { ProjectsPage } from '@/pages/ProjectsPage';
import { type ProjectSort } from '@/types/project';

export interface ProjectSearch {
  keyword?: string;
  cohorts?: number[];
  techTagIds?: number[];
  sort?: ProjectSort;
}

const parseIds = (value: unknown): number[] | undefined => {
  const raw = typeof value === 'string' ? value.split(',') : Array.isArray(value) ? value : [];
  const ids = raw.map(Number).filter((id) => Number.isInteger(id));

  return ids.length > 0 ? ids : undefined;
};

export const Route = createFileRoute('/projects/')({
  validateSearch: (search: Record<string, unknown>): ProjectSearch => ({
    keyword: typeof search.keyword === 'string' && search.keyword ? search.keyword : undefined,
    cohorts: parseIds(search.cohorts),
    techTagIds: parseIds(search.techTagIds),
    sort: isProjectSort(search.sort) ? search.sort : undefined,
  }),
  component: RouteComponent,
});

function RouteComponent() {
  return <ProjectsPage />;
}
