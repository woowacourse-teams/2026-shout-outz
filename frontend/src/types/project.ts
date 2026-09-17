// TODO 대체 ProjectCreateRequest
export interface ProjectCreateRequest {
  title: string;
  teamName: string;
  tagline: string;
  cohort: number;
  thumbnailMediaId: number | null;
  githubRepositoryUrl: string;
  deploymentUrl: string | null;
  descriptionMd: string;
  /** 배열 순서를 그대로 표시 순서로 저장한다 */
  techTagIds: number[];
  /** 작성자 본인은 포함하지 않는다. 서버가 작성자를 맨 앞에 저장한다 */
  memberHandles: string[];
}

export interface ProjectCreated {
  id: number;
  approvalStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
}

// TODO 대체 CohortOption
export interface CohortOption {
  cohort: number;
  year: number;
}

// TODO 대체 TechTag
export interface TechTag {
  id: number;
  displayName: string;
}

// TODO 대체 UserSearchItem
export interface CrewSearchItem {
  handle: string;
  displayName: string;
  userType: string;
  track: string | null;
  cohort: number | null;
  avatarImageId: number | null;
}

/**
 * 프로젝트 등록 폼이 들고 있는 값.
 *
 * 화면에 필요한 모양으로 보관하다가 제출할 때 `toProjectCreateRequest`로 요청 모양으로 바꾼다.
 */
export interface ProjectFormValues {
  title: string;
  teamName: string;
  tagline: string;
  cohort: number | null;
  thumbnailMediaId: number | null;
  githubRepositoryUrl: string;
  deploymentUrl: string;
  descriptionMd: string;
  /** 칩에 이름을 보여줘야 해서 id만 들고 있지 않는다 */
  techTags: TechTag[];
  memberHandles: string[];
}

export type ProjectFormErrors = Partial<Record<keyof ProjectFormValues, string>>;
