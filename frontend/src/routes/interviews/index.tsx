import { createFileRoute } from '@tanstack/react-router';
import { InterviewsPage } from '@/pages/InterviewsPage';

export const Route = createFileRoute('/interviews/')({
  component: InterviewsPage,
});
