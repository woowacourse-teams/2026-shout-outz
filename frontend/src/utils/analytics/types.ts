import type { FeedType } from '@/types/feed';

export type AnalyticsEvent =
  // 인증·가입
  | { name: 'login_started'; from: string }
  | { name: 'signup_submitted' }
  | { name: 'signup_failed'; reason: string }
  | {
      name: 'verification_requested';
      userType: string;
      track: string | null;
      cohort: number | null;
    }
  // 피드
  | { name: 'feed_create_started'; from: string; feedType: FeedType }
  | {
      name: 'feed_create_submitted';
      categoryCount: number;
      mediaCount: number;
      feedType: FeedType;
    }
  | { name: 'feed_create_failed'; reason: string; feedType: FeedType }
  | { name: 'feed_detail_opened'; feedId: number; from: FeedSurface }
  | { name: 'feed_type_changed'; feedType: FeedType; surface: FeedSurface }
  | { name: 'feed_sort_changed'; sort: string; surface: FeedSurface; feedType: FeedType }
  | { name: 'feed_like_toggled'; feedId: number; liked: boolean }
  | { name: 'link_preview_clicked'; feedId: number }
  | {
      name: 'comment_submitted';
      isReply: boolean;
      isAnonymous: boolean;
      /** 피드 댓글인지 매거진 댓글인지 */
      target: 'feeds' | 'magazines';
    }
  | { name: 'comment_agree_toggled'; feedId: number; isReply: boolean; agreed: boolean }
  // 추천 질문 퍼널. position은 0부터 센다.
  | {
      name: 'recommendation_impression';
      recommendationId: number;
      surface: RecommendationSurface;
      position: number;
    }
  | {
      name: 'recommendation_clicked';
      recommendationId: number;
      surface: RecommendationSurface;
      position: number;
    }
  | { name: 'recommendation_write_clicked'; recommendationId: number }
  // 프로젝트
  | { name: 'project_create_started'; from: string }
  | {
      name: 'project_create_submitted';
      cohort: number;
      techTagCount: number;
      memberCount: number;
      hasThumbnail: boolean;
      hasDeploymentUrl: boolean;
      serviceStatus: 'OPERATING' | 'CLOSED';
    }
  | { name: 'project_create_failed'; reason: string; invalidFields: string[] }
  | { name: 'project_detail_opened'; slug: string; from: string }
  | { name: 'project_search_performed'; keywordLength: number; resultCount: number }
  | { name: 'project_filter_applied'; cohortCount: number; techTagCount: number }
  | { name: 'project_sort_changed'; sort: string }
  | { name: 'project_like_toggled'; slug: string; liked: boolean }
  // 소식·프로필
  | { name: 'news_filter_changed'; type: string }
  | { name: 'news_detail_opened'; newsId: number; type: string; from: string }
  | { name: 'profile_tab_changed'; tab: string }
  | { name: 'hero_banner_clicked'; bannerId: number; position: number }
  // 알림
  | { name: 'notification_box_opened'; unreadCount: number }
  | { name: 'notification_clicked'; notificationType: string; wasRead: boolean }
  | { name: 'notification_read_all_clicked'; unreadCount: number }
  // 탐색(홈)
  | { name: 'explore_tab_changed'; tab: ExploreTabName; from: ExploreTabName }
  | {
      name: 'explore_search_performed';
      tab: ExploreTabName;
      queryLength: number;
      source: ExploreSearchSource;
    }
  | { name: 'explore_result_filter_changed'; filter: string; queryLength: number }
  | { name: 'project_topic_selected'; topic: string | null }
  // 자료. source는 서버가 주는 자료 출처(블로그·GitHub·테코톡 등)
  | { name: 'material_summary_opened'; resourceId: number; source: string }
  | { name: 'material_source_clicked'; resourceId: number; source: string; from: MaterialSurface }
  | { name: 'material_detail_clicked'; resourceId: number; source: string }
  | { name: 'tech_talk_clicked'; resourceId: number }
  // 채용공고. position은 0부터 센다.
  | { name: 'job_posting_clicked'; jobPostingId: number; position: number }
  | { name: 'job_posting_source_clicked'; jobPostingId: number }
  | {
      name: 'job_requirement_toggled';
      jobPostingId: number;
      requirementId: number;
      /** 역량 칩을 골랐으면 true, 풀었으면 false */
      selected: boolean;
    }
  | { name: 'job_recommendation_filter_changed'; jobPostingId: number; kind: string }
  // 프로젝트 상세
  | { name: 'project_detail_tab_changed'; slug: string; tab: ProjectDetailTabName }
  | { name: 'project_experience_opened'; slug: string; cardId: number }
  | { name: 'project_link_clicked'; slug: string; target: string }
  | { name: 'project_material_job_role_changed'; slug: string; jobRole: string }
  | { name: 'project_material_topic_selected'; slug: string; topicId: string }
  | { name: 'resource_report_opened'; slug: string; kind: ResourceReportKind }
  | { name: 'resource_report_submitted'; slug: string; kind: ResourceReportKind; count: number }
  // 매거진
  | { name: 'magazine_card_clicked'; magazineId: number; featured: boolean }
  | { name: 'magazine_like_toggled'; magazineId: number; liked: boolean }
  // 버그 제보·첫 방문 안내
  | { name: 'bug_report_opened' }
  | { name: 'bug_report_submitted' }
  | { name: 'first_visit_guide_shown' }
  | { name: 'first_visit_guide_explore_clicked' }
  | { name: 'first_visit_guide_dismissed' }
  // 이동 경로
  | { name: 'nav_tab_clicked'; tab: NavTab; from: string }
  | { name: 'section_more_clicked'; target: 'feeds' | 'news' }
  | { name: 'card_clicked'; target: 'feed' | 'project' | 'news'; surface: string };

