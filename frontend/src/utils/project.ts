import {
  type ProjectCreateRequest,
  type ProjectFormErrors,
  type ProjectFormValues,
  type ProjectUpdateRequest,
} from '@/types/project';
import { isApiResponseError } from '@/utils/error';

/**
 * 서버가 받아 주는 GitHub 레포지토리 주소.
 *
 * `docs/프로젝트등록폼규칙.md`의 정규식을 그대로 옮긴 것이다. 호스트만 보던 예전 검사는
 * `http://`, 레포가 빠진 `/owner`, `/tree/main`이 붙은 주소를 전부 통과시켜 서버 400으로 넘겼다.
 */
const GITHUB_REPOSITORY_URL =
  /^https:\/\/(?:www\.)?github\.com\/[A-Za-z0-9._-]+\/[A-Za-z0-9._-]+?(?:\.git)?\/?$/;

/**
 * 서버와 같은 기준으로 글자 수를 센다.
 *
 * 서버는 유니코드 코드포인트로 세는데 `str.length`는 UTF-16 단위라 이모지 대부분을 2자로 센다.
 * 그대로 쓰면 서버가 받아 줄 입력을 프론트가 막는다.
 */
const countCodePoints = (value: string) => [...value].length;

/** 서버가 검증 전에 앞뒤 공백을 자르는 필드의 상한. `descriptionMd`는 자르지 않아 따로 둔다. */
const MAX_LENGTHS = {
  title: 100,
  teamName: 50,
  tagline: 200,
  githubRepositoryUrl: 2_048,
  deploymentUrl: 2_048,
  descriptionMd: 100_000,
} as const;

/**
 * 프로젝트 상세 URL의 경로 조각.
 *
 * 서버가 `GET /api/v1/projects/@{slug}`로 받으므로 화면 주소도 `@`를 붙여 같은 모양으로 둔다.
 * 라우터(1.170.29)는 `{@{$slug}}` 같은 접두사 세그먼트를 지원하지 않아 `@`를 파라미터 값에 담는다.
 */
export const toProjectSlugParam = (slug: string) => `@${slug}`;

/** URL 파라미터에서 순수 slug를 꺼낸다. `@`가 없는 주소로 들어와도 같은 프로젝트를 찾게 둔다. */
export const toProjectSlug = (param: string) => param.replace(/^@/, '');

const isHttpUrl = (value: string) => {
  try {
    const { protocol } = new URL(value);
    return protocol === 'http:' || protocol === 'https:';
  } catch {
    return false;
  }
};

/**
 * 폼 값을 검사해 필드별 오류 문구를 돌려준다. 오류가 없으면 빈 객체다.
 *
 * 필수 여부는 디자인의 `*` 표시를 따른다.
 * - 필수: 프로젝트 이름, 한 줄 소개, 기수, GitHub 레포지토리 URL, 기술 스택
 * - 선택: 팀 이름, 상세 설명, 서비스 배포 URL, 썸네일, 작성자 외 참여 팀원
 */
export function validateProjectForm(values: ProjectFormValues): ProjectFormErrors {
  const errors: ProjectFormErrors = {};

  const title = values.title.trim();
  if (!title) {
    errors.title = '프로젝트 이름을 입력해 주세요.';
  } else if (countCodePoints(title) > MAX_LENGTHS.title) {
    errors.title = `프로젝트 이름은 ${MAX_LENGTHS.title}자까지 입력할 수 있습니다.`;
  }

  const teamName = values.teamName.trim();
  if (countCodePoints(teamName) > MAX_LENGTHS.teamName) {
    errors.teamName = `팀 이름은 ${MAX_LENGTHS.teamName}자까지 입력할 수 있습니다.`;
  }

  const tagline = values.tagline.trim();
  if (!tagline) {
    errors.tagline = '한 줄 소개를 입력해 주세요.';
  } else if (countCodePoints(tagline) > MAX_LENGTHS.tagline) {
    errors.tagline = `한 줄 소개는 ${MAX_LENGTHS.tagline}자까지 입력할 수 있습니다.`;
  }

  if (values.cohort === null) errors.cohort = '우테코 기수를 선택해 주세요.';
  if (values.techTags.length === 0) errors.techTags = '기술 스택을 1개 이상 선택해 주세요.';
  const githubRepositoryUrl = values.githubRepositoryUrl.trim();
  if (!githubRepositoryUrl) {
    errors.githubRepositoryUrl = 'GitHub 레포지토리 URL을 입력해 주세요.';
  } else if (countCodePoints(githubRepositoryUrl) > MAX_LENGTHS.githubRepositoryUrl) {
    errors.githubRepositoryUrl = `GitHub 레포지토리 URL은 ${MAX_LENGTHS.githubRepositoryUrl}자까지 입력할 수 있습니다.`;
  } else if (!GITHUB_REPOSITORY_URL.test(githubRepositoryUrl)) {
    errors.githubRepositoryUrl = 'https://github.com/소유자/레포지토리 형식으로 입력해 주세요.';
  }

  // 배포 URL은 선택이지만 입력했다면 링크로 쓸 수 있는 주소여야 한다.
  const deploymentUrl = values.deploymentUrl.trim();
  if (deploymentUrl && countCodePoints(deploymentUrl) > MAX_LENGTHS.deploymentUrl) {
    errors.deploymentUrl = `서비스 배포 URL은 ${MAX_LENGTHS.deploymentUrl}자까지 입력할 수 있습니다.`;
  } else if (deploymentUrl && !isHttpUrl(deploymentUrl)) {
    errors.deploymentUrl = 'http 또는 https로 시작하는 주소를 입력해 주세요.';
  }

  // 서버가 앞뒤 공백을 자르지 않는 유일한 필드라 입력값 그대로 센다.
  if (countCodePoints(values.descriptionMd) > MAX_LENGTHS.descriptionMd) {
    errors.descriptionMd = `상세 설명은 ${MAX_LENGTHS.descriptionMd.toLocaleString()}자까지 입력할 수 있습니다.`;
  }

  return errors;
}

