import { useSuspenseQuery } from '@tanstack/react-query';
import { useState } from 'react';

import {
  projectTopicsQuery,
  type ProjectTopicGroup,
  type ProjectTopicGroupCode,
} from '@/apis/project-explore';
import { cn } from '@/utils/cn';

/** '전체'일 때 직군마다 보여 줄 주제 수 */
const PREVIEW_TOPIC_COUNT = 4;

export const groupOfTopic = (groups: ProjectTopicGroup[], topic: string | null) =>
  topic ? groups.find((group) => group.topics.some((item) => item.name === topic)) : undefined;

interface ProjectTopicNavProps {
  topic: string | null;
  onTopicChange: (topic: string | null) => void;
}

/** 데스크톱 프로젝트 탭 왼쪽의 직군·기술 주제 */
export function ProjectTopicNav({ topic, onTopicChange }: ProjectTopicNavProps) {
  const { data } = useSuspenseQuery(projectTopicsQuery());
  // 직군을 고르면 그 직군의 주제만 모두 보여 주고, '전체'면 직군마다 프로젝트가 많은 주제 4개씩 보여 준다.
  const [groupCode, setGroupCode] = useState<'ALL' | ProjectTopicGroupCode>(
    () => groupOfTopic(data.groups, topic)?.code ?? 'ALL',
  );
  const visibleGroups =
    groupCode === 'ALL' ? data.groups : data.groups.filter((group) => group.code === groupCode);

  return (
    <nav aria-label="기술 주제">
      <p className="px-3 pb-2 text-sm font-bold text-gray-900">직군</p>
      <ul className="flex flex-col gap-0.5 rounded-xl bg-gray-50 p-1">
        {/* 모바일 바텀시트와 같게 공통도 고를 수 있게 둔다. */}
        {[{ code: 'ALL' as const, shortName: '전체' }, ...data.groups].map((group) => (
          <li key={group.code}>
            <button
              type="button"
              aria-pressed={groupCode === group.code}
              onClick={() => setGroupCode(group.code)}
              className={cn(
                'focus-visible:outline-primary-600 w-full cursor-pointer rounded-lg px-3 py-2 text-left text-sm font-medium text-gray-600 focus-visible:outline-2',
                groupCode === group.code
                  ? 'bg-primary-50 text-primary-600 font-semibold'
                  : 'hover:bg-gray-100',
              )}
            >
              {group.shortName}
            </button>
          </li>
        ))}
      </ul>

      {visibleGroups.map((group) => (
        <div key={group.code} className="mt-6">
          <p className="px-3 pb-2 text-sm font-bold text-gray-900">
            {group.shortName}{' '}
            <span className="font-medium text-gray-400">{group.projectCount}</span>
          </p>
          <ul aria-label={`${group.name} 주제`}>
            {(groupCode === 'ALL' ? group.topics.slice(0, PREVIEW_TOPIC_COUNT) : group.topics).map(
              (item) => {
                const selected = item.name === topic;
                return (
                  <li key={item.name}>
                    {/* 고른 주제는 직군 목록과 같은 파란 강조로 보여 주고, 다시 누르면 푼다. */}
                    <button
                      type="button"
                      aria-pressed={selected}
                      onClick={() => onTopicChange(selected ? null : item.name)}
                      className={cn(
                        'focus-visible:outline-primary-600 flex w-full cursor-pointer items-start justify-between gap-2 rounded-lg px-3 py-1.5 text-left text-sm font-medium focus-visible:outline-2',
                        selected
                          ? 'bg-primary-50 text-primary-600 font-semibold'
                          : 'text-gray-600 hover:bg-gray-50',
                      )}
                    >
                      <span className="min-w-0">{item.name}</span>
                      <span className="shrink-0 text-gray-400">{item.projectCount}</span>
                    </button>
                  </li>
                );
              },
            )}
          </ul>
        </div>
      ))}
    </nav>
  );
}
