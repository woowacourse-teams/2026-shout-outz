import { mutationOptions, queryOptions } from '@tanstack/react-query';

import type {
  MyResourceReportFindSuccessResponse,
  ResourceAdditionReportRequest,
  ResourceAdditionReportSuccessResponse,
  ResourceRemovalReportRequest,
  ResourceRemovalReportSuccessResponse,
} from '@/api/generated/schema';
import { httpClient } from '@/utils/client';

/** 제보 메모 최대 길이 (서버는 공백을 뺀 뒤 센다) */
export const RESOURCE_REPORT_MEMO_MAX_LENGTH = 500;

/** 한 번에 "필요 없어요"로 보낼 수 있는 자료 수 */
export const RESOURCE_REMOVAL_MAX_COUNT = 100;

export type MyResourceReport = MyResourceReportFindSuccessResponse['data'][number];

/**
 * 스키마는 resourceIds를 `(object | boolean | string | number)[]`로 뽑는다(생성기가 배열 원소 타입을 못 읽은 결과다).
 * 서버가 받는 값은 자료 ID라 number[]로 좁혀 쓴다.
 */
export interface ResourceRemovalReportInput extends Omit<
  ResourceRemovalReportRequest,
  'resourceIds'
> {
  resourceIds: number[];
}

const reportPath = (slug: string) => `/api/v1/projects/@${slug}/resource-reports`;

/** 빠진 PR이나 팀 문서의 GitHub 주소를 알려 준다. 그 프로젝트 팀원만 보낼 수 있다. */
export async function reportResourceAddition(slug: string, input: ResourceAdditionReportRequest) {
  const response = await httpClient<ResourceAdditionReportSuccessResponse>(
    `${reportPath(slug)}/additions`,
    { method: 'post', json: input },
  );
  if (!response) throw new Error('제보 결과를 확인하지 못했습니다.');
  return response.data;
}

/** 이 팀의 PR·팀 문서 중 필요 없는 것을 골라 알려 준다. 그 프로젝트 팀원만 보낼 수 있다. */
export async function reportResourceRemoval(slug: string, input: ResourceRemovalReportInput) {
  const response = await httpClient<ResourceRemovalReportSuccessResponse>(
    `${reportPath(slug)}/removals`,
    { method: 'post', json: input },
  );
  if (!response) throw new Error('제보 결과를 확인하지 못했습니다.');
  return response.data;
}

export const myResourceReportsQuery = (slug: string) =>
  queryOptions({
    queryKey: ['project-detail', slug, 'resource-reports', 'me'],
    queryFn: async ({ signal }) => {
      const response = await httpClient<MyResourceReportFindSuccessResponse>(
        `${reportPath(slug)}/me`,
        { method: 'get', signal },
      );
      return response?.data ?? [];
    },
  });

export const resourceAdditionMutation = (slug: string) =>
  mutationOptions({
    mutationFn: (input: ResourceAdditionReportRequest) => reportResourceAddition(slug, input),
    retry: false,
  });

export const resourceRemovalMutation = (slug: string) =>
  mutationOptions({
    mutationFn: (input: ResourceRemovalReportInput) => reportResourceRemoval(slug, input),
    retry: false,
  });
