import {
  type ProjectCreateRequest,
  type ProjectFormErrors,
  type ProjectFormValues,
} from '@/types/project';

const GITHUB_HOST = 'github.com';

const isHttpUrl = (value: string) => {
  try {
    const { protocol } = new URL(value);
    return protocol === 'http:' || protocol === 'https:';
  } catch {
    return false;
  }
};

const isGithubRepositoryUrl = (value: string) =>
  isHttpUrl(value) && new URL(value).hostname.replace(/^www\./, '') === GITHUB_HOST;

/**
 * 폼 값을 검사해 필드별 오류 문구를 돌려준다. 오류가 없으면 빈 객체다.
 *
 * 필수 여부는 디자인의 `*` 표시를 따른다.
 * - 필수: 프로젝트 이름, 한 줄 소개, 기수, GitHub 레포지토리 URL, 기술 스택, 참여 팀원
 * - 선택: 팀 이름, 상세 설명, 서비스 배포 URL, 썸네일
 */
export function validateProjectForm(values: ProjectFormValues): ProjectFormErrors {
  const errors: ProjectFormErrors = {};

  if (!values.title.trim()) errors.title = '프로젝트 이름을 입력해 주세요.';
  if (!values.tagline.trim()) errors.tagline = '한 줄 소개를 입력해 주세요.';
  if (values.cohort === null) errors.cohort = '우테코 기수를 선택해 주세요.';
  if (values.techTags.length === 0) errors.techTags = '기술 스택을 1개 이상 선택해 주세요.';
  if (values.memberHandles.length === 0) {
    errors.memberHandles = '참여 팀원을 1명 이상 선택해 주세요.';
  }

  const githubRepositoryUrl = values.githubRepositoryUrl.trim();
  if (!githubRepositoryUrl) {
    errors.githubRepositoryUrl = 'GitHub 레포지토리 URL을 입력해 주세요.';
  } else if (!isGithubRepositoryUrl(githubRepositoryUrl)) {
    errors.githubRepositoryUrl = 'github.com 레포지토리 주소를 입력해 주세요.';
  }

  // 배포 URL은 선택이지만 입력했다면 링크로 쓸 수 있는 주소여야 한다.
  const deploymentUrl = values.deploymentUrl.trim();
  if (deploymentUrl && !isHttpUrl(deploymentUrl)) {
    errors.deploymentUrl = 'http 또는 https로 시작하는 주소를 입력해 주세요.';
  }

  return errors;
}

/** 검증을 통과한 폼 값을 등록 요청 본문으로 바꾼다. 비어 있는 선택 입력은 null로 보낸다. */
export function toProjectCreateRequest(values: ProjectFormValues): ProjectCreateRequest {
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
    descriptionMd: values.descriptionMd.trim(),
    techTagIds: values.techTags.map((tag) => tag.id),
    memberHandles: values.memberHandles,
  };
}
