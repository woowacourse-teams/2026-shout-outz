import { IconPlus } from '@tabler/icons-react';

import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { UserHandle } from '@/components/users/UserHandle';
import { UserAffiliation } from '@/components/users/UserAffiliation';
import { CrewSelectModal } from '@/components/modals/CrewSelectModal';
import { useModal } from '@/hooks/useModal';
import { type CrewSearchItem } from '@/types/project';

export interface MemberFieldProps {
  author?: CrewSearchItem;
  value: CrewSearchItem[];
  onChange: (next: CrewSearchItem[]) => void;
  error?: string;
}

export function MemberField({ author, value, onChange, error }: MemberFieldProps) {
  const { open } = useModal();
  const members = author ? value.filter((crew) => crew.handle !== author.handle) : value;

  const openSelect = async () => {
    const selected = await open<CrewSearchItem[]>((close) => (
      <CrewSelectModal
        author={author}
        initial={members}
        onApply={close}
        onClose={() => close(members)}
      />
    ));
    if (selected)
      onChange(author ? selected.filter((crew) => crew.handle !== author.handle) : selected);
  };

  return (
    <div className="flex flex-col gap-2">
      {(author || members.length > 0) && (
        <ul aria-label="선택한 참여 팀원" className="flex flex-wrap gap-2">
          {author && (
            <li key={author.handle} className="max-w-full">
              <Badge
                tone="primary"
                className="max-w-full flex-col items-start gap-0.5 rounded-lg px-3 py-2 whitespace-normal"
              >
                <span className="flex min-w-0 flex-wrap items-baseline gap-x-1">
                  <span>{author.displayName}</span>
                  <UserHandle handle={author.handle} userType={author.userType} />
                  <span>(작성자)</span>
                </span>
                <UserAffiliation {...author} />
              </Badge>
            </li>
          )}
          {members.map((crew) => (
            <li key={crew.handle} className="max-w-full">
              <Badge
                tone="primary"
                className="max-w-full flex-col items-start gap-0.5 rounded-lg px-3 py-2 whitespace-normal"
              >
                <span className="flex min-w-0 flex-wrap items-baseline gap-x-1">
                  <span>{crew.displayName}</span>
                  <UserHandle handle={crew.handle} userType={crew.userType} />
                </span>
                <UserAffiliation {...crew} />
              </Badge>
            </li>
          ))}
        </ul>
      )}

      <Button variant="outline" className="w-fit gap-1" onClick={() => void openSelect()}>
        <IconPlus className="size-4" aria-hidden="true" />
        {members.length > 0 ? '참여 팀원 변경' : '참여 팀원 추가'}
      </Button>

      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );
}
