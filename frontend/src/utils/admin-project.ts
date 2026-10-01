import type { AdminProjectDetail, AdminProjectUpdateBody } from '@/types/admin';
import type { ProjectApprovalStatus } from '@/types/api';
import type { ProjectFormErrors, ProjectFormValues } from '@/types/project';
import { isApiResponseError } from '@/utils/error';
import { validateProjectForm } from '@/utils/project';

/**
 * 관리자 프로젝트 수정 폼 값.
 *
 * 일반 수정 폼 값에 관리자만 바꿀 수 있는 slug와 승인 상태를 더한다.
 * 팀원은 관리자 수정 API가 받지 않아 빈 배열로 둔다.
 */
export interface AdminProjectFormValues extends ProjectFormValues {
  slug: string;
  approvalStatus: ProjectApprovalStatus;
  serviceStatus: 'OPERATING' | 'CLOSED';
}

export type AdminProjectFormErrors = ProjectFormErrors &
  Partial<Record<'slug' | 'approvalStatus', string>>;

/** 서버 `Slug`와 같은 규칙. 소문자·숫자를 하이픈 하나로 잇는다. */
const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const SLUG_MAX_LENGTH = 100;

export function toAdminProjectFormValues(project: AdminProjectDetail): AdminProjectFormValues {
  return {
    title: project.title,
    teamName: project.teamName,
    tagline: project.tagline,
    cohort: project.cohort,
    thumbnailImageId: project.thumbnailImageId ?? null,
    githubRepositoryUrl: project.githubRepositoryUrl,
    deploymentUrl: project.deploymentUrl ?? '',
    descriptionMd: project.descriptionMd ?? '',
    techTags: project.techTags,
    members: [],
    slug: project.slug,
    approvalStatus: project.approvalStatus,
    serviceStatus: project.serviceStatus,
  };
}

export function validateAdminProjectForm(values: AdminProjectFormValues): AdminProjectFormErrors {
  const errors: AdminProjectFormErrors = validateProjectForm(values);
  const slug = values.slug.trim();
  if (!slug) {
    errors.slug = 'slug를 입력해 주세요.';
  } else if (slug.length > SLUG_MAX_LENGTH) {
    errors.slug = `slug는 ${SLUG_MAX_LENGTH}자까지 입력할 수 있습니다.`;
  } else if (!SLUG_PATTERN.test(slug)) {
    errors.slug = '영문 소문자와 숫자를 하이픈(-)으로 이어 주세요. 예: shout-outz';
  }
  return errors;
}

/** 이 화면이 고치는 필드. 생성·수정 시각, 조회수, 스타 수 같은 이관 보정용 필드는 다루지 않는다. */
type EditableBody = Required<
  Pick<
    AdminProjectUpdateBody,
    | 'title'
    | 'teamName'
    | 'tagline'
    | 'cohort'
    | 'slug'
    | 'thumbnailImageId'
    | 'githubRepositoryUrl'
    | 'deploymentUrl'
    | 'descriptionMd'
    | 'serviceStatus'
    | 'approvalStatus'
    | 'techTagIds'
  >
>;

/** 요청에 실을 모양으로 맞춘 값. 앞뒤 공백과 빈 선택 입력을 서버가 받는 값으로 바꾼다. */
function normalize(values: AdminProjectFormValues): EditableBody {
  return {
    title: values.title.trim(),
    teamName: values.teamName.trim(),
    tagline: values.tagline.trim(),
    cohort: values.cohort,
    slug: values.slug.trim(),
    thumbnailImageId: values.thumbnailImageId,
    githubRepositoryUrl: values.githubRepositoryUrl.trim(),
    deploymentUrl: values.deploymentUrl.trim() || null,
    // 서버가 자르지 않는 필드다. 코드블록 들여쓰기 같은 공백을 지키려고 그대로 보낸다.
    descriptionMd: values.descriptionMd || null,
    serviceStatus: values.serviceStatus,
    approvalStatus: values.approvalStatus,
    techTagIds: values.techTags.map((tag) => tag.id),
  };
}

// 기술 스택은 고른 순서가 달라도 같은 목록으로 본다.
const tagIdsKey = (ids: number[] | null) => [...(ids ?? [])].sort((a, b) => a - b).join(',');

/**
 * 처음 값과 달라진 필드만 담은 수정 요청을 만든다.
 *
 * 관리자 수정 API는 보낸 필드만 덮어쓴다. 그대로인 필드까지 보내면 다른 관리자가 그 사이에
 * 바꾼 값을 되돌릴 수 있어서, 바뀐 것만 보낸다.
 */
export function toAdminProjectUpdateBody(
  initial: AdminProjectFormValues,
  values: AdminProjectFormValues,
): AdminProjectUpdateBody {
  const before = normalize(initial);
  const after = normalize(values);
  const body: AdminProjectUpdateBody = {};

  for (const field of Object.keys(after) as (keyof EditableBody)[]) {
    const changed =
      field === 'techTagIds'
        ? tagIdsKey(before.techTagIds) !== tagIdsKey(after.techTagIds)
        : before[field] !== after[field];
    if (changed) Object.assign(body, { [field]: after[field] });
  }
  return body;
}

/** 서버가 짚어 준 필드 오류를 폼 칸에 붙인다. `techTagIds`는 기술 스택 칸으로 옮긴다. */
export function toAdminProjectFormErrors(error: unknown): AdminProjectFormErrors {
  if (!isApiResponseError(error)) return {};
  const errors: AdminProjectFormErrors = {};
  for (const { field, message } of error.data.details ?? []) {
    const key = field === 'techTagIds' ? 'techTags' : field;
    Object.assign(errors, { [key]: message });
  }
  return errors;
}
