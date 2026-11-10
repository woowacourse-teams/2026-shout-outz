import { useSuspenseQuery } from '@tanstack/react-query';
import { useState } from 'react';

import { jobPostingQuery } from '@/apis/job-posting';
import { CommunityFeedPanel } from '@/components/job-postings/CommunityFeedPanel';
import { CompanyLogo } from '@/components/job-postings/CompanyLogo';
import { deadlineText } from '@/components/job-postings/jobPostingFormat';
import { RequirementRecommendations } from '@/components/job-postings/RequirementRecommendations';
import { cn } from '@/utils/cn';

/** 채용공고 상세(문서형). 공고 정보, 찾는 역량, 역량별 추천 자료, 커뮤니티 고민 */
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
      {/* 데스크톱은 오른쪽 한 칸에 커뮤니티 고민을 고정하고, 모바일은 자료 아래로 내린다. */}
      <div className="grid gap-x-12 lg:grid-cols-4">
        <div className="min-w-0 lg:col-span-3">
          <section className="flex flex-col gap-2.5">
            <div className="flex items-center gap-5 md:gap-7">
              <CompanyLogo
                companyName={jobPosting.companyName}
                logoUrl={jobPosting.companyLogoUrl}
                className="size-20 bg-gray-100 md:size-28"
              />
              <div className="flex min-w-0 flex-col gap-2">
                <h1 className="text-2xl leading-tight font-extrabold tracking-tight md:text-3xl">
                  {jobPosting.title}
                </h1>
                <p className="text-base text-gray-600">
                  {jobPosting.companyName} · {deadlineText(jobPosting.deadline)}
                </p>
                <a
                  href={jobPosting.sourceUrl}
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
                      onClick={() => toggle(requirement.requirementId)}
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

        <div className="mt-16 lg:mt-0">
          {/* DESIGN-TOKEN: 화면 높이에서 GNB와 위아래 여백을 뺀 만큼만 차지하고 넘치면 패널 안에서 스크롤하도록 계산식으로 높이를 제한한다. */}
          <div className="lg:sticky lg:top-24 lg:max-h-[calc(100dvh-8rem)] lg:overflow-y-auto lg:border-l lg:border-gray-200 lg:pl-6">
            <CommunityFeedPanel jobPostingId={jobPosting.jobPostingId} />
          </div>
        </div>
      </div>
    </>
  );
}