/** 작성자를 첫 팀원으로 포함해 등록 요청 본문으로 바꾼다. 비어 있는 선택 입력은 null로 보낸다. */
export function toProjectCreateRequest(
  values: ProjectFormValues,
  authorHandle: string,
): ProjectCreateRequest {
  if (values.cohort === null) {
    throw new Error('기수를 고르지 않은 폼 값은 등록 요청으로 바꿀 수 없습니다.');
  }

  return {
    title: values.title.trim(),
    teamName: values.teamName.trim(),
    tagline: values.tagline.trim(),
    cohort: values.cohort,
    thumbnailImageId: values.thumbnailImageId,
    githubRepositoryUrl: values.githubRepositoryUrl.trim(),
    deploymentUrl: values.deploymentUrl.trim() || null,
    // 서버가 자르지 않는 필드다. 여기서 자르면 코드블록 들여쓰기처럼 의미 있는 공백이 사라진다.
    descriptionMd: values.descriptionMd,
    techTagIds: values.techTags.map((tag) => tag.id),
    memberHandles: [
      authorHandle,
      ...values.members
        .filter((member) => member.handle !== authorHandle)
        .map((member) => member.handle),
    ],
  };
}

export function toProjectUpdateRequest(
  values: ProjectFormValues,
  authorHandle: string,
): ProjectUpdateRequest {
  const request = toProjectCreateRequest(values, authorHandle);
  return {
    ...request,
    serviceStatus: values.deploymentUrl.trim() ? (values.serviceStatus ?? 'CLOSED') : 'CLOSED',
  };
}

/** 요청 본문 필드명 → 폼 필드명. 이름이 다른 것만 적는다. */
const FIELD_BY_REQUEST_FIELD: Record<string, keyof ProjectFormValues> = {
  techTagIds: 'techTags',
  memberHandles: 'members',
};

/**
 * 에러 코드만 오고 `details`가 없는 검증. 코드로 보여 줄 입력칸을 정한다.
 *
 * `docs/프로젝트등록폼규칙.md`의 "서비스 검증 에러에는 필드 정보가 없음" 표를 옮긴 것이다.
 */
const FIELD_BY_ERROR_CODE: Record<string, keyof ProjectFormValues> = {
  INVALID_COHORT: 'cohort',
  PROJECT_DUPLICATE_GITHUB_REPOSITORY: 'githubRepositoryUrl',
  PROJECT_DUPLICATE_SLUG: 'githubRepositoryUrl',
  PROJECT_INVALID_SLUG: 'githubRepositoryUrl',
  PROJECT_DUPLICATE_TECH_TAG: 'techTags',
  PROJECT_INVALID_TECH_TAG: 'techTags',
  PROJECT_INVALID_MEMBER: 'members',
  PROJECT_DUPLICATE_MEMBER: 'members',
  PROJECT_MEMBER_REQUIRED: 'members',
  PROJECT_INVALID_THUMBNAIL: 'thumbnailImageId',
  PROJECT_THUMBNAIL_NOT_READY: 'thumbnailImageId',
  PROJECT_INVALID_DESCRIPTION_MEDIA: 'descriptionMd',
  PROJECT_DESCRIPTION_MEDIA_NOT_READY: 'descriptionMd',
};

/** `details[].field`가 폼에 있는 필드인지 확인할 때만 쓴다. */
const FORM_FIELDS: Record<keyof ProjectFormValues, true> = {
  title: true,
  teamName: true,
  tagline: true,
  cohort: true,
  thumbnailImageId: true,
  githubRepositoryUrl: true,
  deploymentUrl: true,
  descriptionMd: true,
  techTags: true,
  members: true,
  serviceStatus: true,
};

/**
 * 서버가 돌려준 등록 실패를 필드별 오류로 편다. 붙일 곳이 없으면 빈 객체다.
 *
 * 두 갈래가 있다.
 * - `VALIDATION_FAILED`: `details`에 `{field, message}`가 모두 담겨 온다.
 * - 그 밖의 코드: `details`가 없어 코드로 입력칸을 정하고 서버 메시지를 그대로 쓴다.
 *
 * 빈 객체를 돌려주면 호출부가 폼 아래 공통 문구를 띄운다. 권한 문제(`PROJECT_REGISTRATION_FORBIDDEN`)나
 * 알 수 없는 코드가 여기에 해당한다.
 */
export function toProjectFormErrors(error: unknown): ProjectFormErrors {
  if (!isApiResponseError(error)) return {};

  const { code, message, details } = error.data;

  if (details && details.length > 0) {
    return Object.fromEntries(
      details.flatMap((detail) => {
        const field = FIELD_BY_REQUEST_FIELD[detail.field] ?? detail.field;
        return field in FORM_FIELDS ? [[field, detail.message]] : [];
      }),
    );
  }

  const field = FIELD_BY_ERROR_CODE[code];

  return field ? { [field]: message } : {};
}
