import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { IconHeart, IconHeartFilled } from '@tabler/icons-react';

import { setFeedLike } from '@/apis/reaction';
import { Button } from '@/components/Button';
import { getApiErrorMessage } from '@/utils/error';
import { useRequireAuthentication } from '@/hooks/useRequireAuthentication';

export function FeedLikeButton({
  feedId,
  likeCount = 0,
  likedByMe = false,
  label = '좋아요',
}: {
  feedId: number;
  likeCount?: number;
  likedByMe?: boolean;
  label?: string;
}) {
  const client = useQueryClient();
  const { requireAuthentication } = useRequireAuthentication();
  const [reactionOverride, setReactionOverride] = useState<{
    baseCount: number;
    baseLiked: boolean;
    count: number;
    liked: boolean;
  } | null>(null);
  const currentOverride =
    reactionOverride?.baseCount === likeCount && reactionOverride.baseLiked === likedByMe
      ? reactionOverride
      : null;
  const count = currentOverride?.count ?? likeCount;
  const liked = currentOverride?.liked ?? likedByMe;
  const mutation = useMutation({ mutationFn: (active: boolean) => setFeedLike(feedId, active) });

  const toggle = async () => {
    if (!requireAuthentication() || mutation.isPending) return;
    const next = !liked;
    setReactionOverride({
      baseCount: likeCount,
      baseLiked: likedByMe,
      count: Math.max(0, count + (next ? 1 : -1)),
      liked: next,
    });
    try {
      const result = await mutation.mutateAsync(next);
      setReactionOverride({
        baseCount: likeCount,
        baseLiked: likedByMe,
        count: result.likeCount,
        liked: result.active,
      });
      void client.invalidateQueries({ queryKey: ['feed', feedId] });
      void client.invalidateQueries({ queryKey: ['feeds'] });
      void client.invalidateQueries({ queryKey: ['users'] });
    } catch {
      setReactionOverride(null);
    }
  };

  return (
    <div className="flex flex-col items-start">
      <Button
        variant="ghost"
        size="sm"
        className={`gap-1 px-2 ${liked ? 'text-primary-600' : ''}`}
        aria-label={liked ? `${label} 취소` : label}
        aria-pressed={liked}
        disabled={mutation.isPending}
        onClick={() => void toggle()}
      >
        {liked ? (
          <IconHeartFilled className="size-4" aria-hidden="true" />
        ) : (
          <IconHeart className="size-4" aria-hidden="true" />
        )}
        <span>
          {label} <span aria-label={`${label} 수`}>{count}</span>
        </span>
      </Button>
      {mutation.isError && (
        <span role="alert" className="sr-only">
          {getApiErrorMessage(mutation.error)}
        </span>
      )}
    </div>
  );
}
