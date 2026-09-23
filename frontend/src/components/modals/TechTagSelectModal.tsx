import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import { techTagsQueryOptions } from '@/api/project';
import { Button } from '@/components/Button';
import {
  ModalSearchInput,
  SelectableItem,
  SelectedCollector,
  SelectionModal,
} from '@/components/modals/SelectionModal';
import { type TechTag } from '@/types/project';

export interface TechTagSelectModalProps {
  initial: TechTag[];
  onApply: (techTags: TechTag[]) => void;
  onClose: () => void;
}

export function TechTagSelectModal({ initial, onApply, onClose }: TechTagSelectModalProps) {
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState<TechTag[]>(initial);
  const { data: techTags = [], isPending } = useQuery(techTagsQueryOptions(keyword.trim()));

  const toggle = (tag: TechTag) =>
    setSelected((current) =>
      current.some((item) => item.id === tag.id)
        ? current.filter((item) => item.id !== tag.id)
        : [...current, tag],
    );

  return (
    <SelectionModal
      title="기술 스택 선택"
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={() => setSelected([])} disabled={selected.length === 0}>
            선택 초기화
          </Button>
          <Button size="lg" className="flex-1 md:flex-none" onClick={() => onApply(selected)}>
            {selected.length === 0 ? '선택 완료' : `${selected.length}개 스택 선택 완료`}
          </Button>
        </>
      }
    >
      <ModalSearchInput
        value={keyword}
        onChange={setKeyword}
        label="기술 스택 검색"
        placeholder="기술 스택 검색... (ex: Spring, React)"
        autoFocus
      />

      <SelectedCollector
        label="선택된 기술 스택"
        items={selected}
        getKey={(tag) => tag.id}
        getLabel={(tag) => tag.displayName}
        onRemove={toggle}
      />

      {isPending ? (
        <p role="status" className="py-8 text-center text-sm text-gray-500">
          기술 스택을 불러오는 중…
        </p>
      ) : techTags.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">검색 결과가 없습니다.</p>
      ) : (
        <ul aria-label="기술 스택 목록" className="grid grid-cols-2 gap-2">
          {techTags.map((tag) => (
            <li key={tag.id} className="min-w-0">
              <SelectableItem
                selected={selected.some((item) => item.id === tag.id)}
                onClick={() => toggle(tag)}
              >
                {tag.displayName}
              </SelectableItem>
            </li>
          ))}
        </ul>
      )}
    </SelectionModal>
  );
}
