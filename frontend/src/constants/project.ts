import { type ProjectFilter, type ProjectSort } from '@/types/project';

export const PROJECT_SORTS = ['LATEST', 'POPULAR'] as const satisfies readonly ProjectSort[];

export const PROJECT_SORT_LABELS: Record<ProjectSort, string> = {
  LATEST: '최신 등록순',
  POPULAR: '인기순',
};

export const DEFAULT_PROJECT_FILTER: ProjectFilter = {
  keyword: '',
  cohorts: [],
  techTagIds: [],
  sort: 'LATEST',
};

export const isProjectSort = (value: unknown): value is ProjectSort =>
  PROJECT_SORTS.some((sort) => sort === value);

/** 걸린 조건 개수. 정렬은 빼고 센다(항상 값이 있어서 세면 늘 1 이상이 된다). */
export const countProjectFilters = (filter: ProjectFilter) =>
  (filter.keyword.trim() ? 1 : 0) + filter.cohorts.length + filter.techTagIds.length;
