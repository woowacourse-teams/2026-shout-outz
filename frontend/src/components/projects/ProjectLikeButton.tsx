import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { IconHeart, IconHeartFilled } from '@tabler/icons-react';

import { setProjectLike } from '@/apis/reaction';
import { cn } from '@/utils/cn';
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

  // 상세를 다시 불러와 값이 바뀌면 화면 값도 맞춘다.
  const [synced, setSynced] = useState({ likeCount, likedByMe });
  if (synced.likeCount !== likeCount || synced.likedByMe !== likedByMe) {
    setSynced({ likeCount, likedByMe });
    setCount(likeCount);
    setLiked(likedByMe);
  }

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
    <button
      type="button"
      className={cn(
        'focus-visible:outline-primary-600 -mr-2 inline-flex shrink-0 cursor-pointer items-center gap-1.5 rounded-lg p-2 text-sm font-semibold tabular-nums focus-visible:outline-2 disabled:cursor-default',
        liked ? 'text-primary-600' : 'text-gray-500 hover:text-gray-900',
      )}
      aria-label={liked ? '프로젝트 좋아요 취소' : '프로젝트 좋아요'}
      aria-pressed={liked}
      disabled={mutation.isPending}
      onClick={() => void toggle()}
    >
      {liked ? (
        <IconHeartFilled className="size-6" aria-hidden="true" />
      ) : (
        <IconHeart className="size-6" aria-hidden="true" />
      )}
      <span aria-label="좋아요 수">{count}</span>
    </button>
  );
}
