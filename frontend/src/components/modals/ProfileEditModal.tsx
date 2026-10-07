import { useId, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { updateMyProfileMutation } from '@/apis/user';
import { Button } from '@/components/Button';
import { Field } from '@/components/Field';
import { Input } from '@/components/Input';
import { SelectionModal } from '@/components/modals/SelectionModal';
import type { UserProfile } from '@/types/user';
import { getApiErrorMessage } from '@/utils/error';

export interface ProfileEditModalProps {
  profile: UserProfile;
  onClose: () => void;
}

/**
 * 내 프로필의 닉네임을 고친다.
 *
 * 구성원 인증을 마친 사용자는 인증 때 확인한 닉네임을 써야 해서 닉네임 칸을 막는다.
 * 프로필 수정은 전체 교체라 지금 사진, 소개, 링크도 그대로 다시 보낸다.
 */
export function ProfileEditModal({ profile, onClose }: ProfileEditModalProps) {
  const formId = useId();
  const client = useQueryClient();
  const verified = profile.userType !== 'GENERAL';
  const [displayName, setDisplayName] = useState(profile.displayName);
  const [error, setError] = useState<string>();
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
    if (verified || update.isPending) return;
    const nextError = validate();
    setError(nextError);
    if (nextError) return;

    update.mutate({
      displayName: displayName.trim(),
      bio: profile.bio,
      githubProfileUrl: profile.githubProfileUrl,
      blogUrl: profile.blogUrl,
      avatarImageId: profile.avatarImageId,
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
            disabled={verified || update.isPending}
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
                  구성원 인증을 마친 사용자는 닉네임을 바꿀 수 없어요.
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
