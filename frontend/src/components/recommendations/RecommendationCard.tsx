import { useEffect, useRef } from 'react';
import { useNavigate, useLocation } from '@tanstack/react-router';
import { IconArrowUpRight, IconSparkles } from '@tabler/icons-react';
import type { QuestionRecommendation } from '@/types/recommendation';
import { toPlainText } from '@/utils/markdown';
import { cn } from '@/utils/cn';
import { analytics, type RecommendationSurface } from '@/utils/analytics';

/** 카드가 이 비율 이상 화면에 보이면 노출로 센다. */
const IMPRESSION_RATIO = 0.5;

interface RecommendationCardProps {
  recommendation: QuestionRecommendation;
  /** 카드가 놓인 화면. */
  surface: RecommendationSurface;
  /** 같은 화면의 추천 카드 중 몇 번째인지. 0부터 센다. */
  position: number;
  /** list: 커뮤니티 목록 사이의 파란 카드. detail: 글 상세 관련 질문의 흰 카드. */
  variant?: 'list' | 'detail';
  className?: string;
}

export function RecommendationCard({
  recommendation,
  surface,
  position,
  variant = 'list',
  className,
}: RecommendationCardProps) {
  const isDetail = variant === 'detail';
  const navigate = useNavigate();
  const from = useLocation({ select: (location) => location.href });
  const { recommendationId } = recommendation;
  const card = useRef<HTMLElement>(null);
  // 카드 하나당 노출은 한 번만 보낸다. 리렌더나 스크롤 왕복으로 다시 보내지 않는다.
  const impressed = useRef(false);

  useEffect(() => {
    const element = card.current;
    if (!element || impressed.current || typeof IntersectionObserver === 'undefined') return;
    // 카드가 절반 이상 보여야 노출로 센다. observe 직후의 첫 콜백은 threshold와 상관없이 오고
    // 그때 isIntersecting은 1px만 보여도 true라서, 보이는 비율로 판단한다.
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry || entry.intersectionRatio < IMPRESSION_RATIO || impressed.current) return;
        impressed.current = true;
        observer.disconnect();
        analytics.track({ name: 'recommendation_impression', recommendationId, surface, position });
      },
      { threshold: IMPRESSION_RATIO },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, [recommendationId, surface, position]);

  return (
    <article
      ref={card}
      className={cn(
        'group relative min-w-0 border transition-colors',
        isDetail
          ? 'bg-background flex flex-col gap-2.5 rounded-xl border-gray-200 p-5 hover:border-gray-300'
          : 'border-primary-100 bg-primary-50/40 hover:border-primary-300 rounded-lg p-5 md:p-6',
        className,
      )}
    >
      <button
        type="button"
        onClick={() => {
          analytics.track({ name: 'recommendation_clicked', recommendationId, surface, position });
          void navigate({
            to: '/community/recommendations',
            state: { questionRecommendation: { recommendation, from } },
          });
        }}
        aria-label={`추천 질문: ${recommendation.title}`}
        className={cn(
          'focus-visible:outline-primary-600 absolute inset-0 z-10 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2',
          isDetail ? 'rounded-xl' : 'rounded-lg',
        )}
      />
      {isDetail ? (
        <>
          <div className="flex items-center justify-between gap-2 text-xs">
            <span className="bg-primary-50 text-primary-600 rounded-sm px-2 py-0.5 font-bold">
              AI 추천 질문
            </span>
            <span className="text-gray-400">아직 등록되지 않았어요</span>
          </div>
          <h3 className="group-hover:text-primary-600 text-base font-bold break-words text-gray-900">
            <span className="text-primary-600 mr-1.5">Q.</span>
            {recommendation.title}
          </h3>
          <p className="line-clamp-3 text-sm leading-relaxed break-words text-gray-600">
            {toPlainText(recommendation.content)}
          </p>
        </>
      ) : (
        <>
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
        </>
      )}
    </article>
  );
}
