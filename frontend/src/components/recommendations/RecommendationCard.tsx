import { useNavigate, useLocation } from '@tanstack/react-router';
import { IconArrowUpRight, IconSparkles } from '@tabler/icons-react';
import type { QuestionRecommendation } from '@/types/recommendation';
import { toPlainText } from '@/utils/markdown';

export function RecommendationCard({ recommendation }: { recommendation: QuestionRecommendation }) {
  const navigate = useNavigate();
  const from = useLocation({ select: (location) => location.href });

  return (
    <article className="group border-primary-100 bg-primary-50/40 hover:border-primary-300 relative min-w-0 rounded-lg border p-5 transition-colors md:p-6">
      <button
        type="button"
        onClick={() =>
          void navigate({
            to: '/community/recommendations',
            state: { questionRecommendation: { recommendation, from } },
          })
        }
        aria-label={`추천 질문: ${recommendation.title}`}
        className="focus-visible:outline-primary-600 absolute inset-0 z-10 cursor-pointer rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2"
      />
      <div className="mb-3 flex items-center justify-between gap-2 text-xs font-medium">
        <span className="text-primary-600 inline-flex items-center gap-1.5">
          <IconSparkles className="size-4" aria-hidden="true" />
          AI 추천 질문
        </span>
        <span className="text-gray-500">아직 등록되지 않았어요</span>
      </div>
      <h3 className="group-hover:text-primary-600 text-base leading-6 font-semibold break-words text-gray-900">
        <span className="text-primary-600 mr-1.5">Q.</span>
        {recommendation.title}
      </h3>
      <p className="mt-2 line-clamp-2 text-sm leading-6 break-words text-gray-600">
        {toPlainText(recommendation.content)}
      </p>
      <div className="text-primary-600 mt-4 flex items-center gap-1 text-xs font-medium">
        궁금하다면 질문해 보세요 <IconArrowUpRight className="size-3.5" aria-hidden="true" />
      </div>
    </article>
  );
}
