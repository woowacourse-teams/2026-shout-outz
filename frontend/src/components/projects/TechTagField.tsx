import { IconPlus } from '@tabler/icons-react';

import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { TechTagSelectModal } from '@/components/modals/TechTagSelectModal';
import { useModal } from '@/hooks/useModal';
import { type TechTag } from '@/types/project';

export interface TechTagFieldProps {
  value: TechTag[];
  onChange: (next: TechTag[]) => void;
  error?: string;
}

export function TechTagField({ value, onChange, error }: TechTagFieldProps) {
  const { open } = useModal();

  const openSelect = async () => {
    const selected = await open<TechTag[]>((close) => (
      <TechTagSelectModal initial={value} onApply={close} onClose={() => close(value)} />
    ));
    if (selected) onChange(selected);
  };

  return (
    <div className="flex flex-col gap-2">
      {value.length > 0 && (
        <ul aria-label="선택한 기술 스택" className="flex flex-wrap gap-2">
          {value.map((tag) => (
            <li key={tag.id}>
              <Badge tone="primary">{tag.displayName}</Badge>
            </li>
          ))}
        </ul>
      )}

      <Button variant="outline" className="w-fit gap-1" onClick={() => void openSelect()}>
        <IconPlus className="size-4" aria-hidden="true" />
        {value.length > 0 ? '기술 스택 변경' : '기술 스택 추가'}
      </Button>

      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );
}
