const TRACK_LABELS: Record<string, string> = {
  ANDROID: '안드로이드',
  BACKEND: '백엔드',
  FRONTEND: '프론트엔드',
};

export function formatUserAffiliation({
  userType,
  cohort,
  track,
  isCurrent,
  anonymous = false,
}: {
  userType?: string | null;
  cohort?: number | null;
  track?: string | null;
  isCurrent?: boolean | null;
  anonymous?: boolean;
}): string | null {
  if (userType === 'WOOWACOURSE_COACH') return '우아한테크코스 코치';
  if (userType !== 'WOOWACOURSE_CREW') return null;
  if (!anonymous && cohort != null) {
    return ['우아한테크코스', `${cohort}기`, formatTrackLabel(track), '크루']
      .filter(Boolean)
      .join(' ');
  }
  return isCurrent === false ? '우아한테크코스 수료생' : '우아한테크코스 크루';
}

export function formatTrackLabel(track: string | null | undefined): string | null {
  return track == null ? null : (TRACK_LABELS[track] ?? null);
}

/**
 * 크루의 소속 표시 문구.
 *
 * - 기수와 트랙을 모두 알면: "6기 백엔드"
 * - 아는 것만 있으면 그것만: "6기", "백엔드"
 * - 둘 다 없거나 한글 표기를 모르는 트랙뿐이면: null (표시하지 않는다)
 *
 * 프로필 배지와 피드 작성자 줄이 같은 규칙을 쓴다.
 */
export function formatCrewRole(
  cohort: number | null | undefined,
  track: string | null | undefined,
): string | null {
  const parts = [cohort == null ? null : `${cohort}기`, formatTrackLabel(track)].filter(Boolean);

  return parts.length === 0 ? null : parts.join(' ');
}

/** 이름과 소속을 이어 붙인다. 소속을 알 수 없으면 이름만 남는다. */
export function formatCrewName(
  displayName: string,
  cohort: number | null | undefined,
  track: string | null | undefined,
): string {
  return [displayName, formatCrewRole(cohort, track)].filter(Boolean).join(' · ');
}

/** 한 줄 소개와 GitHub·블로그 주소. 입력칸 값 그대로라 빈 문자열일 수 있다. */
export interface ProfileIntroInput {
  bio: string;
  githubProfileUrl: string;
  blogUrl: string;
}

export type ProfileIntroErrors = Partial<Record<keyof ProfileIntroInput, string>>;

// 서버 `UserProfileUpdateRequest`·`OAuthSignupRequest`의 `@Pattern`과 같은 규칙.
const GITHUB_PROFILE_URL = /^https:\/\/github\.com\/[^/\s?#]+\/?$/;
const BLOG_URL = /^https?:\/\/[^\s/?#:]+(?::\d{1,5})?(?:[/?#][^\s]*)?$/;

/** 프로필 수정과 회원가입이 함께 쓰는 소개·주소 검사. 비어 있으면 통과한다. */
export function validateProfileIntro({
  bio,
  githubProfileUrl,
  blogUrl,
}: ProfileIntroInput): ProfileIntroErrors {
  const errors: ProfileIntroErrors = {};
  if (Array.from(bio.trim()).length > 200) {
    errors.bio = '한 줄 소개는 200자 이하로 입력해 주세요.';
  }
  if (githubProfileUrl.trim() && !GITHUB_PROFILE_URL.test(githubProfileUrl.trim())) {
    errors.githubProfileUrl = 'https://github.com/아이디 형식으로 입력해 주세요.';
  }
  if (blogUrl.trim() && !BLOG_URL.test(blogUrl.trim())) {
    errors.blogUrl = 'http:// 또는 https://로 시작하는 주소를 입력해 주세요.';
  }
  return errors;
}

const toNullable = (value: string) => value.trim() || null;

/** 요청 본문으로 바꾼다. 비운 칸은 null로 보내 지운다. */
export function toProfileIntroBody({ bio, githubProfileUrl, blogUrl }: ProfileIntroInput) {
  return {
    bio: toNullable(bio),
    githubProfileUrl: toNullable(githubProfileUrl),
    blogUrl: toNullable(blogUrl),
  };
}
