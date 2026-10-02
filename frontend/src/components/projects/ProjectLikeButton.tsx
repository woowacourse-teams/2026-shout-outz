import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { IconHeart, IconHeartFilled } from '@tabler/icons-react';

import { setProjectLike } from '@/apis/reaction';
import { Button } from '@/components/Button';
import { useRequireAuthentication } from '@/hooks/useRequireAuthentication';
import { analytics } from '@/utils/analytics';

export function ProjectLikeButton({
  slug,
  likeCount,
  likedByMe,
}: {
  slug: string;
  likeCount: number;
  likedByMe: boolean;
}) {
  const client = useQueryClient();
  const { requireAuthentication } = useRequireAuthentication();
  const [count, setCount] = useState(likeCount);
  const [liked, setLiked] = useState(likedByMe);
  const mutation = useMutation({
    mutationFn: (active: boolean) => setProjectLike(slug, active),
  });

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
      analytics.track({ name: 'project_like_toggled', slug, liked: result.active });
      void client.invalidateQueries({ queryKey: ['project-detail', slug] });
      void client.invalidateQueries({ queryKey: ['project-list'] });
      void client.invalidateQueries({ queryKey: ['users'] });
    } catch {
      setCount(previous.count);
      setLiked(previous.liked);
    }
  };

  return (
    <div className="flex flex-col items-start">
      <Button
        variant="outline"
        size="lg"
        className={`gap-2 ${liked ? 'border-primary-300 text-primary-600' : ''}`}
        aria-label={liked ? '프로젝트 좋아요 취소' : '프로젝트 좋아요'}
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
    </div>
  );
}
