import { createFileRoute } from '@tanstack/react-router';
import { feedQuery } from '@/apis/feed';
import { FeedDetailPage } from '@/pages/FeedDetailPage';

export const Route = createFileRoute('/community/$feedId')({
  // 글 등록·수정 직후 공유 링크에 실제 제목과 요약이 OG로 보이도록 요청마다 서버에서 렌더합니다.
  // 서버 렌더 전에 글 상세를 받아 두어 셸과 $_TSR에 글이 실리고, 없는 글은 404로 응답합니다.
  staticData: {
    ssr: true,
    ssrQueries: ({ feedId }) => [feedQuery(Number(feedId))],
  },
  component: FeedDetailRoute,
});

function FeedDetailRoute() {
  const { feedId } = Route.useParams();
  return <FeedDetailPage feedId={Number(feedId)} />;
}
