import { useSuspenseQuery } from '@tanstack/react-query';
import { useState } from 'react';

import { jobPostingQuery } from '@/apis/job-posting';
import { deadlineText } from '@/components/job-postings/jobPostingFormat';
import { RequirementRecommendations } from '@/components/job-postings/RequirementRecommendations';
import { analytics } from '@/utils/analytics';
import { cn } from '@/utils/cn';

/** 채용공고 상세(문서형). 공고 정보, 찾는 역량, 역량별 추천 자료 */
export function JobPostingDetail({ jobPostingId }: { jobPostingId: number }) {
  const { data: jobPosting } = useSuspenseQuery(jobPostingQuery(jobPostingId));
  // 처음에는 공고의 첫 번째 역량을 골라 둔다.
  const [selectedIds, setSelectedIds] = useState<number[]>(() =>
    jobPosting.requirements.slice(0, 1).map((requirement) => requirement.requirementId),
  );
  const selected = jobPosting.requirements.filter((requirement) =>
    selectedIds.includes(requirement.requirementId),
  );

  const toggle = (requirementId: number) =>
    setSelectedIds((ids) =>
      ids.includes(requirementId)
        ? ids.filter((id) => id !== requirementId)
        : [...ids, requirementId],
    );

  return (
    <>
      <title>{`${jobPosting.title} · ${jobPosting.companyName} | shout-outz`}</title>
      {/* 오른쪽 "연관 질문" 칸은 없앴다. 본문이 그 자리까지 넓게 쓴다. */}
      <div className="min-w-0">
        <section className="flex flex-col gap-2.5">
          {/* TODO(채용공고 후속 PR): 서버가 회사 로고(companyLogoUrl)를 주면 제목 앞에 로고(모바일 80px, 데스크톱 112px)를 다시 둔다. */}
          <div className="flex items-center gap-5 md:gap-7">
            <div className="flex min-w-0 flex-col gap-2">
              <h1 className="text-2xl leading-tight font-extrabold tracking-tight md:text-3xl">
                {jobPosting.title}
              </h1>
              <p className="text-base text-gray-600">
                {jobPosting.companyName} · {deadlineText(jobPosting.deadline)}
              </p>
              <a
                href={jobPosting.sourceUrl}
                onClick={() =>
                  analytics.track({ name: 'job_posting_source_clicked', jobPostingId })
                }
                target="_blank"
                rel="noopener noreferrer"
                className="mt-0.5 self-start text-sm text-gray-600 underline decoration-gray-200 underline-offset-3 hover:text-gray-900"
              >
                공고 원문 보기 ↗
              </a>
            </div>
          </div>

          <div className="mt-7 flex flex-col gap-3.5 border-y border-gray-200 py-6">
            <p className="text-xs font-semibold text-gray-500">
              이 공고가 찾는 역량{' '}
              <span className="font-normal text-gray-400">· 중복 선택 가능</span>
            </p>
            <div
              role="group"
              aria-label="이 공고가 찾는 역량"
              className="flex flex-wrap gap-x-1.5 gap-y-1"
            >
              {jobPosting.requirements.map((requirement) => {
                const pressed = selectedIds.includes(requirement.requirementId);
                return (
                  <button
                    key={requirement.requirementId}
                    type="button"
                    aria-pressed={pressed}
                    onClick={() => {
                      analytics.track({
                        name: 'job_requirement_toggled',
                        jobPostingId,
                        requirementId: requirement.requirementId,
                        selected: !pressed,
                      });
                      toggle(requirement.requirementId);
                    }}
                    className={cn(
                      'focus-visible:outline-primary-600 cursor-pointer rounded-full px-3 py-1 text-sm transition-colors focus-visible:outline-2',
                      pressed
                        ? 'bg-primary-50 text-primary-600 font-semibold'
                        : 'font-medium text-gray-500 hover:bg-gray-50',
                    )}
                  >
                    {requirement.name}
                  </button>
                );
              })}
            </div>
          </div>
        </section>

        <div className="pt-10">
          <RequirementRecommendations
            jobPostingId={jobPosting.jobPostingId}
            requirements={selected}
          />
        </div>
      </div>
    </>
  );
}
