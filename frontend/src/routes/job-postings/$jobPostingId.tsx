import { createFileRoute, notFound } from '@tanstack/react-router';

import { JobPostingDetailPage } from '@/pages/JobPostingDetailPage';

// 탐색 화면과 같이 CSR로만 둔다(prerender·ssr을 지정하지 않는다).
export const Route = createFileRoute('/job-postings/$jobPostingId')({
  beforeLoad: ({ params }) => {
    if (!/^[1-9]\d*$/.test(params.jobPostingId)) throw notFound();
  },
  component: JobPostingDetailRoute,
});

function JobPostingDetailRoute() {
  const { jobPostingId } = Route.useParams();
  return <JobPostingDetailPage jobPostingId={Number(jobPostingId)} />;
}
