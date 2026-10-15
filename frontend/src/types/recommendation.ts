import type { FeedFindSuccessResponse, FeedFindAllSuccessResponse } from '@/api/generated/schema';

export type RelatedQuestionRecommendations = NonNullable<
  FeedFindSuccessResponse['data']['relatedQuestionRecommendations']
>;
export type QuestionRecommendation = RelatedQuestionRecommendations['items'][number];
export type QuestionRecommendationSlot = NonNullable<
  FeedFindAllSuccessResponse['meta']['recommendationSlots']
>[number];

export interface RecommendationNavigation {
  recommendation: QuestionRecommendation;
  from: string;
}

// 미등록 질문의 내용은 URL이나 영구 저장소가 아니라 해당 history entry에만 보관합니다.
declare module '@tanstack/history' {
  interface HistoryState {
    questionRecommendation?: RecommendationNavigation;
    questionDraft?: RecommendationNavigation;
    questionDraftFromPreview?: boolean;
  }
}
