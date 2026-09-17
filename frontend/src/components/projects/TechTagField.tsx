import { useState } from 'react';

import { fetchTechTags } from '@/api/project';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { Input } from '@/components/Input';
import { type TechTag } from '@/types/project';

/**
 * 기술 스택 선택 필드.
 *
 * TODO 기술 스택 선택 시트/모달이 구현되면 그것으로 교체한다. 지금은 개발용으로 태그 id를 직접
 * 입력받아 `GET /api/v1/tech-tags`에서 찾은 태그를 칩으로 쌓는다.
 */
export interface TechTagFieldProps {
  value: TechTag[];
  onChange: (next: TechTag[]) => void;
  error?: string;
}

export function TechTagField({ value, onChange, error }: TechTagFieldProps) {
  const [id, setId] = useState('');
  const [notFound, setNotFound] = useState(false);

  // 임시 UI라 쿼리 캐시에 남기지 않고 누를 때마다 조회한다.
  const add = async () => {
    const techTags = await fetchTechTags();
    const found = techTags.find((tag) => String(tag.id) === id.trim());

    if (!found) {
      setNotFound(true);
      return;
    }

    setNotFound(false);
    setId('');
    if (value.some((tag) => tag.id === found.id)) return;

    onChange([...value, found]);
  };

  return (
    <div className="flex flex-col gap-2">
      {value.length > 0 && (
        <ul className="flex flex-wrap gap-2">
          {value.map((tag) => (
            <li key={tag.id}>
              <Badge tone="primary" className="gap-1 py-1 pr-1 pl-2">
                {tag.displayName}
                <button
                  type="button"
                  aria-label={`${tag.displayName} 삭제`}
                  className="focus-visible:outline-primary-600 cursor-pointer rounded-sm px-1 focus-visible:outline-2"
                  onClick={() => onChange(value.filter((selected) => selected.id !== tag.id))}
                >
                  ✕
                </button>
              </Badge>
            </li>
          ))}
        </ul>
      )}

      {/* TODO 시트가 생기면 이 입력 줄은 시트를 여는 버튼으로 바뀐다. */}
      <div className="flex gap-2">
        <Input
          aria-label="기술 스택 ID"
          value={id}
          onChange={(event) => setId(event.target.value)}
          placeholder="기술 스택 ID (개발용)"
        />
        <Button variant="outline" className="shrink-0" onClick={() => void add()}>
          기술 스택 추가
        </Button>
      </div>

      {notFound && <p className="text-xs text-red-600">해당 id의 기술 스택을 찾을 수 없습니다.</p>}
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );
}
