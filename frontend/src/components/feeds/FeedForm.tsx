import { useRef, useState } from 'react';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { createFeedMutation, updateFeedMutation, feedQuery, type Feed } from '@/apis/feed';
import { myProfileQuery } from '@/apis/user';
import { categoriesQuery } from '@/apis/category';
import { Select } from '@/components/Select';
import { Avatar } from '@/components/Avatar';
import { Button } from '@/components/Button';
import { formatCrewName } from '@/utils/user';
import { getApiErrorMessage } from '@/utils/error';
import { analytics } from '@/utils/analytics';

interface FeedFormProps {
  userId: number;
  initialFeed?: Feed;
  onCancel: () => void;
  onSaved: (feedId: number) => void;
}

export function FeedForm({ userId, initialFeed, onCancel, onSaved }: FeedFormProps) {
  const { data: profile } = useSuspenseQuery(myProfileQuery(userId));
  const { data: categories } = useSuspenseQuery(categoriesQuery);
  const generalCategories = categories.filter((category) => category.type === 'GENERAL');
  const [categoryId, setCategoryId] = useState<string | null>(() => {
    const category = initialFeed?.categories.find((item) => item.type === 'GENERAL');
    return category ? String(category.categoryId) : null;
  });
  const hasCategory = generalCategories.some(
    (category) => String(category.categoryId) === categoryId,
  );
  const client = useQueryClient();
  const mutation = useMutation(
    initialFeed ? updateFeedMutation(initialFeed.feedId) : createFeedMutation,
  );
  const submitting = useRef(false);
  const [content, setContent] = useState(initialFeed?.content ?? '');
  const tooLong = Array.from(content).length > 500;

  async function submit() {
    if (!content.trim() || !hasCategory || tooLong || submitting.current) return;
    submitting.current = true;
    try {
      const feed = await mutation.mutateAsync({
        content,
        categoryIds: [
          Number(categoryId),
          ...(initialFeed?.categories
            .filter((item) => item.type === 'EVENT')
            .map((item) => item.categoryId) ?? []),
        ],
        mediaIds: [...(initialFeed?.media ?? [])]
          .sort((a, b) => a.displayOrder - b.displayOrder)
          .flatMap((item) => (item.mediaId === undefined ? [] : [item.mediaId])),
      });
      client.setQueryData(feedQuery(feed.feedId).queryKey, feed);
      void client.invalidateQueries({ queryKey: ['feeds'] });
      // TODO 프로필 피드 탭은 ['users', handle, 'feeds']로 따로 캐시된다.
      // api 폴더를 정리할 때 피드 캐시 키를 한 규칙으로 맞추고 이 줄을 없앤다.
      void client.invalidateQueries({ queryKey: ['users'] });
      if (!initialFeed) {
        analytics.track({
          name: 'feed_create_submitted',
          categoryCount: feed.categories.length,
          mediaCount: feed.media.length,
        });
      }
      onSaved(feed.feedId);
    } catch {
      if (!initialFeed) {
        analytics.track({ name: 'feed_create_failed', reason: '요청 실패' });
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
      className="flex flex-col gap-4 md:gap-7"
      onSubmit={(event) => {
        event.preventDefault();
        void submit();
      }}
    >
      <div className="flex items-center gap-2 md:gap-3">
        <Avatar size="sm" alt="" />
        <p className="text-sm font-semibold text-gray-900">
          {formatCrewName(profile.displayName, profile.cohort, profile.track)}
        </p>
      </div>
      <div className="space-y-2">
        <label htmlFor="feed-category" className="text-sm font-medium text-gray-900">
          카테고리
        </label>
        <Select
          id="feed-category"
          aria-label="카테고리"
          placeholder="카테고리를 선택해 주세요"
          value={categoryId}
          onValueChange={setCategoryId}
          disabled={mutation.isPending || generalCategories.length === 0}
        >
          {generalCategories.map((category) => (
            <Select.Item key={category.categoryId} value={String(category.categoryId)}>
              {category.displayName}
            </Select.Item>
          ))}
        </Select>
        {generalCategories.length === 0 && (
          <p role="status" className="text-sm text-gray-500">
            선택 가능한 카테고리가 없습니다.
          </p>
        )}
      </div>
      <div className="focus-within:ring-primary-600 rounded-xl border border-gray-200 p-4 focus-within:ring-2 md:p-5">
        <label htmlFor="feed-content" className="sr-only">
          피드 내용
        </label>
        <textarea
          id="feed-content"
          value={content}
          onChange={(event) => setContent(event.target.value)}
          placeholder="크루들과 나누고 싶은 기술 이야기나 경험을 적어보세요."
          rows={8}
          disabled={mutation.isPending}
          aria-invalid={tooLong || undefined}
          aria-describedby={tooLong ? 'feed-content-error' : undefined}
          className="block w-full resize-y bg-transparent text-sm leading-6 text-gray-900 outline-none placeholder:text-gray-500"
        />
      </div>
      {tooLong && (
        <p id="feed-content-error" role="alert" className="text-sm text-red-600">
          본문은 500자 이하로 입력해 주세요.
        </p>
      )}
      {mutation.isError && (
        <p role="alert" className="text-sm text-red-600">
          {getApiErrorMessage(mutation.error)}
        </p>
      )}
      <div className="flex gap-2 md:gap-3 md:pt-2">
        <Button variant="outline" onClick={onCancel} disabled={mutation.isPending}>
          취소
        </Button>
        <Button
          type="submit"
          className="flex-1 md:flex-none"
          disabled={!content.trim() || !hasCategory || tooLong || mutation.isPending}
        >
          {mutation.isPending ? '저장 중…' : initialFeed ? '수정 완료' : '피드 등록하기'}
        </Button>
      </div>
    </form>
  );
}
