import { Link } from '@tanstack/react-router';

import type { JobPostingListItem } from '@/apis/job-posting';
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
      {/* TODO(채용공고 후속 PR): 서버가 회사 로고(companyLogoUrl)를 주면 회사 이름 앞에 48px 로고를 다시 둔다. */}
      <div className="flex items-center gap-3">
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
      {/* TODO(채용공고 후속 PR): 서버가 해시태그(tags)를 주면 제목 아래에 "#Kotlin #Spring"처럼 다시 둔다. */}
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
        <div className="min-w-0">
          <p className="truncate text-sm text-gray-500">기업명</p>
          <p className="mt-0.5 text-xs font-medium text-gray-400">신입 | 개발 | 정규직</p>
        </div>
        <span className={`${DEADLINE_BADGE} text-gray-500`}>준비 중</span>
      </div>
      <p className="mt-4 text-lg font-bold text-gray-500">곧 추가될 공고예요</p>
      <p className="mt-auto pt-4 text-sm font-semibold text-gray-400">찾는 역량 분석 중</p>
    </div>
  );
}
