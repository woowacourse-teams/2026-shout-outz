import type { QuestionRecommendation, RecommendationNavigation } from '@/types/recommendation';

export function pickRecommendations(items: readonly QuestionRecommendation[], count = 2) {
  const pool = [...new Map(items.map((item) => [item.recommendationId, item])).values()];
  for (let index = pool.length - 1; index > 0; index--) {
    const other = Math.floor(Math.random() * (index + 1));
    [pool[index], pool[other]] = [pool[other]!, pool[index]!];
  }
  return pool.slice(0, count);
}

export function readRecommendationNavigation(value: unknown): RecommendationNavigation | undefined {
  if (!value || typeof value !== 'object') return;
  const { recommendation, from } = value as Partial<RecommendationNavigation>;
  if (
    !recommendation ||
    !Number.isSafeInteger(recommendation.recommendationId) ||
    recommendation.recommendationId <= 0 ||
    typeof recommendation.title !== 'string' ||
    !recommendation.title.trim() ||
    typeof recommendation.content !== 'string' ||
    !recommendation.content.trim() ||
    typeof from !== 'string' ||
    !/^\/community(?:\/\d+)?\/?(?:\?[^#]*)?$/.test(from)
  )
    return;
  return { recommendation, from };
}

const LOGIN_DRAFT_KEY = 'shoutoutz:recommendation-login-draft';
const LOGIN_DRAFT_TTL = 60 * 60 * 1000;

/** 외부 로그인으로 문서를 떠나는 순간에만 사용합니다. */
export function saveLoginQuestionDraft(draft: RecommendationNavigation): boolean {
  try {
    sessionStorage.setItem(LOGIN_DRAFT_KEY, JSON.stringify({ draft, savedAt: Date.now() }));
    return true;
  } catch {
    return false;
  }
}

export function readLoginQuestionDraft(): RecommendationNavigation | undefined {
  try {
    const raw = sessionStorage.getItem(LOGIN_DRAFT_KEY);
    if (!raw) return;
    const stored = JSON.parse(raw) as { draft?: unknown; savedAt?: unknown };
    if (typeof stored.savedAt !== 'number' || Date.now() - stored.savedAt > LOGIN_DRAFT_TTL) {
      clearLoginQuestionDraft();
      return;
    }
    const draft = readRecommendationNavigation(stored.draft);
    if (!draft) clearLoginQuestionDraft();
    return draft;
  } catch {
    clearLoginQuestionDraft();
    return;
  }
}

export function clearLoginQuestionDraft() {
  try {
    sessionStorage.removeItem(LOGIN_DRAFT_KEY);
  } catch {
    // 저장소가 차단된 브라우저에서도 일반 글 작성은 계속할 수 있습니다.
  }
}
