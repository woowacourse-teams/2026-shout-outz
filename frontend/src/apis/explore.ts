import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';

import type {
  PopularKeywordsSuccessResponse,
  ResourceDetailSuccessResponse,
  ResourceRecentSuccessResponse,
  ResourceSearchMoreSuccessResponse,
  ResourceSearchSuccessResponse,
  ResourceTopicsPopularSuccessResponse,
  ResourceTopicsSuccessResponse,
} from '@/api/generated/schema';
import { httpClient, type ApiSuccessBody } from '@/utils/client';

/**
 * 기술 자료 카드 하나. 블로그, PR과 팀 문서, 테코톡이 같은 모양이다.
 * `source`(blog-post, github-pr, github-teamdoc, github-wiki, github-discussion, youtube-tekotalk)로 종류를 구분한다.
 */
export type Material = NonNullable<ResourceSearchSuccessResponse['data']['blogs']>['items'][number];

/** 자료 요약 모달과 자료 정리 보기 페이지가 쓰는 자료 상세 */
export type MaterialDetail = ResourceDetailSuccessResponse['data'];

export type MaterialExperience = MaterialDetail['experiences'][number];

/** 많이 다룬 기술 주제 */
export type PopularTopic = ResourceTopicsPopularSuccessResponse['data'][number];

/** 분야별 기술 주제. side는 COMMON, BACKEND, FRONTEND, MOBILE */
export type TopicField = ResourceTopicsSuccessResponse['data'][number];

/** 이어 읽을 수 있는 자료 종류. 테코톡은 이어 읽기가 없다. */
export type MaterialSectionKind = 'BLOG' | 'DOCUMENT';

/**
 * 검색 결과의 한 종류.
 * nextCursor가 있으면 이어 읽을 자료가 있다. 받은 건수와 관계없이 이 값으로만 판단하고, 커서는 해석하지 않고 그대로 보낸다.
 */
export interface MaterialSection {
  items: Material[];
  nextCursor: string | null;
}

export interface MaterialSearchResult {
  matchedProjects: MatchedProject[];
  blogs: MaterialSection;
  /** PR과 팀 문서 */
  documents: MaterialSection;
  talks: Material[];
}

export interface MatchedProject {
  name: string;
  projectSlug: string;
}

type SearchSection = ResourceSearchSuccessResponse['data']['blogs'];

const toSection = (section: SearchSection): MaterialSection => ({
  items: section?.items ?? [],
  nextCursor: section?.hasNext ? (section.nextCursor ?? null) : null,
});

async function get<T>(
  path: string,
  signal?: AbortSignal,
  searchParams?: Record<string, string | number>,
) {
  const response = await httpClient<ApiSuccessBody<T>>(path, {
    method: 'get',
    signal,
    searchParams,
  });
  if (!response?.data) throw new Error(`응답이 비어 있습니다: ${path}`);
  return response.data;
}

export function recentMaterialsQuery(kind: 'BLOG' | 'TALK', size: number) {
  return queryOptions({
    queryKey: ['explore', 'recent', kind, size],
    queryFn: ({ signal }) =>
      get<ResourceRecentSuccessResponse['data']>('/api/v1/resources/recent', signal, {
        kind,
        size,
      }),
  });
}

export function popularTopicsQuery() {
  return queryOptions({
    queryKey: ['explore', 'popular-topics'],
    queryFn: ({ signal }) =>
      get<PopularTopic[]>('/api/v1/resources/topics/popular', signal, { size: 10 }),
  });
}

export function topicFieldsQuery() {
  return queryOptions({
    queryKey: ['explore', 'topic-fields'],
    queryFn: ({ signal }) => get<TopicField[]>('/api/v1/resources/topics', signal),
  });
}

export function popularKeywordsQuery() {
  return queryOptions({
    queryKey: ['explore', 'popular-keywords'],
    queryFn: ({ signal }) =>
      get<PopularKeywordsSuccessResponse['data']>('/api/v1/resources/popular-keywords', signal),
  });
}

export function materialSearchQuery(keyword: string) {
  return queryOptions({
    queryKey: ['explore', 'search', keyword],
    queryFn: async ({ signal }): Promise<MaterialSearchResult> => {
      const data = await get<ResourceSearchSuccessResponse['data']>(
        '/api/v1/resources/search',
        signal,
        { keyword },
      );
      return {
        matchedProjects: (data.matchedProjects ?? []).flatMap((project) =>
          project.projectSlug
            ? [{ name: project.name ?? project.projectSlug, projectSlug: project.projectSlug }]
            : [],
        ),
        blogs: toSection(data.blogs),
        documents: toSection(data.documents),
        talks: data.talks?.items ?? [],
      };
    },
  });
}

/**
 * 검색 결과 한 종류를 이어 읽는다. 첫 페이지는 검색 응답의 그 종류를 그대로 쓰고, 다음 페이지부터 이어 읽기 API를 부른다.
 * 일반 검색의 커서는 첫 검색에서 정한 순위를 담고 있어, 첫 페이지 뒤에 이어 붙이면 처음부터 받은 목록과 같다.
 * 검색 응답과 커서가 어긋나지 않도록 첫 페이지는 검색 응답을 받은 그때 값으로 고정하고 다시 받지 않는다.
 */
export function materialSectionQuery(
  keyword: string,
  section: MaterialSectionKind,
  firstPage: MaterialSection,
) {
  return infiniteQueryOptions({
    queryKey: ['explore', 'search-more', keyword, section],
    queryFn: async ({ pageParam, signal }): Promise<MaterialSection> => {
      if (pageParam === null) return firstPage;
      const response = await httpClient<
        ApiSuccessBody<
          ResourceSearchMoreSuccessResponse['data'],
          ResourceSearchMoreSuccessResponse['meta']
        >
      >('/api/v1/resources/search/more', {
        method: 'get',
        signal,
        searchParams: { keyword, section, cursor: pageParam },
      });
      if (!response?.data) throw new Error('응답이 비어 있습니다: /api/v1/resources/search/more');
      return {
        items: response.data,
        nextCursor: response.meta?.hasNext ? (response.meta.nextCursor ?? null) : null,
      };
    },
    initialPageParam: null as string | null,
    initialData: { pages: [firstPage], pageParams: [null] },
    getNextPageParam: (lastPage) => lastPage.nextCursor,
    staleTime: Infinity,
  });
}

export function materialDetailQuery(resourceId: number) {
  return queryOptions({
    queryKey: ['explore', 'material', resourceId],
    queryFn: ({ signal }) => get<MaterialDetail>(`/api/v1/resources/${resourceId}`, signal),
  });
}
