import { useId, useRef, useState, type ChangeEvent } from 'react';

import { uploadAvatar } from '@/api/media';
import { Button } from '@/components/Button';

export interface AvatarUploadButtonProps {
  label: string; // "프로필 사진 추가", "사진 변경"
  onUploaded: (mediaId: number) => void;
  onPreview?: (objectUrl: string | undefined) => void;
  disabled?: boolean;
}

export function AvatarUploadButton({
  label,
  onUploaded,
  onPreview,
  disabled,
}: AvatarUploadButtonProps) {
  const inputId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [failed, setFailed] = useState(false);

  const pick = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setFailed(false);
    let objectUrl: string | undefined;
    try {
      if (onPreview) {
        objectUrl = URL.createObjectURL(file);
        onPreview(objectUrl);
      }

      onUploaded(await uploadAvatar(file));
    } catch {
      setFailed(true);
      onPreview?.(undefined);
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    } finally {
      setUploading(false);
      event.target.value = '';
    }
  };

  return (
    <>
      <label htmlFor={inputId} className="sr-only">
        {label}
      </label>
      <input
        ref={inputRef}
        id={inputId}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="sr-only"
        disabled={disabled || uploading}
        onChange={(event) => void pick(event)}
      />
      <Button
        variant="outline"
        size="sm"
        disabled={disabled || uploading}
        onClick={() => inputRef.current?.click()}
      >
        {uploading ? '올리는 중…' : label}
      </Button>

      {failed && (
        <p role="alert" className="text-xs text-red-600">
          이미지 업로드에 실패했습니다.
        </p>
      )}
    </>
  );
}
