import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import { crewSearchQueryOptions } from '@/api/project';
import { Avatar } from '@/components/Avatar';
import { Button } from '@/components/Button';
import {
  ModalSearchInput,
  SelectedCollector,
  SelectionModal,
} from '@/components/modals/SelectionModal';
import { type CrewSearchItem } from '@/types/project';
import { cn } from '@/utils/cn';
import { formatCrewRole } from '@/utils/user';

export interface CrewSelectModalProps {
  initial: CrewSearchItem[];
  onApply: (crews: CrewSearchItem[]) => void;
  onClose: () => void;
}

export function CrewSelectModal({ initial, onApply, onClose }: CrewSelectModalProps) {
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState<CrewSearchItem[]>(initial);
  const trimmed = keyword.trim();
  const { data: crews = [], isFetching } = useQuery({
    ...crewSearchQueryOptions(trimmed),
    enabled: trimmed.length > 0,
  });

  const toggle = (crew: CrewSearchItem) =>
    setSelected((current) =>
      current.some((item) => item.handle === crew.handle)
        ? current.filter((item) => item.handle !== crew.handle)
        : [...current, crew],
    );

  return (
    <SelectionModal
      title="참여 팀원 선택"
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={() => setSelected([])} disabled={selected.length === 0}>
            선택 초기화
          </Button>
          <Button size="lg" className="flex-1 md:flex-none" onClick={() => onApply(selected)}>
            {selected.length === 0 ? '팀원 추가하기' : `${selected.length}명 팀원 추가하기`}
          </Button>
        </>
      }
    >
      <ModalSearchInput
        value={keyword}
        onChange={setKeyword}
        label="크루 검색"
        placeholder="우테코 크루 이름 또는 닉네임 검색..."
        autoFocus
      />

      <SelectedCollector
        label="선택된 팀원"
        items={selected}
        getKey={(crew) => crew.handle}
        getLabel={(crew) => crew.displayName}
        onRemove={toggle}
      />

      {!trimmed ? (
        <p className="py-8 text-center text-sm text-gray-500">
          이름이나 닉네임으로 크루를 검색해 주세요.
        </p>
      ) : isFetching ? (
        <p role="status" className="py-8 text-center text-sm text-gray-500">
          검색 중…
        </p>
      ) : crews.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">검색 결과가 없습니다.</p>
      ) : (
        <ul aria-label="크루 검색 결과" className="flex flex-col gap-2">
          {crews.map((crew) => {
            const isSelected = selected.some((item) => item.handle === crew.handle);
            const role = formatCrewRole(crew.cohort, crew.track);

            return (
              <li key={crew.handle} className="min-w-0">
                <button
                  type="button"
                  role="checkbox"
                  aria-checked={isSelected}
                  onClick={() => toggle(crew)}
                  className={cn(
                    'focus-visible:outline-primary-600 flex w-full cursor-pointer items-center gap-3 rounded-2xl px-4 py-3 text-left focus-visible:outline-2 md:rounded-lg md:py-2.5',
                    isSelected ? 'bg-primary-50' : 'bg-gray-50 hover:bg-gray-100 md:bg-transparent',
                  )}
                >
                  <Avatar size="sm" src={crew.avatarUrl ?? undefined} alt="" />
                  <span className="flex min-w-0 flex-col">
                    <span
                      className={cn(
                        'truncate text-sm font-bold',
                        isSelected ? 'text-primary-600' : 'text-gray-900',
                      )}
                    >
                      {crew.displayName}
                    </span>
                    <span className="truncate text-xs text-gray-500">
                      {role ? `우아한테크코스 ${role}` : `@${crew.handle}`}
                    </span>
                  </span>
                  <span
                    aria-hidden="true"
                    className={cn(
                      'ml-auto flex size-5.5 shrink-0 items-center justify-center rounded-full text-xs font-bold',
                      isSelected ? 'bg-primary-600 text-white' : 'bg-white text-transparent',
                    )}
                  >
                    ✓
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </SelectionModal>
  );
}
