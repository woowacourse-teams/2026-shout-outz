import { useState } from 'react';
import { RecommendationCard } from '@/components/recommendations/RecommendationCard';
import type { QuestionRecommendation } from '@/types/recommendation';
import { pickRecommendations } from '@/utils/recommendation';

/** 상세를 읽는 동안 리렌더나 백그라운드 재조회로 추천이 바뀌지 않게 합니다. */
export function RelatedQuestions({ items }: { items: QuestionRecommendation[] }) {
  const [selected] = useState(() => pickRecommendations(items));
  if (selected.length === 0) return null;
  return (
    <section aria-label="연관 추천 질문" className="mt-10 border-t border-gray-100 pt-7">
      <h2 className="text-base font-bold text-gray-900">이런 질문도 궁금한가요?</h2>
      <p className="mt-1 text-sm text-gray-500">
        AI가 제안한 질문을 살펴보고, 함께 이야기해 보세요.
      </p>
      <div className="mt-4 grid gap-3 md:grid-cols-2">
        {selected.map((item) => (
          <RecommendationCard key={item.recommendationId} recommendation={item} />
        ))}
      </div>
    </section>
  );
}
