import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Footer } from '@/components/Footer';
import { JobPostingDetail } from '@/components/job-postings/JobPostingDetail';

export function JobPostingDetailPage({ jobPostingId }: { jobPostingId: number }) {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <AppGnb />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pt-10 pb-24 break-keep">
        <AsyncBoundary>
          <JobPostingDetail key={jobPostingId} jobPostingId={jobPostingId} />
        </AsyncBoundary>
      </main>
      <Footer />
    </div>
  );
}
