import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import { projectFilterOptionsQueryOptions } from '@/api/project-list';
import { Button } from '@/components/Button';
import { Tab } from '@/components/Tab';
import {
  ModalSearchInput,
  SelectableItem,
  SelectedCollector,
  SelectionModal,
} from '@/components/modals/SelectionModal';
import { type ProjectFilter } from '@/types/project';
import { cn } from '@/utils/cn';

export interface ProjectFilterModalProps {
  initial: ProjectFilter;
  onApply: (filter: ProjectFilter) => void;
  onClose: () => void;
}

type FilterTab = 'techTags' | 'cohorts';

const TAB_LABELS: Record<FilterTab, string> = {
  techTags: '기술 스택',
  cohorts: '기수',
};

export function ProjectFilterModal({ initial, onApply, onClose }: ProjectFilterModalProps) {
  const [tab, setTab] = useState<FilterTab>('techTags');
  const [draft, setDraft] = useState<ProjectFilter>(initial);
  const [techKeyword, setTechKeyword] = useState('');

  const { data: options, isPending, isError } = useQuery(projectFilterOptionsQueryOptions(draft));

  const toggleCohort = (cohort: number) =>
    setDraft((current) => ({
      ...current,
      cohorts: current.cohorts.includes(cohort)
        ? current.cohorts.filter((item) => item !== cohort)
        : [...current.cohorts, cohort],
    }));

  const toggleTechTag = (id: number) =>
    setDraft((current) => ({
      ...current,
      techTagIds: current.techTagIds.includes(id)
        ? current.techTagIds.filter((item) => item !== id)
        : [...current.techTagIds, id],
    }));

  const techTags = options?.techTags ?? [];
  const cohorts = options?.cohorts ?? [];
  const needle = techKeyword.trim().toLowerCase();
  const visibleTechTags = needle
    ? techTags.filter((tag) => tag.displayName.toLowerCase().includes(needle))
    : techTags;

  const chips = [
    ...draft.cohorts.map((cohort) => ({
      key: `cohort-${cohort}`,
      label: `${cohort}기`,
      remove: () => toggleCohort(cohort),
    })),
    ...draft.techTagIds.map((id) => ({
      key: `tech-${id}`,
      label: techTags.find((tag) => tag.id === id)?.displayName ?? `#${id}`,
      remove: () => toggleTechTag(id),
    })),
  ];

  const matched = options?.matchedProjectCount;

  return (
    <SelectionModal
      title="프로젝트 상세 필터"
      width="lg"
      onClose={onClose}
      footer={
        <>
          <Button
            variant="ghost"
            onClick={() => setDraft({ ...draft, cohorts: [], techTagIds: [] })}
            disabled={chips.length === 0}
          >
            전체 초기화
          </Button>
          <Button size="lg" className="flex-1 md:flex-none" onClick={() => onApply(draft)}>
            {matched === undefined ? '결과 보기' : `${matched}개 프로젝트 보기`}
          </Button>
        </>
      }
    >
      <Tab
        variant="chip"
        size="sm"
        value={tab}
        onChange={(next) => setTab(next as FilterTab)}
        aria-label="필터 분류"
      >
        {(Object.keys(TAB_LABELS) as FilterTab[]).map((value) => (
          <Tab.Item key={value} value={value}>
            {TAB_LABELS[value]}
          </Tab.Item>
        ))}
      </Tab>

      <SelectedCollector
        label="선택된 필터"
        items={chips}
        getKey={(chip) => chip.key}
        getLabel={(chip) => chip.label}
        onRemove={(chip) => chip.remove()}
        onClear={() => setDraft({ ...draft, cohorts: [], techTagIds: [] })}
      />

      {isPending ? (
        <p role="status" className="py-8 text-center text-sm text-gray-500">
          필터를 불러오는 중…
        </p>
      ) : isError ? (
        <p role="alert" className="py-8 text-center text-sm text-gray-600">
          필터를 불러오지 못했습니다.
        </p>
      ) : tab === 'techTags' ? (
        <>
          <ModalSearchInput
            value={techKeyword}
            onChange={setTechKeyword}
            label="기술 스택 검색"
            placeholder="기술 스택 검색... (ex: Spring, React)"
          />
          {visibleTechTags.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-500">검색 결과가 없습니다.</p>
          ) : (
            <ul aria-label="기술 스택 필터" className="grid grid-cols-2 gap-2">
              {visibleTechTags.map((tag) => (
                <li key={tag.id} className="min-w-0">
                  <SelectableItem
                    selected={draft.techTagIds.includes(tag.id)}
                    onClick={() => toggleTechTag(tag.id)}
                    meta={<span className="text-gray-500">{tag.projectCount}개</span>}
                  >
                    {tag.displayName}
                  </SelectableItem>
                </li>
              ))}
            </ul>
          )}
        </>
      ) : (
        <ul aria-label="기수 필터" className="grid grid-cols-2 gap-2">
          {cohorts.map((item) => (
            <li key={item.cohort} className="min-w-0">
              <SelectableItem
                selected={draft.cohorts.includes(item.cohort)}
                onClick={() => toggleCohort(item.cohort)}
                meta={<span className="text-gray-500">{item.projectCount}개</span>}
              >
                <span className={cn(draft.cohorts.includes(item.cohort) && 'font-bold')}>
                  {`${item.cohort}기 (${item.year})`}
                </span>
              </SelectableItem>
            </li>
          ))}
        </ul>
      )}
    </SelectionModal>
  );
}
