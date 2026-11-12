import { useQuery } from '@tanstack/react-query';
import {
  IconCheck,
  IconChevronLeft,
  IconChevronRight,
  IconSelector,
  IconX,
} from '@tabler/icons-react';
import { overlay } from 'overlay-kit';
import { useEffect, useRef, useState } from 'react';

import {
  projectTopicsQuery,
  type ProjectTopic,
  type ProjectTopicGroupCode,
} from '@/apis/project-explore';
import { groupOfTopic } from '@/components/project-explore/ProjectTopicNav';
import { Drawer, DrawerContent, DrawerTitle } from '@/components/ui/drawer';
import { cn } from '@/utils/cn';

type GroupCode = 'ALL' | ProjectTopicGroupCode;

interface ProjectTopicFieldProps {
  topic: string | null;
  onTopicChange: (topic: string | null) => void;
}

/** 모바일 프로젝트 탭의 기술 주제 필드. 누르면 바텀시트가 열린다. */
export function ProjectTopicField({ topic, onTopicChange }: ProjectTopicFieldProps) {
  const open = () =>
    overlay.open(({ isOpen, close, unmount }) => (
      <ProjectTopicSheet
        topic={topic}
        onSelect={onTopicChange}
        isOpen={isOpen}
        onClose={close}
        onExit={unmount}
      />
    ));

  return (
    <div className="flex h-12 items-center rounded-xl border border-gray-200 bg-white">
      <button
        type="button"
        aria-haspopup="dialog"
        onClick={open}
        className="focus-visible:outline-primary-600 flex h-full min-w-0 flex-1 cursor-pointer items-center gap-2 rounded-xl px-4 text-left text-sm font-medium text-gray-900 focus-visible:outline-2"
      >
        <IconSelector className="size-4 shrink-0 text-gray-500" aria-hidden="true" />
        <span className="sr-only">기술 주제: </span>
        <span className={cn('truncate', topic && 'text-primary-600 font-semibold')}>
          {topic ?? '전체 주제'}
        </span>
      </button>
      {topic && (
        <button
          type="button"
          aria-label="기술 주제 해제"
          onClick={() => onTopicChange(null)}
          className="focus-visible:outline-primary-600 mr-1 flex size-10 shrink-0 cursor-pointer items-center justify-center rounded-lg text-gray-500 focus-visible:outline-2"
        >
          <IconX className="size-4" aria-hidden="true" />
        </button>
      )}
    </div>
  );
}

interface ProjectTopicSheetProps {
  topic: string | null;
  onSelect: (topic: string | null) => void;
  isOpen: boolean;
  onClose: () => void;
  onExit: () => void;
}