export type NavTab = 'home' | 'explore' | 'feeds' | 'projects' | 'magazines' | 'news';

/** 탐색 화면(홈)의 탭. 키워드 탭이 기본이다. */
export type ExploreTabName = 'keyword' | 'job' | 'project';

/** 탐색 검색이 시작된 곳. 검색창, 인기 검색어 칩, 키워드 홈의 기술 주제 */
export type ExploreSearchSource = 'search_box' | 'suggestion' | 'topic';

/** 자료 원문을 연 곳. 요약 모달(바텀시트)과 자료 정리 보기 페이지 */
export type MaterialSurface = 'summary' | 'page';

export type ProjectDetailTabName = 'experience' | 'material' | 'intro';

/** addition 빠진 자료 알려 주기, removal 필요 없는 자료 고르기 */
export type ResourceReportKind = 'addition' | 'removal';

/** 피드를 보여주는 화면. 같은 행동이라도 어디서 일어났는지 구분한다. */
export type FeedSurface = 'home' | 'feeds' | 'profile';

/** 추천 질문 카드를 보여주는 화면. 상세의 연관 추천과 질문 목록 사이 슬롯을 구분한다. */
export type RecommendationSurface = 'feed_detail' | 'feed_list';

export interface AnalyticsUser {
  userId: number;
  handle: string;
}

/** 앱이 그때그때 읽어서 넘겨주는 값. */
export interface AnalyticsContextInput {
  path: string;
  viewport: 'mobile' | 'desktop';
}

/**
 * 모든 이벤트에 자동으로 붙는 값. 호출부는 넘기지 않는다.
 *
 * 로그인 여부는 `identify`로 받은 사용자에서 판단하므로 앱이 따로 알려주지 않는다.
 */
export interface AnalyticsContext extends AnalyticsContextInput {
  isLoggedIn: boolean;
}

/** 수집 도구 하나를 감싼 구현. 도구가 바뀌면 이 구현만 갈아끼운다. */
export interface AnalyticsAdapter {
  name: string;
  pageView(path: string, context: AnalyticsContext): void;
  track(event: AnalyticsEvent, context: AnalyticsContext): void;
  identify(user: AnalyticsUser | null): void;
}

/** 화면이 사용하는 유일한 창구. */
export interface AnalyticsClient {
  pageView(path: string): void;
  track(event: AnalyticsEvent): void;
  identify(user: AnalyticsUser | null): void;
}
