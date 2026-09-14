import { Link } from '@tanstack/react-router';
import { formatDotDate } from '@/utils/date';

type NewsNavDirection = 'previous' | 'next';

const DIRECTION_LABEL: Record<NewsNavDirection, string> = {
  previous: '이전글',
  next: '다음글',
};

interface NewsNavRowProps {
  direction: NewsNavDirection;
  id: number;
  title: string;
  publishedAt: string;
}

export function NewsNavRow({ direction, id, title, publishedAt }: NewsNavRowProps) {
  return (
    <Link
      to="/news/$newsId"
      params={{ newsId: String(id) }}
      className="flex flex-col gap-0.5 border-b border-gray-100 py-3 last:border-b-0 md:flex-row md:items-center md:gap-3 md:py-3.5"
    >
      <span className="text-xs font-bold text-gray-500 md:w-12 md:flex-none">
        {DIRECTION_LABEL[direction]}
      </span>
      <span className="text-sm text-gray-900">{title}</span>
      <time dateTime={publishedAt} className="hidden text-xs text-gray-500 md:ml-auto md:block">
        {formatDotDate(publishedAt)}
      </time>
    </Link>
  );
}
