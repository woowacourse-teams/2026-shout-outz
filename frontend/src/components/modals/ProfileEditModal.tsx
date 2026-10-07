import { useId, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { updateMyProfileMutation } from '@/apis/user';
import { Button } from '@/components/Button';
import { Field } from '@/components/Field';
import { Input } from '@/components/Input';
import { SelectionModal } from '@/components/modals/SelectionModal';
import type { UserProfile } from '@/types/user';
import { getApiErrorMessage, isApiResponseError } from '@/utils/error';

export interface ProfileEditModalProps {
  profile: UserProfile;
  onClose: () => void;
}

interface ProfileEditErrors {
  displayName?: string;
  bio?: string;
  githubProfileUrl?: string;
  blogUrl?: string;
}

const toNullable = (value: string) => value.trim() || null;

/**
 * 내 프로필의 닉네임, 한 줄 소개, 링크를 고친다.
 *
 * 구성원 인증을 마친 사용자는 인증 때 확인한 닉네임을 써야 해서 닉네임 칸을 막는다.
 * 프로필 수정은 전체 교체라 지금 사진도 그대로 다시 보낸다.
 */
export function ProfileEditModal({ profile, onClose }: ProfileEditModalProps) {
  const formId = useId();
  const client = useQueryClient();
  const verified = profile.userType !== 'GENERAL';
  const [displayName, setDisplayName] = useState(profile.displayName);
  const [bio, setBio] = useState(profile.bio ?? '');
  const [githubProfileUrl, setGithubProfileUrl] = useState(profile.githubProfileUrl ?? '');
  const [blogUrl, setBlogUrl] = useState(profile.blogUrl ?? '');
  const [errors, setErrors] = useState<ProfileEditErrors>({});
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
    onError: (error) => {
      if (!isApiResponseError(error)) return;
      setErrors(
        Object.fromEntries(
          (error.data.details ?? []).map((detail) => [detail.field, detail.message]),
        ),
      );
    },
  });

  function validate() {
    const next: ProfileEditErrors = {};
    if (!displayName.trim()) next.displayName = '닉네임을 입력해 주세요.';
    else if (Array.from(displayName).length > 50) {
      next.displayName = '닉네임은 50자 이하로 입력해 주세요.';
    }
    if (Array.from(bio).length > 200) next.bio = '한 줄 소개는 200자 이하로 입력해 주세요.';
    return next;
  }

  function submit() {
    if (update.isPending) return;
    const nextErrors = validate();
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    update.mutate({
      displayName: verified ? profile.displayName : displayName.trim(),
      bio: toNullable(bio),
      githubProfileUrl: toNullable(githubProfileUrl),
      blogUrl: toNullable(blogUrl),
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
            disabled={update.isPending}
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
        <Field label="닉네임" error={errors.displayName}>
          {(id) => (
            <div className="flex flex-col gap-1.5">
              <Input
                id={id}
                value={displayName}
                disabled={verified}
                aria-describedby={verified ? `${id}-locked` : undefined}
                aria-invalid={errors.displayName ? true : undefined}
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
        <Field label="한 줄 소개" error={errors.bio}>
          {(id) => (
            <Input
              id={id}
              value={bio}
              placeholder="나를 한 줄로 소개해 주세요"
              onChange={(event) => setBio(event.target.value)}
            />
          )}
        </Field>
        <Field label="GitHub" error={errors.githubProfileUrl}>
          {(id) => (
            <Input
              id={id}
              type="url"
              value={githubProfileUrl}
              placeholder="https://github.com/"
              onChange={(event) => setGithubProfileUrl(event.target.value)}
            />
          )}
        </Field>
        <Field label="블로그" error={errors.blogUrl}>
          {(id) => (
            <Input
              id={id}
              type="url"
              value={blogUrl}
              placeholder="https://"
              onChange={(event) => setBlogUrl(event.target.value)}
            />
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
