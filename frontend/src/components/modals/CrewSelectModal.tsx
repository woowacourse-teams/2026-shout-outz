import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import { crewSearchQueryOptions } from '@/api/project';
import { Avatar } from '@/components/Avatar';
import { UserHandle } from '@/components/users/UserHandle';
import { UserAffiliation } from '@/components/users/UserAffiliation';
import { Button } from '@/components/Button';
import {
  ModalSearchInput,
  SelectedCollector,
  SelectionModal,
} from '@/components/modals/SelectionModal';
import { type CrewSearchItem } from '@/types/project';
import { cn } from '@/utils/cn';

export interface CrewSelectModalProps {
  author?: CrewSearchItem;
  initial: CrewSearchItem[];
  onApply: (crews: CrewSearchItem[]) => void;
  onClose: () => void;
}

export function CrewSelectModal({ author, initial, onApply, onClose }: CrewSelectModalProps) {
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState<CrewSearchItem[]>(() =>
    author ? initial.filter((crew) => crew.handle !== author.handle) : initial,
  );
  const trimmed = keyword.trim();
  const { data: crews = [], isFetching } = useQuery({
    ...crewSearchQueryOptions(trimmed),
    enabled: trimmed.length > 0,
  });

  const toggle = (crew: CrewSearchItem) => {
    if (crew.handle === author?.handle) return;
    setSelected((current) =>
      current.some((item) => item.handle === crew.handle)
        ? current.filter((item) => item.handle !== crew.handle)
        : [...current, crew],
    );
  };

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

      {author && (
        <div className="flex items-center gap-2 rounded-xl bg-gray-50 p-3 text-sm font-semibold text-gray-900">
          <Avatar size="sm" src={author.avatarUrl} name={author.displayName} alt="" />
          <div className="min-w-0 flex-1">
            <span className="flex min-w-0 flex-wrap items-baseline gap-x-1">
              <span>{author.displayName}</span>
              <UserHandle handle={author.handle} userType={author.userType} />
            </span>
            <UserAffiliation {...author} />
          </div>
          <span className="text-primary-600 text-xs">작성자 · 항상 포함</span>
        </div>
      )}

      <SelectedCollector
        label="선택된 팀원"
        items={selected}
        getKey={(crew) => crew.handle}
        getLabel={(crew) => crew.displayName}
        renderLabel={(crew) => (
          <span className="flex min-w-0 flex-col items-start">
            <span className="flex min-w-0 flex-wrap items-baseline gap-x-1">
              <span>{crew.displayName}</span>
              <UserHandle handle={crew.handle} userType={crew.userType} />
            </span>
            <UserAffiliation {...crew} />
          </span>
        )}
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
            const isAuthor = crew.handle === author?.handle;
            const isSelected = selected.some((item) => item.handle === crew.handle);

            return (
              <li key={crew.handle} className="min-w-0">
                <button
                  type="button"
                  role="checkbox"
                  aria-checked={isAuthor || isSelected}
                  disabled={isAuthor}
                  onClick={() => toggle(crew)}
                  className={cn(
                    'focus-visible:outline-primary-600 flex w-full cursor-pointer items-center gap-3 rounded-2xl px-4 py-3 text-left focus-visible:outline-2 md:rounded-lg md:py-2.5',
                    isAuthor || isSelected
                      ? 'bg-primary-50'
                      : 'bg-gray-50 hover:bg-gray-100 md:bg-transparent',
                  )}
                >
                  <Avatar size="sm" src={crew.avatarUrl} name={crew.displayName} alt="" />
                  <span className="flex min-w-0 flex-col">
                    <span className="flex min-w-0 flex-wrap items-baseline gap-x-1.5">
                      <span
                        className={cn(
                          'truncate text-sm font-bold',
                          isSelected ? 'text-primary-600' : 'text-gray-900',
                        )}
                      >
                        {crew.displayName}
                      </span>
                      <UserHandle handle={crew.handle} userType={crew.userType} />
                    </span>
                    <UserAffiliation {...crew} />
                  </span>
                  <span
                    aria-hidden="true"
                    className={cn(
                      'ml-auto flex size-5.5 shrink-0 items-center justify-center rounded-full text-xs font-bold',
                      isAuthor || isSelected
                        ? 'bg-primary-600 text-white'
                        : 'bg-white text-transparent',
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
