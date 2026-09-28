import { useState } from 'react';

import { Avatar, type AvatarSize } from '@/components/Avatar';
import { AvatarUploadButton } from '@/components/users/AvatarUploadButton';

export interface AvatarPickerProps {
  /** 기본 프로필에 쓸 이름 */
  name: string;
  onChange: (mediaId: number | null) => void;
  /** @default 'lg' */
  size?: AvatarSize;
  label: string;
  disabled?: boolean;
}

export function AvatarPicker({ name, onChange, size = 'lg', label, disabled }: AvatarPickerProps) {
  const [preview, setPreview] = useState<string>();

  return (
    <div className="flex flex-col items-center gap-2">
      <Avatar size={size} src={preview} name={name} alt="" />
      <AvatarUploadButton
        label={label}
        disabled={disabled}
        onPreview={(objectUrl) => {
          setPreview((previous) => {
            if (previous) URL.revokeObjectURL(previous);
            return objectUrl;
          });
          if (!objectUrl) onChange(null);
        }}
        onUploaded={onChange}
      />
    </div>
  );
}