/** 1단계에서 직군을, 2단계에서 그 직군의 주제를 고르는 바텀시트 */
function ProjectTopicSheet({ topic, onSelect, isOpen, onClose, onExit }: ProjectTopicSheetProps) {
  const { data, isError } = useQuery(projectTopicsQuery());
  // 다시 열면 늘 1단계부터 보여 준다.
  const [step, setStep] = useState<GroupCode | null>(null);
  const [selected, setSelected] = useState(topic);
  const selectedGroup = data ? groupOfTopic(data.groups, selected) : undefined;

  const groups: { code: GroupCode; name: string; count: number; topics: ProjectTopic[] }[] = data
    ? [
        {
          code: 'ALL',
          name: '전체',
          count: data.totalCount,
          topics: uniqueTopics(data.groups.flatMap((group) => group.topics)),
        },
        ...data.groups.map((group) => ({
          code: group.code,
          name: group.name,
          count: group.projectCount,
          topics: group.topics,
        })),
      ]
    : [];
  const current = groups.find((group) => group.code === step);
  const highlightedCode: GroupCode = selectedGroup?.code ?? 'ALL';

  const choose = (next: string | null) => {
    setSelected(next);
    onSelect(next);
    if (next) onClose();
  };

  return (
    <Drawer
      open={isOpen}
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
      onAnimationEnd={(open) => {
        if (!open) onExit();
      }}
    >
      {/* DESIGN-TOKEN: 프로토타입 시트는 화면 높이의 대부분을 차지해 기본 80vh 대신 88dvh로 둔다. */}
      <DrawerContent aria-describedby={undefined} className="max-h-[88dvh] rounded-t-3xl">
        <div className="flex items-center gap-1 px-3 pt-2.5 pb-1.5">
          {current && (
            <button
              type="button"
              aria-label="직군 다시 고르기"
              onClick={() => setStep(null)}
              className="focus-visible:outline-primary-600 flex size-10 cursor-pointer items-center justify-center rounded-lg text-gray-900 focus-visible:outline-2"
            >
              <IconChevronLeft className="size-5" aria-hidden="true" />
            </button>
          )}
          <DrawerTitle className={cn('text-lg font-extrabold', !current && 'pl-2')}>
            {current?.name ?? '기술 주제'}
          </DrawerTitle>
          <button
            type="button"
            aria-label="닫기"
            onClick={onClose}
            className="focus-visible:outline-primary-600 ml-auto flex size-10 cursor-pointer items-center justify-center rounded-lg text-gray-500 focus-visible:outline-2"
          >
            <IconX className="size-4.5" aria-hidden="true" />
          </button>
        </div>

        <div className="px-5 pt-0.5 pb-2.5">
          <div
            role="status"
            className="bg-primary-50 text-primary-600 flex h-12 items-center gap-2 rounded-xl pr-2 pl-3.5 text-base font-bold"
          >
            <span className="shrink-0 text-xs font-semibold text-gray-500">선택됨</span>
            {selected ? (
              <>
                {selectedGroup && (
                  <span className="shrink-0 font-medium text-gray-400">{selectedGroup.name} ›</span>
                )}
                <span className="min-w-0 truncate">{selected}</span>
                <button
                  type="button"
                  onClick={() => choose(null)}
                  className="focus-visible:outline-primary-600 ml-auto h-7.5 shrink-0 cursor-pointer rounded-full border border-gray-200 bg-white px-3 text-sm font-semibold text-gray-600 focus-visible:outline-2"
                >
                  해제
                </button>
              </>
            ) : (
              <>
                <span>전체</span>
                <IconCheck className="ml-auto size-4.5 shrink-0" aria-hidden="true" />
              </>
            )}
          </div>
        </div>

        <div className="overflow-y-auto px-3 pb-6">
          {isError && (
            <p className="py-10 text-center text-sm text-gray-500">
              기술 주제를 불러오지 못했어요.
            </p>
          )}
          {current ? (
            <TopicList topics={current.topics} selected={selected} onChoose={choose} />
          ) : (
            <ul aria-label="직군">
              {groups.map((group) => {
                const highlighted = group.code === highlightedCode;
                return (
                  <li key={group.code} className="border-b border-gray-100 last:border-b-0">
                    <button
                      type="button"
                      onClick={() => setStep(group.code)}
                      className={cn(
                        'focus-visible:outline-primary-600 flex h-13 w-full cursor-pointer items-center gap-2 rounded-xl px-2 text-left text-base font-bold focus-visible:outline-2',
                        highlighted ? 'bg-primary-50 text-primary-600' : 'text-gray-900',
                      )}
                    >
                      {group.name}
                      <span className="text-sm font-medium text-gray-400">{group.count}</span>
                      <IconChevronRight
                        className={cn('ml-auto size-4.5', !highlighted && 'text-gray-500')}
                        aria-hidden="true"
                      />
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </DrawerContent>
    </Drawer>
  );
}

interface TopicListProps {
  topics: ProjectTopic[];
  selected: string | null;
  onChoose: (topic: string) => void;
}

function TopicList({ topics, selected, onChoose }: TopicListProps) {
  const selectedRef = useRef<HTMLButtonElement>(null);

  // 고른 주제가 있으면 그 위치로 스크롤해 둔다.
  useEffect(() => {
    selectedRef.current?.scrollIntoView?.({ block: 'center' });
  }, []);

  return (
    <ul aria-label="주제">
      {topics.map((item) => {
        const isSelected = item.name === selected;
        return (
          <li key={item.name} className="border-b border-gray-100 last:border-b-0">
            <button
              ref={isSelected ? selectedRef : undefined}
              type="button"
              aria-pressed={isSelected}
              onClick={() => onChoose(item.name)}
              className={cn(
                'focus-visible:outline-primary-600 flex h-13 w-full cursor-pointer items-center gap-1.5 rounded-xl px-2 text-left text-base font-medium focus-visible:outline-2',
                isSelected ? 'bg-primary-50 text-primary-600 font-bold' : 'text-gray-900',
              )}
            >
              <span className="min-w-0 truncate">{item.name}</span>
              <span className="shrink-0 text-sm font-medium text-gray-400">
                {item.projectCount}
              </span>
              {isSelected && <IconCheck className="ml-auto size-4.5 shrink-0" aria-hidden="true" />}
            </button>
          </li>
        );
      })}
    </ul>
  );
}

/** 여러 직군에 같은 주제가 있으면 한 번만, 프로젝트가 많은 순으로 */
const uniqueTopics = (topics: ProjectTopic[]) =>
  [...new Map(topics.map((topic) => [topic.name, topic])).values()].sort(
    (a, b) => b.projectCount - a.projectCount,
  );
