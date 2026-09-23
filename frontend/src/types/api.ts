/**
 * 서버 응답/요청 타입의 단일 출처.
 *
 * `src/api/generated/schema.ts`는 `npm run generate:api`가 서버의 openapi3.yaml로 만든 파일이다.
 * 손으로 고치지 않으며, 화면 코드가 쓰는 이름은 전부 이 파일에서 그 스키마를 깎아 만든다.
 *
 * 생성된 타입은 `XxxSuccessResponse`라는 봉투(`{ status, data, meta }`) 모양이라 그대로 쓰기 불편하다.
 * 아래 `Data`/`Meta`/`Item`으로 봉투를 벗겨 화면이 다루는 알맹이만 이름 붙인다.
 *
 * 스키마에 없는 응답(홈 통계, 미디어 업로드 등)은 `docs/api-types.md`에 따로 적어 두었다.
 */
import type {
  AuthSessionSuccessResponse,
  CohortFindAllSuccessResponse,
  FeedCommentFindAllSuccessResponse,
  FeedFindAllSuccessResponse,
  FeedFindSuccessResponse,
  HomeBannerFindAllSuccessResponse,
  HomeStatisticsSuccessResponse,
  NewsFindAllSuccessResponse,
  NewsFindDetailSuccessResponse,
  ProjectCreateRequest as GeneratedProjectCreateRequest,
  ProjectCreateSuccessResponse,
  ProjectFilterOptionsSuccessResponse,
  ProjectFindAllSuccessResponse,
  ProjectFindDetailSuccessResponse,
  TechTagFindAllSuccessResponse,
  UserFeedFindAllSuccessResponse,
  UserProfileSuccessResponse,
  UserProfileSummarySuccessResponse,
  UserProjectFindAllSuccessResponse,
  UserSearchSuccessResponse,
  UserVerificationRequestCreateRequest,
  UserVerificationRequestSuccessResponse,
} from '@/api/generated/schema';

/** 봉투에서 `data`만 꺼낸다. 생성기가 `data?`로 뽑은 응답도 있어 undefined를 벗긴다. */
type Data<T extends { data?: unknown }> = NonNullable<T['data']>;

/** 봉투에서 `meta`만 꺼낸다. */
type Meta<T extends { meta?: unknown }> = NonNullable<T['meta']>;

/** 배열 타입에서 원소 타입을 꺼낸다. */
export type Item<T> = T extends readonly (infer E)[] ? E : never;

/** 커서 페이지네이션 meta. 목록 응답이 공통으로 쓴다. */
export type CursorMeta = Meta<FeedFindAllSuccessResponse>;

// ── 공통 열거값 ──────────────────────────────────────────────────────────────

export type Track = NonNullable<Data<UserProfileSuccessResponse>['track']>;
export type UserType = Data<UserProfileSuccessResponse>['userType'];

// ── 세션 ────────────────────────────────────────────────────────────────────

export type SessionData = Data<AuthSessionSuccessResponse>;

// ── 사용자 ──────────────────────────────────────────────────────────────────

export type UserProfileData = Data<UserProfileSuccessResponse>;
export type UserProfileSummaryData = Data<UserProfileSummarySuccessResponse>;
export type UserSearchItem = Item<Data<UserSearchSuccessResponse>['items']>;

export type VerificationRequestData = Data<UserVerificationRequestSuccessResponse>;
export type VerificationRequestBody = UserVerificationRequestCreateRequest;

// ── 카테고리 · 기수 · 기술 스택 ───────────────────────────────────────────────

export type CohortItem = Item<Data<CohortFindAllSuccessResponse>['items']>;
export type TechTagItem = Item<Data<TechTagFindAllSuccessResponse>['items']>;

// ── 피드 ────────────────────────────────────────────────────────────────────

export type FeedData = Data<FeedFindSuccessResponse>;
export type FeedListItemData = Item<Data<FeedFindAllSuccessResponse>>;
export type UserFeedListItemData = Item<Data<UserFeedFindAllSuccessResponse>>;
export type FeedCommentData = Item<Data<FeedCommentFindAllSuccessResponse>>;

// ── 프로젝트 ────────────────────────────────────────────────────────────────

export type ProjectCreateBody = GeneratedProjectCreateRequest;
export type ProjectCreatedData = Data<ProjectCreateSuccessResponse>;
export type ProjectListItemData = Item<Data<ProjectFindAllSuccessResponse>>;
export type ProjectListMetaData = Meta<ProjectFindAllSuccessResponse>;
export type UserProjectListItemData = Item<Data<UserProjectFindAllSuccessResponse>>;
export type ProjectDetailData = Data<ProjectFindDetailSuccessResponse>;
export type ProjectFilterOptionsData = Data<ProjectFilterOptionsSuccessResponse>;

// ── 홈 ──────────────────────────────────────────────────────────────────────

export type HomeStatisticsData = Data<HomeStatisticsSuccessResponse>;
export type HomeBannerItem = Item<Data<HomeBannerFindAllSuccessResponse>>;

// ── 소식 ────────────────────────────────────────────────────────────────────

export type NewsListItemData = Item<Data<NewsFindAllSuccessResponse>>;
export type NewsDetailData = Data<NewsFindDetailSuccessResponse>;
