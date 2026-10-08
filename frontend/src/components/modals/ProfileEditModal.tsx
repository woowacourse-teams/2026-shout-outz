import { useEffect, useId, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { updateMyProfileMutation } from '@/apis/user';
import { Avatar } from '@/components/Avatar';
import { Button } from '@/components/Button';
import { Field } from '@/components/Field';
import { Input } from '@/components/Input';
import { SelectionModal } from '@/components/modals/SelectionModal';
import { AvatarUploadButton } from '@/components/users/AvatarUploadButton';
import type { UserProfile } from '@/types/user';
import { getApiErrorMessage } from '@/utils/error';

export interface ProfileEditModalProps {
  profile: UserProfile;
  onClose: () => void;
}

/**
 * 내 프로필의 사진과 닉네임을 고친다.
 *
 * 우아한테크코스 소속 인증을 마친 사용자는 인증 때 확인한 닉네임을 써야 해서 닉네임 칸만 막는다.
 * 사진은 고른 즉시 올려 두고, 저장할 때 그 미디어 ID를 함께 보낸다.
 * 프로필 수정은 전체 교체라 화면에 없는 소개와 링크도 그대로 다시 보낸다.
 */
export function ProfileEditModal({ profile, onClose }: ProfileEditModalProps) {
  const formId = useId();
  const client = useQueryClient();
  const verified = profile.userType !== 'GENERAL';
  const [displayName, setDisplayName] = useState(profile.displayName);
  // 저장할 사진. 새로 올린 사진은 서버 주소를 모르니 미리보기 주소로 보여 준다.
  const [avatar, setAvatar] = useState({
    id: profile.avatarImageId ?? null,
    url: profile.avatarUrl ?? null,
  });
  const [preview, setPreview] = useState<string>();
  const [error, setError] = useState<string>();
  const objectUrls = useRef<string[]>([]);
  // 업로드가 끝났을 때 부르는 함수는 고를 때의 렌더에서 온 것이라, 최신 미리보기는 ref로 읽는다.
  const previewRef = useRef<string>(undefined);
  const uploading = preview !== undefined;

  // 미리보기 주소는 저장한 사진으로 계속 쓸 수 있어서 모달을 닫을 때 한꺼번에 정리한다.
  useEffect(() => () => objectUrls.current.forEach((url) => URL.revokeObjectURL(url)), []);

  const update = useMutation({
    ...updateMyProfileMutation,
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ['users', profile.handle] }),
        client.invalidateQueries({ queryKey: ['my-profile-summary'] }),
        client.invalidateQueries({ queryKey: ['my-profile'] }),
      ]);
      onClose();
    },
  });

  function validate() {
    if (!displayName.trim()) return '닉네임을 입력해 주세요.';
    if (Array.from(displayName).length > 50) return '닉네임은 50자 이하로 입력해 주세요.';
  }

  function submit() {
    if (uploading || update.isPending) return;
    const nextError = validate();
    setError(nextError);
    if (nextError) return;

    update.mutate({
      displayName: displayName.trim(),
      bio: profile.bio,
      githubProfileUrl: profile.githubProfileUrl,
      blogUrl: profile.blogUrl,
      avatarImageId: avatar.id,
    });
  }

  return (
    <SelectionModal
      title="프로필 수정"
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            취소
          </Button>
          <Button
            type="submit"
            form={formId}
            size="lg"
            className="flex-1 md:flex-none"
            disabled={uploading || update.isPending}
          >
            {update.isPending ? '저장 중…' : '저장'}
          </Button>
        </>
      }
    >
      <form
        id={formId}
        className="flex flex-col gap-5"
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
      >
        <div className="flex flex-col items-center gap-3">
          <Avatar
            size="lg"
            src={preview ?? avatar.url}
            name={displayName || profile.displayName}
            alt=""
          />
          <div className="flex flex-wrap items-center justify-center gap-2">
            <AvatarUploadButton
              label={avatar.url ? '사진 변경' : '프로필 사진 추가'}
              disabled={update.isPending}
              onPreview={(objectUrl) => {
                if (objectUrl) objectUrls.current.push(objectUrl);
                previewRef.current = objectUrl;
                setPreview(objectUrl);
              }}
              onUploaded={(mediaId) => {
                setAvatar({ id: mediaId, url: previewRef.current ?? null });
                previewRef.current = undefined;
                setPreview(undefined);
              }}
            />
            {avatar.url && (
              <Button
                variant="ghost"
                size="sm"
                disabled={uploading || update.isPending}
                onClick={() => setAvatar({ id: null, url: null })}
              >
                기본 이미지로
              </Button>
            )}
          </div>
        </div>
        <Field label="닉네임" error={error}>
          {(id) => (
            <div className="flex flex-col gap-1.5">
              <Input
                id={id}
                value={displayName}
                disabled={verified}
                aria-describedby={verified ? `${id}-locked` : undefined}
                aria-invalid={error ? true : undefined}
                onChange={(event) => setDisplayName(event.target.value)}
              />
              {verified && (
                <p id={`${id}-locked`} className="text-xs text-gray-500">
                  우아한테크코스 소속 인증을 마친 사용자는 닉네임을 바꿀 수 없어요.
                </p>
              )}
            </div>
          )}
        </Field>
        {update.isError && (
          <p role="alert" className="text-sm text-red-600">
            {getApiErrorMessage(update.error)}
          </p>
        )}
      </form>
    </SelectionModal>
  );
}
