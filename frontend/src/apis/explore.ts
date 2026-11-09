import { queryOptions } from '@tanstack/react-query';

import { httpClient, type ApiSuccessBody } from '@/utils/client';

// MOCK-API: 탐색 API가 아직 없어 화면에 필요한 모양대로 임의로 정했다. 실제 API가 생기면 바뀔 수 있다.
// 응답은 src/api/mock/explore.ts의 MSW 핸들러가 내려준다.

export type MaterialKind = 'BLOG' | 'PR' | 'TEAM_DOC' | 'TECH_TALK';

/** 자료를 만든 팀(프로젝트). 테코톡처럼 팀이 없는 자료도 있다. */
export interface MaterialProject {
  name: string;
  logoUrl: string | null;
}

export interface Material {
  id: string;
  kind: MaterialKind;
  title: string;
  url: string;
  /** YYYY-MM-DD */
  publishedAt: string | null;
  /** 한 줄 소개. 이 자료를 읽으면 무엇을 얻는지 */
  summary: string | null;
  project: MaterialProject | null;
  /** 블로그 글의 파비콘 */
  faviconUrl: string | null;
  /** 테코톡 발표자 */
  speaker: string | null;
  /** 테코톡 영상 썸네일 */
  thumbnailUrl: string | null;
}

export interface MaterialExperience {
  problem: string;
  cause: string;
  solution: string;
  result: string;
}

export interface MaterialDetail extends Material {
  techTags: string[];
  learnings: string[];
  experiences: MaterialExperience[];
}

export interface TopicField {
  name: string;
  topics: string[];
}

export interface ExploreHome {
  recentBlogs: Material[];
  recentTechTalks: Material[];
  popularTopics: string[];
  topicFields: TopicField[];
}

export interface ExploreSearchResult {
  query: string;
  /** 검색어와 맞는 기술 주제 이름 */
  topics: string[];
  blogs: Material[];
  /** PR과 팀 문서 */
  documents: Material[];
  techTalks: Material[];
}

export async function fetchExploreHome(signal?: AbortSignal) {
  const response = await httpClient<ApiSuccessBody<ExploreHome>>('/api/v1/explore', {
    method: 'get',
    signal,
  });
  if (!response?.data) throw new Error('탐색 응답이 비어 있습니다.');
  return response.data;
}

export async function fetchExploreSearch(query: string, signal?: AbortSignal) {
  const response = await httpClient<ApiSuccessBody<ExploreSearchResult>>('/api/v1/explore/search', {
    method: 'get',
    signal,
    searchParams: { q: query },
  });
  if (!response?.data) throw new Error('탐색 검색 응답이 비어 있습니다.');
  return response.data;
}

export async function fetchMaterialDetail(materialId: string, signal?: AbortSignal) {
  const response = await httpClient<ApiSuccessBody<MaterialDetail>>(
    `/api/v1/explore/materials/${encodeURIComponent(materialId)}`,
    { method: 'get', signal },
  );
  if (!response?.data) throw new Error('자료 요약 응답이 비어 있습니다.');
  return response.data;
}

export function exploreHomeQuery() {
  return queryOptions({
    queryKey: ['explore', 'home'],
    queryFn: ({ signal }) => fetchExploreHome(signal),
  });
}

export function exploreSearchQuery(query: string) {
  return queryOptions({
    queryKey: ['explore', 'search', query],
    queryFn: ({ signal }) => fetchExploreSearch(query, signal),
  });
}

export function materialDetailQuery(materialId: string) {
  return queryOptions({
    queryKey: ['explore', 'material', materialId],
    queryFn: ({ signal }) => fetchMaterialDetail(materialId, signal),
  });
}
