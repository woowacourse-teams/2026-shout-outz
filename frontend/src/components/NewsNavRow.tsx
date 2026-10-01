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
      className="group focus-visible:outline-primary-600 flex flex-col gap-1 border-b border-gray-100 py-4 focus-visible:outline-2 md:flex-row md:items-center md:gap-4"
    >
      <span className="text-xs font-medium text-gray-500 md:w-12 md:flex-none">
        {DIRECTION_LABEL[direction]}
      </span>
      <span className="group-hover:text-primary-600 min-w-0 text-sm font-medium break-words text-gray-900">
        {title}
      </span>
      <time dateTime={publishedAt} className="hidden text-xs text-gray-500 md:ml-auto md:block">
        {formatDotDate(publishedAt)}
      </time>
    </Link>
  );
}
