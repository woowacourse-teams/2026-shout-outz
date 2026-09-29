import { useEffect, useState } from 'react';
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
}: {
  feedId: number;
  likeCount?: number;
  likedByMe?: boolean;
}) {
  const client = useQueryClient();
  const { requireAuthentication } = useRequireAuthentication();
  const [count, setCount] = useState(likeCount);
  const [liked, setLiked] = useState(likedByMe);
  const mutation = useMutation({ mutationFn: (active: boolean) => setFeedLike(feedId, active) });

  useEffect(() => {
    setCount(likeCount);
    setLiked(likedByMe);
  }, [likeCount, likedByMe]);

  const toggle = async () => {
    if (!requireAuthentication() || mutation.isPending) return;
    const previous = { count, liked };
    const next = !liked;
    setLiked(next);
    setCount(Math.max(0, count + (next ? 1 : -1)));
    try {
      const result = await mutation.mutateAsync(next);
      setCount(result.likeCount);
      setLiked(result.active);
      void client.invalidateQueries({ queryKey: ['feed', feedId] });
      void client.invalidateQueries({ queryKey: ['feeds'] });
      void client.invalidateQueries({ queryKey: ['users'] });
    } catch {
      setCount(previous.count);
      setLiked(previous.liked);
    }
  };

  return (
    <div className="flex flex-col items-start">
      <Button
        variant="ghost"
        size="sm"
        className={`gap-1 px-2 ${liked ? 'text-primary-600' : ''}`}
        aria-label={liked ? '좋아요 취소' : '좋아요'}
        aria-pressed={liked}
        disabled={mutation.isPending}
        onClick={() => void toggle()}
      >
        {liked ? (
          <IconHeartFilled className="size-4" aria-hidden="true" />
        ) : (
          <IconHeart className="size-4" aria-hidden="true" />
        )}
        <span aria-label="좋아요 수">{count}</span>
      </Button>
      {mutation.isError && (
        <span role="alert" className="sr-only">
          {getApiErrorMessage(mutation.error)}
        </span>
      )}
    </div>
  );
}
