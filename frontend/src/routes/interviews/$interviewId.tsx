import { createFileRoute } from '@tanstack/react-router';
import { InterviewDetailPage } from '@/pages/InterviewDetailPage';

export const Route = createFileRoute('/interviews/$interviewId')({
  component: InterviewDetailRoute,
});

function InterviewDetailRoute() {
  const { interviewId } = Route.useParams();
  return <InterviewDetailPage interviewId={Number(interviewId)} />;
}
