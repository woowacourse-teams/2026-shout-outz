import type {
  CohortItem,
  Item,
  ProjectCreateBody,
  ProjectCreatedData,
  ProjectDetailData,
  ProjectFilterOptionsData,
  ProjectListItemData,
  ProjectListMetaData,
  TechTagItem,
  UserProjectListItemData,
  UserSearchItem,
} from '@/types/api';

/** POST /api/v1/projects 요청 본문 */
export type ProjectCreateRequest = ProjectCreateBody;

/** 등록 응답은 `{ projectId, slug }`다. 승인 상태와 생성 시각은 내려오지 않는다. */
export type ProjectCreated = ProjectCreatedData;

export type CohortOption = CohortItem;
export type TechTag = TechTagItem;

/** 참여 팀원 검색 결과. `GET /api/v1/users/search` */
export type CrewSearchItem = UserSearchItem;

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
  thumbnailImageId: number | null;
  githubRepositoryUrl: string;
  deploymentUrl: string;
  descriptionMd: string;
  /** 칩에 이름을 보여줘야 해서 id만 들고 있지 않는다 */
  techTags: TechTag[];
  /** 제출에는 handle만 쓰지만, 칩에 이름을 보여줘야 해서 고른 크루를 통째로 들고 있는다 */
  members: CrewSearchItem[];
}

export type ProjectFormErrors = Partial<Record<keyof ProjectFormValues, string>>;

/** 목록 응답 한 건. `GET /api/v1/projects` */
export type ProjectListItem = ProjectListItemData;
export type ProjectListMeta = ProjectListMetaData;

/** 프로필 프로젝트 탭 한 건. `GET /api/v1/users/{handle}/projects` */
export type UserProjectListItem = UserProjectListItemData;

/**
 * 프로젝트 카드가 다루는 공통 모양.
 *
 * 목록과 프로필 탭이 같은 필드를 내려주지만 프로필 탭에는 `members[].userId`가 없고
 * 목록에는 `serviceStatus`·`teamName`이 없다. 카드가 쓰는 교집합만 남긴다.
 */
export type ProjectSummary = ProjectListItem;

export type ProjectTechTag = Item<ProjectListItem['techTags']>;

/**
 * 프로젝트 목록을 좁히는 조건.
 *
 * URL 검색 파라미터와 모달이 같은 모양을 쓴다. 비어 있으면 전체 조회다.
 */
export interface ProjectFilter {
  keyword: string;
  cohorts: number[];
  techTagIds: number[];
  sort: ProjectSort;
}

/** `GET /api/v1/projects`의 sort. 스키마가 쿼리 파라미터는 내보내지 않아 여기서 정의한다. */
export type ProjectSort = 'LATEST' | 'POPULAR';

/** 필터 선택지와 각 선택지의 프로젝트 수. `GET /api/v1/projects/filters` */
export type ProjectFilterOptions = ProjectFilterOptionsData;
export type ProjectFilterCohort = Item<ProjectFilterOptions['cohorts']>;
export type ProjectFilterTechTag = Item<ProjectFilterOptions['techTags']>;

/** 상세 응답. `GET /api/v1/projects/{projectId}` */
export type ProjectDetail = ProjectDetailData;
