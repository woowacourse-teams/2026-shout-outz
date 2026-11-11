import { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import {
  createFeedMutation,
  FEED_CONTENT_MAX,
  FEED_TITLE_MAX,
  feedQuery,
  updateFeedMutation,
  type Feed,
  type FeedType,
} from '@/apis/feed';
import { myProfileQuery } from '@/apis/user';
import { categoriesQuery } from '@/apis/category';
import { tabItemBaseStyle, tabItemSizeStyles, tabItemStyles } from '@/components/Tab';
import { Button } from '@/components/Button';
import { getApiErrorMessage } from '@/utils/error';
import type { QuestionRecommendation } from '@/types/recommendation';
import { analytics } from '@/utils/analytics';
import { cn } from '@/utils/cn';

const FIELD_CLASS_NAME =
  'focus-visible:border-primary-600 focus-visible:bg-background focus-visible:ring-primary-50 w-full rounded-xl border border-gray-200 bg-gray-50 px-4 py-3.5 text-sm text-gray-900 outline-none placeholder:text-gray-400 focus-visible:ring-3';

interface FeedFormProps {
  userId: number;
  feedType?: FeedType;
  initialFeed?: Feed;
  initialDraft?: Pick<QuestionRecommendation, 'recommendationId' | 'title' | 'content'>;
  onCancel: () => void;
  onSaved: (feedId: number) => void;
}

export function FeedForm({
  userId,
  feedType = 'POST',
  initialFeed,
  initialDraft,
  onCancel,
  onSaved,
}: FeedFormProps) {
  const effectiveType =
    initialFeed?.feedType === 'QUESTION' ? 'QUESTION' : initialFeed ? 'POST' : feedType;
  const { data: profile } = useSuspenseQuery(myProfileQuery(userId));
  const { data: categories } = useSuspenseQuery(categoriesQuery(effectiveType));
  const generalCategories = categories.filter((category) => category.type === 'GENERAL');
  const [categoryId, setCategoryId] = useState<string | null>(() => {
    const category = initialFeed?.categories.find((item) => item.type === 'GENERAL');
    return category ? String(category.categoryId) : null;
  });
  const hasCategory = generalCategories.some(
    (category) => String(category.categoryId) === categoryId,
  );
  const client = useQueryClient();

  const creating = initialFeed === undefined;
  useEffect(() => {
    if (creating) {
      analytics.track({
        name: 'feed_create_started',
        from: document.referrer,
        feedType: effectiveType,
      });
    }
  }, [creating, effectiveType]);

  const mutation = useMutation(
    initialFeed ? updateFeedMutation(initialFeed.feedId) : createFeedMutation,
  );
  const submitting = useRef(false);
  const [title, setTitle] = useState(initialFeed?.title ?? initialDraft?.title ?? '');
  const [content, setContent] = useState(initialFeed?.content ?? initialDraft?.content ?? '');
  const [isAnonymous, setIsAnonymous] = useState(initialFeed?.isAnonymous ?? false);
  // 길이는 코드 포인트로 센다. 서버가 Unicode 기준으로 재므로 이모지·한글이 같은 수로 잡힌다.
  const titleTooLong = Array.from(title).length > FEED_TITLE_MAX;
  const tooLong = Array.from(content).length > FEED_CONTENT_MAX;
  const invalid = !title.trim() || !content.trim() || !hasCategory || titleTooLong || tooLong;

  async function submit() {
    if (invalid || submitting.current) return;
    submitting.current = true;
    // 추천에서 시작한 질문 작성에만 키를 넣는다. 제목·본문을 고쳐도 원래 추천 ID를 보낸다.
    const recommendationId =
      !initialFeed && initialDraft && effectiveType === 'QUESTION'
        ? initialDraft.recommendationId
        : undefined;
    try {
      const feed = await mutation.mutateAsync({
        title: title.trim(),
        content,
        isAnonymous,
        ...(!initialFeed ? { feedType: effectiveType } : {}),
        ...(recommendationId !== undefined ? { recommendationId } : {}),
        categoryIds: [
          Number(categoryId),
          ...(initialFeed?.categories
            .filter((item) => item.type === 'EVENT')
            .map((item) => item.categoryId) ?? []),
        ],
        // 수정은 전체 교체라, 조회 응답이 준 mediaId를 표시 순서대로 다시 실어 기존 첨부를 지킨다.
        mediaIds: [...(initialFeed?.media ?? [])]
          .sort((a, b) => a.displayOrder - b.displayOrder)
          .flatMap((item) => (item.mediaId === undefined ? [] : [item.mediaId])),
      });
      // 작성·수정 응답의 media에는 아직 mediaId가 없다(조회 응답에만 있다). 그 값을 상세 캐시에
      // 그대로 넣으면 바로 이어서 수정할 때 첨부 ID를 잃으므로, 캐시를 비우고 다시 읽게 한다.
      void client.invalidateQueries({ queryKey: feedQuery(feed.feedId).queryKey });
      void client.invalidateQueries({ queryKey: ['feeds'] });
      // 사용한 추천이 남은 상세 캐시를 지운다. 상세의 연관 추천은 처음 그린 값으로 고정되므로,
      // 무효화만 하면 돌아갔을 때 캐시에 남은 추천이 그대로 보인다.
      if (recommendationId !== undefined) client.removeQueries({ queryKey: ['feed'] });
      // TODO 프로필 피드 탭은 ['users', handle, 'feeds']로 따로 캐시된다.
      // api 폴더를 정리할 때 피드 캐시 키를 한 규칙으로 맞추고 이 줄을 없앤다.
      void client.invalidateQueries({ queryKey: ['users'] });
      if (!initialFeed) {
        analytics.track({
          name: 'feed_create_submitted',
          categoryCount: feed.categories.length,
          mediaCount: feed.media.length,
          feedType: effectiveType,
        });
      }
      onSaved(feed.feedId);
    } catch {
      if (!initialFeed) {
        analytics.track({
          name: 'feed_create_failed',
          reason: '요청 실패',
          feedType: effectiveType,
        });
      }
      // Mutation의 오류 상태로 메시지를 표시하고 작성 내용은 유지한다.
    } finally {
      submitting.current = false;
    }
  }

  if (initialFeed && initialFeed.author.handle !== profile.handle) {
    return (
      <p role="alert" className="text-sm text-red-600">
        본인이 작성한 피드만 수정할 수 있습니다.
      </p>
    );
  }

  return (
    <form
      className="flex flex-col gap-6"
      onSubmit={(event) => {
        event.preventDefault();
        void submit();
      }}
    >
      <div className="flex flex-col gap-2">
        {/* 잘못된 분류로 올라가지 않게 처음에는 카테고리를 고르지 않는다. */}
        <p id="feed-category-label" className="text-sm font-bold text-gray-900">
          카테고리
        </p>
        <div
          role="radiogroup"
          aria-labelledby="feed-category-label"
          className="flex flex-wrap gap-2"
        >
          {generalCategories.map((category) => {
            const value = String(category.categoryId);
            const selected = categoryId === value;
            return (
              <button
                key={category.categoryId}
                type="button"
                role="radio"
                aria-checked={selected}
                data-status={selected ? 'active' : 'inactive'}
                onClick={() => setCategoryId(value)}
                disabled={mutation.isPending}
                className={cn(
                  tabItemBaseStyle,
                  tabItemSizeStyles.sm,
                  tabItemStyles.chip,
                  'font-semibold',
                )}
              >
                {category.displayName}
              </button>
            );
          })}
        </div>
        {generalCategories.length === 0 && (
          <p role="status" className="text-sm text-gray-500">
            선택 가능한 카테고리가 없습니다.
          </p>
        )}
      </div>
      <div className="flex flex-col gap-2">
        <label htmlFor="feed-title" className="text-sm font-bold text-gray-900">
          제목
        </label>
        <input
          id="feed-title"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder={
            effectiveType === 'QUESTION'
              ? '궁금한 내용을 한 줄로 적어 주세요'
              : '어떤 이야기인지 한 줄로 알려주세요.'
          }
          disabled={mutation.isPending}
          aria-invalid={titleTooLong || undefined}
          aria-describedby={titleTooLong ? 'feed-title-error' : undefined}
          className={FIELD_CLASS_NAME}
        />
        {titleTooLong && (
          <p id="feed-title-error" role="alert" className="text-sm text-red-600">
            {`제목은 ${FEED_TITLE_MAX}자 이하로 입력해 주세요.`}
          </p>
        )}
      </div>
      <div className="flex flex-col gap-2">
        <label htmlFor="feed-content" className="text-sm font-bold text-gray-900">
          내용
        </label>
        <textarea
          id="feed-content"
          value={content}
          onChange={(event) => setContent(event.target.value)}
          placeholder={
            effectiveType === 'QUESTION'
              ? '질문의 배경과 궁금한 점을 구체적으로 적어 주세요'
              : '크루들과 나누고 싶은 기술 이야기나 경험을 적어보세요.'
          }
          disabled={mutation.isPending}
          aria-invalid={tooLong || undefined}
          aria-describedby={tooLong ? 'feed-content-error' : undefined}
          className={cn(FIELD_CLASS_NAME, 'h-55 resize-y leading-6')}
        />
        {tooLong && (
          <p id="feed-content-error" role="alert" className="text-sm text-red-600">
            {`본문은 ${FEED_CONTENT_MAX}자 이하로 입력해 주세요.`}
          </p>
        )}
      </div>
      {mutation.isError && (
        <p role="alert" className="text-sm text-red-600">
          {getApiErrorMessage(mutation.error)}
        </p>
      )}
      <div className="flex flex-wrap items-center gap-2">
        <label className="mr-auto flex cursor-pointer items-center gap-2 text-sm text-gray-600">
          <input
            type="checkbox"
            checked={isAnonymous}
            onChange={(event) => setIsAnonymous(event.target.checked)}
            disabled={mutation.isPending}
            className="accent-primary-600 size-4"
          />
          익명으로 쓰기
        </label>
        <Button
          variant="ghost"
          className="h-11 px-5 font-semibold text-gray-500"
          onClick={onCancel}
          disabled={mutation.isPending}
        >
          취소
        </Button>
        <Button
          type="submit"
          className="h-11 bg-gray-900 px-6 font-semibold text-white hover:bg-gray-700 disabled:hover:bg-gray-900"
          disabled={invalid || mutation.isPending}
        >
          {mutation.isPending ? '저장 중…' : initialFeed ? '수정 완료' : '등록하기'}
        </Button>
      </div>
    </form>
  );
}
