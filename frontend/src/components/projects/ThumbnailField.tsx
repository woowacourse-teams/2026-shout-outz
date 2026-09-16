import { useId, useState, type ChangeEvent } from 'react';
import { uploadProjectThumbnail } from '@/api/media';

/**
 * 대표 썸네일 이미지 필드(16:9 권장).
 *
 * 파일을 고르면 바로 업로드해서 받은 `mediaId`를 `onChange`로 알린다. 업로드 중에는 진행 상태를,
 * 실패하면 오류 문구를 보여준다.
 */
export interface ThumbnailFieldProps {
  value: number | null;
  onChange: (next: number | null) => void;
  error?: string;
}

export function ThumbnailField({ value, onChange, error }: ThumbnailFieldProps) {
  const inputId = useId();
  const [uploading, setUploading] = useState(false);
  const [failed, setFailed] = useState(false);

  const upload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setFailed(false);
    try {
      onChange(await uploadProjectThumbnail(file));
    } catch {
      setFailed(true);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={inputId} className="text-sm font-medium text-gray-900">
        대표 썸네일 이미지 (16:9 권장)
      </label>

      <input
        id={inputId}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="focus-visible:outline-primary-600 rounded-lg border border-dashed border-gray-300 px-4 py-6 text-sm text-gray-600 focus-visible:outline-2"
        onChange={upload}
      />

      {uploading && <p className="text-xs text-gray-500">이미지를 올리는 중입니다…</p>}
      {!uploading && value !== null && <p className="text-xs text-gray-600">이미지를 올렸어요.</p>}
      {failed && <p className="text-xs text-red-600">이미지 업로드에 실패했습니다.</p>}
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );
}
