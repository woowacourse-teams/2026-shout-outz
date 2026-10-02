/**
 * 서버 응답/요청 타입의 공통 진입점.
 *
 * `src/api/generated/schema.ts`는 `npm run generate:api`가 서버의 openapi3.yaml로 만든 파일이다.
 * 손으로 고치지 않는다. 현재 백엔드 DTO와 생성 타입이 다른 프로젝트 응답은 아래에서
 * DTO에 맞게 덮어쓴다.
 *
 * 생성된 타입은 `XxxSuccessResponse`라는 봉투(`{ status, data, meta }`) 모양이라 그대로 쓰기 불편하다.
 * 아래 `Data`/`Meta`/`Item`으로 봉투를 벗겨 화면이 다루는 알맹이만 이름 붙인다.
 *
 */
import type {
  AdminProjectMigrationUpdateRequest,
  AdminVerificationRequestFindAllSuccessResponse,
  AuthSessionSuccessResponse,
  CohortFindAllSuccessResponse,
  FeedCommentFindAllSuccessResponse,
  FeedFindAllSuccessResponse,
  FeedFindSuccessResponse,
  EventCreateRequest,
  HomeBannerAdminFindAllSuccessResponse,
  HomeBannerFindAllSuccessResponse,
  HomeBannerUpsertRequest,
  HomeStatisticsSuccessResponse,
  NewsFindAllSuccessResponse,
  NewsFindDetailSuccessResponse,
  NoticeCreateRequest,
  NotificationListSuccessResponse,
  NotificationUnreadCountSuccessResponse,
  ProjectCreateRequest as GeneratedProjectCreateRequest,
  ProjectFilterOptionsSuccessResponse,
  ProjectFindAllSuccessResponse,
  ProjectFindDetailSuccessResponse,
  ProjectUpdateRequest as GeneratedProjectUpdateRequest,
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

/** 병합된 API가 내려주지만 아직 생성 타입에 없는 팀원 유형을 화면 타입에 반영한다. */
type WithProjectMemberType<T extends { members: unknown[] }> = Omit<T, 'members'> & {
  members: Item<T['members']>[];
  likedByMe?: boolean;
  approvalStatus?: ProjectApprovalStatus;
  rejectReason?: string | null;
};

/** 피드 작성자의 기수 번호 대신 현재 기수 여부를 내려주는 최신 응답. */
type WithFeedAuthorStatus<T extends { author: object }> = Omit<T, 'author'> & {
  author: Omit<T['author'], 'cohort'> & { isCurrent?: boolean | null };
};

/** 피드 응답에 서버가 제공하지만 생성 타입에서 빠진 반응·개수 필드. */
type WithFeedReactionState<T extends { author: object }> = WithFeedAuthorStatus<T> & {
  likeCount?: number;
  likedByMe?: boolean;
  commentCount?: number;
  feedType?: 'POST' | 'QUESTION';
  isAnonymous?: boolean;
};

/** 커서 페이지네이션 meta. 목록 응답이 공통으로 쓴다. */
export type CursorMeta = Meta<FeedFindAllSuccessResponse>;

// ── 공통 열거값 ──────────────────────────────────────────────────────────────

export type Track = NonNullable<Data<UserProfileSuccessResponse>['track']>;
export type UserType = Data<UserProfileSuccessResponse>['userType'];
export type ProjectApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

// ── 세션 ────────────────────────────────────────────────────────────────────

export type SessionData = Data<AuthSessionSuccessResponse>;

// ── 사용자 ──────────────────────────────────────────────────────────────────

export type UserProfileData = Data<UserProfileSuccessResponse>;
export type UserProfileSummaryData = Data<UserProfileSummarySuccessResponse>;
export type UserSearchItem = Item<Data<UserSearchSuccessResponse>>;

export type VerificationRequestData = Data<UserVerificationRequestSuccessResponse>;
export type VerificationRequestBody = UserVerificationRequestCreateRequest;

// ── 카테고리 · 기수 · 기술 스택 ───────────────────────────────────────────────

export type CohortItem = Item<Data<CohortFindAllSuccessResponse>['items']>;
export type TechTagItem = Item<Data<TechTagFindAllSuccessResponse>['items']>;

// ── 피드 ────────────────────────────────────────────────────────────────────

export type FeedData = WithFeedReactionState<Data<FeedFindSuccessResponse>>;
export type FeedListItemData = WithFeedReactionState<Item<Data<FeedFindAllSuccessResponse>>>;
export type UserFeedListItemData = WithFeedReactionState<
  Item<Data<UserFeedFindAllSuccessResponse>>
>;
type GeneratedFeedComment = Item<Data<FeedCommentFindAllSuccessResponse>>;
export type FeedCommentData = Omit<GeneratedFeedComment, 'author'> & {
  agreeCount?: number;
  agreedByMe?: boolean;
  isAnonymous?: boolean;
  author: Omit<GeneratedFeedComment['author'], 'cohort'> & {
    handle?: string | null;
    isCurrent?: boolean | null;
  };
};

// ── 프로젝트 ────────────────────────────────────────────────────────────────

export type ProjectCreateBody = GeneratedProjectCreateRequest;
export type ProjectUpdateRequest = GeneratedProjectUpdateRequest;
/** ProjectCreateResponse.java: 등록 응답은 slug만 제공한다. */
export type ProjectCreatedData = { slug: string };
/** ProjectUpdateResponse.java: 수정 결과에는 slug와 승인 상태가 있다. */
export type ProjectUpdatedData = { slug: string; approvalStatus: ProjectApprovalStatus };
/** ProjectFindAllResponse.Item.java: 일반 목록에는 프로젝트 ID가 없다. */
export type ProjectListItemData = Omit<
  WithProjectMemberType<Item<Data<ProjectFindAllSuccessResponse>>>,
  'id'
>;
export type ProjectListMetaData = Meta<ProjectFindAllSuccessResponse>;
/** UserProjectResponse.java: 프로필 목록은 slug와 반려 사유를 제공한다. */
export type UserProjectListItemData = Omit<
  WithProjectMemberType<Item<Data<UserProjectFindAllSuccessResponse>>>,
  'id'
> & { rejectReason: string | null };
/** ProjectDetailResponse.java: 상세 응답에도 프로젝트 ID가 없다. */
export type ProjectDetailData = Omit<
  WithProjectMemberType<Data<ProjectFindDetailSuccessResponse>>,
  'id'
>;
export type ProjectFilterOptionsData = Data<ProjectFilterOptionsSuccessResponse>;

// ── 알림 ────────────────────────────────────────────────────────────────────

export type NotificationItemData = Item<Data<NotificationListSuccessResponse>>;
export type NotificationUnreadCountData = Data<NotificationUnreadCountSuccessResponse>;

// ── 홈 ──────────────────────────────────────────────────────────────────────

export type HomeStatisticsData = Data<HomeStatisticsSuccessResponse>;
export type HomeBannerItem = Item<Data<HomeBannerFindAllSuccessResponse>>;

// ── 소식 ────────────────────────────────────────────────────────────────────

export type NewsListItemData = Item<Data<NewsFindAllSuccessResponse>>;
export type NewsDetailData = Data<NewsFindDetailSuccessResponse>;

export type NoticeCreateBody = NoticeCreateRequest;
export type EventCreateBody = EventCreateRequest;

// ── 관리자 ──────────────────────────────────────────────────────────────────

export type AdminVerificationListData = Data<AdminVerificationRequestFindAllSuccessResponse>;
export type AdminVerificationItem = Item<AdminVerificationListData>;
export type AdminHomeBannerItem = Item<Data<HomeBannerAdminFindAllSuccessResponse>>;
export type HomeBannerUpsertBody = HomeBannerUpsertRequest;
export type AdminProjectUpdateRequest = AdminProjectMigrationUpdateRequest;
