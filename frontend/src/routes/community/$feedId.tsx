import { createFileRoute } from '@tanstack/react-router';
import { FeedDetailPage } from '@/pages/FeedDetailPage';

export const Route = createFileRoute('/community/$feedId')({
  // 글 등록·수정 직후 공유 링크에 실제 제목과 요약이 OG로 보이도록 요청마다 서버에서 렌더합니다.
  staticData: { ssr: true },
  component: FeedDetailRoute,
});

function FeedDetailRoute() {
  const { feedId } = Route.useParams();
  return <FeedDetailPage feedId={Number(feedId)} />;
}
