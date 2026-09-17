import { useSuspenseQuery } from '@tanstack/react-query';

import { mediaQuery } from '@/apis/media';
import { Image } from '@/components/Image';

interface FeedMediaProps {
  mediaId: number;
}

export function FeedMedia({ mediaId }: FeedMediaProps) {
  const { data } = useSuspenseQuery(mediaQuery(mediaId));

  return (
    <Image
      src={data.downloadUrl}
      alt="피드 첨부 이미지"
      loading="lazy"
      className="max-h-96 w-full rounded-xl bg-gray-50 object-contain"
      fallback={<p className="text-sm text-gray-500">이미지를 불러오지 못했습니다.</p>}
    />
  );
}
