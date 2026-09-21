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
  | { name: 'feed_create_started'; from: string }
  | { name: 'feed_create_submitted'; categoryCount: number; mediaCount: number }
  | { name: 'feed_create_failed'; reason: string }
  | { name: 'feed_detail_opened'; feedId: number; from: FeedSurface }
  | { name: 'feed_sort_changed'; sort: string; surface: FeedSurface }
  | { name: 'comment_submitted' }
  // 프로젝트
  | { name: 'project_create_started'; from: string }
  | {
      name: 'project_create_submitted';
      cohort: number;
      techTagCount: number;
      memberCount: number;
      hasThumbnail: boolean;
      hasDeploymentUrl: boolean;
    }
  | { name: 'project_create_failed'; reason: string; invalidFields: string[] }
  | { name: 'project_detail_opened'; projectId: number; from: string }
  | { name: 'project_search_performed'; keywordLength: number; resultCount: number }
  // 소식·프로필
  | { name: 'news_filter_changed'; type: string }
  | { name: 'news_detail_opened'; newsId: number; type: string; from: string }
  | { name: 'profile_tab_changed'; tab: string }
  | { name: 'hero_banner_clicked' }
  // 이동 경로
  | { name: 'nav_tab_clicked'; tab: NavTab; from: string }
  | { name: 'section_more_clicked'; target: 'feeds' | 'news' }
  | { name: 'card_clicked'; target: 'feed' | 'project' | 'news'; surface: string };

export type NavTab = 'home' | 'feeds' | 'projects' | 'news';

/** 피드를 보여주는 화면. 같은 행동이라도 어디서 일어났는지 구분한다. */
export type FeedSurface = 'home' | 'feeds' | 'profile';

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
