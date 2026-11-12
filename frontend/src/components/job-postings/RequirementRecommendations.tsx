import { useQueries } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';

import type { Material } from '@/apis/explore';
import { requirementRecommendationsQuery, type JobRequirement } from '@/apis/job-posting';
import { DotTab } from '@/components/explore/DotTab';
import { MaterialList, TechTalkList } from '@/components/explore/MaterialList';
import { recommendationToMaterial } from '@/components/job-postings/recommendationMaterial';
import { isBlogMaterial, isGithubMaterial, isTechTalk } from '@/components/explore/MaterialParts';
import { getApiErrorMessage } from '@/utils/error';

type KindFilter = 'ALL' | 'BLOG' | 'DOCUMENT' | 'TECH_TALK';

interface RequirementRecommendationsProps {
  jobPostingId: number;
  /** 고른 역량. 공고에 적힌 순서다. */
  requirements: JobRequirement[];
}

/** 고른 역량들의 추천 자료를 하나로 모아 종류별로 보여 준다. 여러 역량에 걸친 자료는 한 번만 보여 준다. */
export function RequirementRecommendations({
  jobPostingId,
  requirements,
}: RequirementRecommendationsProps) {
  const [filter, setFilter] = useState<KindFilter>('ALL');
  const results = useQueries({
    queries: requirements.map((requirement) =>
      requirementRecommendationsQuery(jobPostingId, requirement.requirementId),
    ),
  });

  if (requirements.length === 0) {
    return <Notice>역량을 하나 이상 고르면 관련 자료를 보여 드려요.</Notice>;
  }

  const failed = results.find((result) => result.isError);
  if (failed) {
    return (
      <p role="alert" className="text-sm text-gray-500">
        {getApiErrorMessage(failed.error)}
      </p>
    );
  }

  if (results.some((result) => result.isPending)) {
    return (
      <p role="status" className="py-6 text-gray-500">
        자료를 불러오는 중…
      </p>
    );
  }

  // 같은 자료는 한 번만 담고, 어떤 역량에서 나왔는지 모은다.
  const byId = new Map<number, { material: Material; requirementNames: string[] }>();
  results.forEach((result, index) => {
    for (const recommendation of result.data ?? []) {
      const material = recommendationToMaterial(recommendation);
      const entry = byId.get(material.resourceId) ?? { material, requirementNames: [] };
      entry.requirementNames.push(requirements[index]!.name);
      byId.set(material.resourceId, entry);
    }
  });
  const entries = [...byId.values()];
  const requirementNamesOf = (material: Material) =>
    byId.get(material.resourceId)?.requirementNames ?? [];

  const groups: { kind: Exclude<KindFilter, 'ALL'>; title: string; materials: Material[] }[] = [
    {
      kind: 'BLOG',
      title: '블로그',
      materials: entries.filter((e) => isBlogMaterial(e.material)).map((e) => e.material),
    },
    {
      kind: 'DOCUMENT',
      title: 'PR과 팀 문서',
      materials: entries.filter((e) => isGithubMaterial(e.material)).map((e) => e.material),
    },
    {
      kind: 'TECH_TALK',
      title: '테코톡',
      materials: entries.filter((e) => isTechTalk(e.material)).map((e) => e.material),
    },
  ];
  const visible = groups.filter(
    (group) => group.materials.length > 0 && (filter === 'ALL' || filter === group.kind),
  );

  return (
    <div className="flex flex-col gap-12">
      <div
        role="tablist"
        aria-label="자료 종류"
        className="-mb-3 flex flex-wrap items-center gap-4.5"
      >
        <DotTab
          selected={filter === 'ALL'}
          count={entries.length}
          onSelect={() => setFilter('ALL')}
        >
          전체
        </DotTab>
        {groups.map((group) => (
          <DotTab
            key={group.kind}
            selected={filter === group.kind}
            count={group.materials.length}
            onSelect={() => setFilter(group.kind)}
          >
            {group.title}
          </DotTab>
        ))}
      </div>

      {visible.length === 0 && <Notice>고른 역량에 맞는 자료가 아직 없어요.</Notice>}

      {visible.map((group) => (
        <section key={group.kind} aria-labelledby={`job-recommendation-${group.kind}`}>
          <h2
            id={`job-recommendation-${group.kind}`}
            className="pb-1 text-lg font-bold tracking-tight"
          >
            {group.title}{' '}
            <span className="font-medium text-gray-400">{group.materials.length}</span>
          </h2>
          {group.kind === 'TECH_TALK' ? (
            <div className="mt-4">
              <TechTalkList talks={group.materials} columns={3} />
            </div>
          ) : (
            <MaterialList
              materials={group.materials}
              renderExtra={(material) => (
                <span className="text-xs text-gray-400">
                  관련 역량 · {requirementNamesOf(material).join(', ')}
                </span>
              )}
            />
          )}
        </section>
      ))}
    </div>
  );
}

function Notice({ children }: { children: ReactNode }) {
  return (
    <div className="rounded-2xl border border-dashed border-gray-200 px-4 py-10 text-center text-sm text-gray-500">
      {children}
    </div>
  );
}
