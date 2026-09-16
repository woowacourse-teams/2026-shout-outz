import { useState } from 'react';

import { searchCrews } from '@/api/project';
import { type CrewSearchItem } from '@/types/project';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { Input } from '@/components/Input';

/**
 * 참여 팀원 선택 필드.
 *
 * TODO 참여 팀원 선택 시트/모달이 구현되면 그것으로 교체한다. 지금은 폼 안에서 이름이나 handle로
 * `GET /api/v1/users/search`를 검색해 고른 크루의 handle을 쌓는다.
 */
export interface MemberFieldProps {
  value: string[];
  onChange: (next: string[]) => void;
  error?: string;
}

export function MemberField({ value, onChange, error }: MemberFieldProps) {
  const [keyword, setKeyword] = useState('');
  const [crews, setCrews] = useState<CrewSearchItem[]>([]);

  // 임시 UI라 쿼리 캐시에 남기지 않고 버튼을 누를 때 조회한다.
  const search = async () => {
    const trimmed = keyword.trim();
    setCrews(trimmed ? await searchCrews(trimmed) : []);
  };

  const add = (handle: string) => {
    if (value.includes(handle)) return;

    onChange([...value, handle]);
  };

  return (
    <div className="flex flex-col gap-2">
      {value.length > 0 && (
        <ul className="flex flex-wrap gap-2">
          {value.map((handle) => (
            <li key={handle}>
              <Badge tone="primary" className="gap-1 py-1 pr-1 pl-2">
                {handle}
                <button
                  type="button"
                  aria-label={`${handle} 삭제`}
                  className="focus-visible:outline-primary-600 cursor-pointer rounded-sm px-1 focus-visible:outline-2"
                  onClick={() => onChange(value.filter((selected) => selected !== handle))}
                >
                  ✕
                </button>
              </Badge>
            </li>
          ))}
        </ul>
      )}

      {/* TODO 시트가 생기면 이 검색 줄은 시트를 여는 버튼으로 바뀐다. */}
      <div className="flex gap-2">
        <Input
          type="search"
          aria-label="참여 팀원 검색"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="우테코 크루 이름 또는 닉네임 검색"
        />
        <Button variant="outline" className="shrink-0" onClick={() => void search()}>
          검색
        </Button>
      </div>

      {crews.length > 0 && (
        <ul className="flex flex-col gap-1">
          {crews.map((crew) => (
            <li key={crew.handle}>
              <button
                type="button"
                className="focus-visible:outline-primary-600 w-full cursor-pointer rounded-lg px-3 py-2 text-left text-sm text-gray-900 hover:bg-gray-50 focus-visible:outline-2"
                onClick={() => add(crew.handle)}
              >
                {crew.displayName}
                <span className="ml-2 text-xs text-gray-500">{crew.handle}</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );
}
