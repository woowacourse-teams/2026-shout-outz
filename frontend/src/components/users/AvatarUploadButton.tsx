import { useId, useRef, useState, type ChangeEvent } from 'react';

import { MediaNotReadyError, uploadAvatar, waitUntilReady } from '@/api/media';
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
  const [slow, setSlow] = useState(false);
  const [failed, setFailed] = useState(false);
  /**
   * 기다리다 끊겼을 때의 미디어 ID.
   *
   * 파일은 이미 올라갔고 서버가 처리 중일 뿐이라, 다시 시도할 때 업로드를 되풀이하지 않고
   * 이 ID의 상태만 이어서 확인한다.
   */
  const [pendingMediaId, setPendingMediaId] = useState<number | null>(null);

  /** 성공하면 알리고, 아직 처리 중이면 이어받을 ID를 남긴다. */
  const settle = async (run: () => Promise<number>) => {
    setUploading(true);
    setSlow(false);
    setFailed(false);
    setPendingMediaId(null);

    try {
      onUploaded(await run());
      return true;
    } catch (error) {
      if (error instanceof MediaNotReadyError) {
        setPendingMediaId(error.mediaId);
      } else {
        setFailed(true);
      }
      return false;
    } finally {
      setUploading(false);
      setSlow(false);
    }
  };

  const pick = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    let objectUrl: string | undefined;
    if (onPreview) {
      objectUrl = URL.createObjectURL(file);
      onPreview(objectUrl);
    }

    const ok = await settle(() => uploadAvatar(file, { onSlow: () => setSlow(true) }));
    if (!ok) {
      onPreview?.(undefined);
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    }

    // 같은 파일을 다시 골랐을 때도 change가 울리도록 비운다.
    event.target.value = '';
  };

  /** 업로드는 건너뛰고 처리 완료만 다시 기다린다. */
  const resume = async (mediaId: number) => {
    await settle(async () => {
      await waitUntilReady(mediaId, { onSlow: () => setSlow(true) });
      return mediaId;
    });
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

      {/* 업로드는 끝났고 서버가 처리 중이다. 기다리는 동안 상황을 알린다. */}
      {uploading && slow && (
        <p role="status" className="text-xs text-gray-500">
          이미지 처리가 오래 걸리고 있어요. 조금만 기다려 주세요.
        </p>
      )}

      {pendingMediaId !== null && (
        <span className="flex items-center gap-2">
          <span role="status" className="text-xs text-gray-600">
            이미지 처리가 아직 끝나지 않았어요.
          </span>
          <Button variant="ghost" size="sm" onClick={() => void resume(pendingMediaId)}>
            다시 확인
          </Button>
        </span>
      )}

      {failed && (
        <p role="alert" className="text-xs text-red-600">
          이미지 업로드에 실패했습니다.
        </p>
      )}
    </>
  );
}
