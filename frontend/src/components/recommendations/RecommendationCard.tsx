import { useEffect, useId, useRef } from 'react';
import { useNavigate, useLocation } from '@tanstack/react-router';
import { IconArrowUpRight, IconSparkles } from '@tabler/icons-react';
import type { QuestionRecommendation } from '@/types/recommendation';
import { toPlainText } from '@/utils/markdown';
import { formatFeedCardPreview } from '@/utils/feed';
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

  const previewId = useId();
  const preview = toPlainText(recommendation.content);
  const spokenPreview = formatFeedCardPreview(recommendation);
  const open = () => {
    analytics.track({ name: 'recommendation_clicked', recommendationId, surface, position });
    void navigate({
      to: '/community/recommendations',
      state: { questionRecommendation: { recommendation, from } },
    });
  };

  return (
    <article
      ref={card}
      className={cn(
        'group relative flex min-w-0 flex-col border transition-colors',
        isDetail
          ? 'bg-background gap-2.5 rounded-xl border-gray-200 p-5 hover:border-gray-300'
          : 'border-primary-100 bg-primary-50/40 hover:border-primary-300 rounded-lg p-5 md:p-6',
        className,
      )}
    >
      {/*
        커뮤니티 카드와 같이, 스크린리더에서는 카드 하나가 제목 겸 버튼 하나로 읽힌다.
        버튼 이름에 AI 추천 질문이라는 구분을 함께 담고, 본문은 설명으로 연결한다.
        버튼은 제목 안에 두되 가상 요소로 카드 전체를 덮어, 카드 어디를 눌러도 미리보기로 이동한다.
      */}
      <h3
        className={cn(
          'group-hover:text-primary-600 order-2 break-words text-gray-900',
          isDetail ? 'text-base font-bold' : 'text-base leading-6 font-semibold',
        )}
      >
        <button
          type="button"
          onClick={open}
          aria-label={`${recommendation.title}, AI 추천 질문, 아직 등록되지 않았어요`}
          aria-describedby={spokenPreview ? previewId : undefined}
          className={cn(
            'focus-visible:after:outline-primary-600 cursor-pointer text-left outline-none after:absolute after:inset-0 after:z-10 focus-visible:after:outline-2 focus-visible:after:outline-offset-2',
            isDetail ? 'after:rounded-xl' : 'after:rounded-lg',
          )}
        >
          <span aria-hidden="true" className="text-primary-600 mr-1.5">
            Q.
          </span>
          {recommendation.title}
        </button>
      </h3>
      {spokenPreview && (
        <span id={previewId} hidden>
          {spokenPreview}
        </span>
      )}
      {/* 아래 정보는 버튼 이름과 설명으로 함께 읽으므로, 같은 내용을 다시 읽지 않게 숨긴다. */}
      {isDetail ? (
        <>
          <div
            aria-hidden="true"
            className="order-1 flex items-center justify-between gap-2 text-xs"
          >
            <span className="bg-primary-50 text-primary-600 rounded-sm px-2 py-0.5 font-bold">
              AI 추천 질문
            </span>
            <span className="text-gray-400">아직 등록되지 않았어요</span>
          </div>
          <p
            aria-hidden="true"
            className="order-3 line-clamp-3 text-sm leading-relaxed break-words text-gray-600"
          >
            {preview}
          </p>
        </>
      ) : (
        <>
          <div
            aria-hidden="true"
            className="order-1 mb-3 flex items-center justify-between gap-2 text-xs font-medium"
          >
            <span className="text-primary-600 inline-flex items-center gap-1.5">
              <IconSparkles className="size-4" aria-hidden="true" />
              AI 추천 질문
            </span>
            <span className="text-gray-500">아직 등록되지 않았어요</span>
          </div>
          <p
            aria-hidden="true"
            className="order-3 mt-2 line-clamp-2 text-sm leading-6 break-words text-gray-600"
          >
            {preview}
          </p>
          <div
            aria-hidden="true"
            className="text-primary-600 order-4 mt-4 flex items-center gap-1 text-xs font-medium"
          >
            궁금하다면 질문해 보세요 <IconArrowUpRight className="size-3.5" aria-hidden="true" />
          </div>
        </>
      )}
    </article>
  );
}
