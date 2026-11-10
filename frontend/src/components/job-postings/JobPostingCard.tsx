import { Link } from '@tanstack/react-router';

import type { JobPostingListItem } from '@/apis/job-posting';
import { CompanyLogo } from '@/components/job-postings/CompanyLogo';
import { deadlineBadge, JOB_ROLE_LABELS } from '@/components/job-postings/jobPostingFormat';

const CARD = 'flex h-full flex-col rounded-2xl border border-gray-200 p-5';
const DEADLINE_BADGE = 'ml-auto shrink-0 rounded-lg bg-gray-100 px-2.5 py-1 text-xs font-bold';

export function JobPostingCard({ jobPosting }: { jobPosting: JobPostingListItem }) {
  return (
    <Link
      to="/job-postings/$jobPostingId"
      params={{ jobPostingId: String(jobPosting.jobPostingId) }}
      className={`${CARD} focus-visible:outline-primary-600 transition-colors hover:bg-gray-50 focus-visible:outline-2`}
    >
      <div className="flex items-center gap-3">
        <CompanyLogo
          companyName={jobPosting.companyName}
          logoUrl={jobPosting.companyLogoUrl}
          className="size-12 rounded-xl"
        />
        <div className="min-w-0">
          <p className="truncate text-sm text-gray-700">{jobPosting.companyName}</p>
          <p className="text-primary-600 mt-0.5 flex flex-wrap gap-x-2 text-xs font-medium">
            <span>{JOB_ROLE_LABELS[jobPosting.jobRole]}</span>
            {jobPosting.employmentType && <span>{jobPosting.employmentType}</span>}
          </p>
        </div>
        <span className={`${DEADLINE_BADGE} text-gray-700`}>
          {deadlineBadge(jobPosting.deadline)}
        </span>
      </div>
      <h2 className="mt-4 text-lg font-bold text-gray-900">{jobPosting.title}</h2>
      {jobPosting.tags.length > 0 && (
        <p className="mt-1.5 flex flex-wrap gap-x-3 text-sm text-gray-500">
          {jobPosting.tags.map((tag) => (
            <span key={tag}>#{tag}</span>
          ))}
        </p>
      )}
      <p className="mt-auto pt-4 text-sm font-semibold text-gray-700">
        찾는 역량 {jobPosting.requirementCount}개 →
      </p>
    </Link>
  );
}

/** 공고가 더 들어올 예정이라는 안내 카드. 검색하지 않은 목록 끝에 항상 둔다. */
export function UpcomingJobPostingCard() {
  return (
    <div aria-hidden="true" className={`${CARD} opacity-50`}>
      <div className="flex items-center gap-3">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-gray-200 text-lg font-bold text-gray-400">
          ?
        </span>
        <div className="min-w-0">
          <p className="truncate text-sm text-gray-500">기업명</p>
          <p className="mt-0.5 text-xs font-medium text-gray-400">신입 | 개발 | 정규직</p>
        </div>
        <span className={`${DEADLINE_BADGE} text-gray-500`}>준비 중</span>
      </div>
      <p className="mt-4 text-lg font-bold text-gray-500">곧 추가될 공고예요</p>
      <p className="mt-1.5 flex flex-wrap gap-x-3 text-sm text-gray-400">
        <span>#키워드</span>
        <span>#키워드</span>
        <span>#키워드</span>
      </p>
      <p className="mt-auto pt-4 text-sm font-semibold text-gray-400">찾는 역량 분석 중</p>
    </div>
  );
}
