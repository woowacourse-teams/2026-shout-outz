import { IconPlus } from '@tabler/icons-react';

import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { CrewSelectModal } from '@/components/modals/CrewSelectModal';
import { useModal } from '@/hooks/useModal';
import { type CrewSearchItem } from '@/types/project';

export interface MemberFieldProps {
  value: CrewSearchItem[];
  onChange: (next: CrewSearchItem[]) => void;
  error?: string;
}

export function MemberField({ value, onChange, error }: MemberFieldProps) {
  const { open } = useModal();

  const openSelect = async () => {
    const selected = await open<CrewSearchItem[]>((close) => (
      <CrewSelectModal initial={value} onApply={close} onClose={() => close(value)} />
    ));
    if (selected) onChange(selected);
  };

  return (
    <div className="flex flex-col gap-2">
      {value.length > 0 && (
        <ul aria-label="선택한 참여 팀원" className="flex flex-wrap gap-2">
          {value.map((crew) => (
            <li key={crew.handle}>
              <Badge tone="primary">{crew.displayName}</Badge>
            </li>
          ))}
        </ul>
      )}

      <Button variant="outline" className="w-fit gap-1" onClick={() => void openSelect()}>
        <IconPlus className="size-4" aria-hidden="true" />
        {value.length > 0 ? '참여 팀원 변경' : '참여 팀원 추가'}
      </Button>

      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );
}
