import { queryOptions } from '@tanstack/react-query';

import type {
  PopularKeywordsSuccessResponse,
  ResourceDetailSuccessResponse,
  ResourceRecentSuccessResponse,
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

export interface MaterialSearchResult {
  blogs: Material[];
  /** PR과 팀 문서 */
  documents: Material[];
  talks: Material[];
}

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
        blogs: data.blogs?.items ?? [],
        documents: data.documents?.items ?? [],
        talks: data.talks?.items ?? [],
      };
    },
  });
}

export function materialDetailQuery(resourceId: number) {
  return queryOptions({
    queryKey: ['explore', 'material', resourceId],
    queryFn: ({ signal }) => get<MaterialDetail>(`/api/v1/resources/${resourceId}`, signal),
  });
}
