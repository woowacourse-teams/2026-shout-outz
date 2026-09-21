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

export interface ProjectTechTag {
  id: number;
  displayName: string;
}

export interface ProjectMember {
  userId: number | null;
  handle: string | null;
  displayName: string;
  cohort: number | null;
  track: 'BACKEND' | 'ANDROID' | 'FRONTEND' | null;
  avatarUrl: string | null;
  githubAvatarUrl: string | null;
  githubProfileUrl: string | null;
}

/**
 * 프로젝트 카드 공통 응답.
 * 프로젝트 목록과 프로필 프로젝트 탭이 같은 모양으로 내려준다.
 */
// TODO 대체 ProjectListItem
export interface ProjectSummary {
  id: number;
  slug: string;
  title: string;
  tagline: string;
  cohort: number | null;
  thumbnailUrl: string | null;
  likeCount: number;
  commentCount: number;
  techTags: ProjectTechTag[];
  members: ProjectMember[];
}
