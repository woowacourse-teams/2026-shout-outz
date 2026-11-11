import { createFileRoute } from '@tanstack/react-router';

import { ExperienceDetailPage } from '@/pages/ExperienceDetailPage';
import { toProjectSlug } from '@/utils/project';

// 경력 카드는 프로젝트 경력 카드 목록 응답에서 꺼내 쓰므로, 따로 미리 만들지 않고 브라우저 렌더로만 그린다.
export const Route = createFileRoute('/projects/$slug_/experiences/$cardId')({
  component: ExperienceDetailRoute,
});

function ExperienceDetailRoute() {
  const { slug, cardId } = Route.useParams();
  return <ExperienceDetailPage slug={toProjectSlug(slug)} cardId={Number(cardId)} />;
}
