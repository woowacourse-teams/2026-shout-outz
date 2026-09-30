import { Suspense, useState } from 'react';
import * as Sentry from '@sentry/react';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';

import { sessionQuery, signupMutation } from '@/apis/session';
import { updateMyProfile } from '@/apis/user';
import { AvatarPicker } from '@/components/users/AvatarPicker';
import { AppGnb } from '@/components/AppGnb';
import { Button, getButtonStyles } from '@/components/Button';
import { Field } from '@/components/Field';
import { Footer } from '@/components/Footer';
import { Input } from '@/components/Input';
import { getGithubLoginUrl } from '@/utils/auth';
import { getApiErrorMessage, isApiResponseError } from '@/utils/error';
import { analytics, toPathPattern } from '@/utils/analytics';

interface SignupPageProps {
  onComplete: () => void;
}

interface SignupErrors {
  handle?: string;
  displayName?: string;
}

export function SignupPage({ onComplete }: SignupPageProps) {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>가입 | shout-outz</title>
      <AppGnb />
      <main className="mx-auto flex w-full max-w-md flex-1 items-center px-4 py-12">
        <Suspense
          fallback={
            <p className="text-sm text-gray-600" role="status">
              로그인 상태를 확인하는 중…
            </p>
          }
        >
          <SignupContent onComplete={onComplete} />
        </Suspense>
      </main>
      <Footer />
    </div>
  );
}

function SignupContent({ onComplete }: SignupPageProps) {
  const { data: session } = useSuspenseQuery(sessionQuery);

  if (session.status === 'SIGNUP_REQUIRED') {
    return <SignupForm onComplete={onComplete} />;
  }

  if (session.status === 'AUTHENTICATED') {
    return (
      <div className="w-full rounded-xl border border-gray-200 p-6 text-center">
        <h1 className="text-xl font-bold">이미 가입되어 있어요.</h1>
        <Button className="mt-5" onClick={onComplete}>
          홈으로 가기
        </Button>
      </div>
    );
  }

  return (
    <div className="w-full rounded-xl border border-gray-200 p-6 text-center">
      <h1 className="text-xl font-bold">GitHub 로그인이 필요해요.</h1>
      <p className="mt-2 text-sm text-gray-600">
        로그인 후 서비스에서 사용할 정보를 입력해 주세요.
      </p>
      <a
        href={getGithubLoginUrl()}
        onClick={() =>
          analytics.track({ name: 'login_started', from: toPathPattern(window.location.pathname) })
        }
        className={getButtonStyles({ className: 'mt-5' })}
      >
        GitHub 로그인
      </a>
    </div>
  );
}

function SignupForm({ onComplete }: SignupPageProps) {
  const queryClient = useQueryClient();
  const mutation = useMutation(signupMutation);
  const [handle, setHandle] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [avatarImageId, setAvatarImageId] = useState<number | null>(null);
  const [errors, setErrors] = useState<SignupErrors>({});

  function validate() {
    const next: SignupErrors = {};
    if (!/^[A-Za-z0-9_-]{2,30}$/.test(handle)) {
      next.handle = '2~30자의 영문, 숫자, 밑줄, 하이픈으로 입력해 주세요.';
    }
    if (!displayName.trim()) next.displayName = '표시 이름을 입력해 주세요.';
    else if (Array.from(displayName).length > 50) {
      next.displayName = '표시 이름은 50자 이하로 입력해 주세요.';
    }
    return next;
  }

  async function submit() {
    const nextErrors = validate();
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    const name = displayName.trim();

    try {
      await mutation.mutateAsync({ handle, displayName: name });
      analytics.track({ name: 'signup_submitted' });

      // 가입 요청에는 프로필 이미지 자리가 없어서, 계정이 생긴 뒤에 한 번 더 저장한다.
      // 사진을 고르지 않았으면 부르지 않고, 이름에서 만든 기본 프로필이 그대로 남는다.
      // 여기서 실패해도 가입 자체는 끝난 것이라 막지 않고 넘어간다(프로필에서 다시 올릴 수 있다).
      if (avatarImageId !== null) {
        try {
          await updateMyProfile({ displayName: name, avatarImageId });
        } catch (error) {
          Sentry.captureException(error);
          // 프로필 사진만 못 저장한 것이므로 가입 흐름을 멈추지 않는다.
        }
      }

      await queryClient.fetchQuery(sessionQuery);
      onComplete();
    } catch (error) {
      analytics.track({
        name: 'signup_failed',
        reason: isApiResponseError(error) ? error.data.code : 'UNKNOWN',
      });
      if (isApiResponseError(error)) {
        const fieldErrors = Object.fromEntries(
          (error.data.details ?? []).map((detail) => [detail.field, detail.message]),
        );
        setErrors(fieldErrors);
      }
    }
  }

  return (
    <div className="w-full rounded-xl border border-gray-200 p-6 md:p-8">
      <h1 className="text-2xl font-bold">프로필 만들기</h1>
      <p className="mt-2 text-sm text-gray-600">서비스에서 사용할 이름을 입력해 주세요.</p>
      <form
        className="mt-7 flex flex-col gap-5"
        onSubmit={(event) => {
          event.preventDefault();
          void submit();
        }}
      >
        <AvatarPicker
          name={displayName}
          onChange={setAvatarImageId}
          label="프로필 사진 추가"
          disabled={mutation.isPending}
        />
        <Field label="아이디" error={errors.handle}>
          {(id) => (
            <Input
              id={id}
              value={handle}
              onChange={(event) => setHandle(event.target.value)}
              placeholder="shoutoutz_user"
              autoComplete="username"
              aria-invalid={Boolean(errors.handle)}
              disabled={mutation.isPending}
            />
          )}
        </Field>
        <Field label="표시 이름" error={errors.displayName}>
          {(id) => (
            <Input
              id={id}
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              placeholder="샤라웃"
              autoComplete="name"
              aria-invalid={Boolean(errors.displayName)}
              disabled={mutation.isPending}
            />
          )}
        </Field>
        {mutation.isError && (
          <p role="alert" className="text-sm text-red-600">
            {getApiErrorMessage(mutation.error)}
          </p>
        )}
        <Button type="submit" size="lg" className="mt-1 w-full" disabled={mutation.isPending}>
          {mutation.isPending ? '가입 중…' : '가입하기'}
        </Button>
      </form>
    </div>
  );
}
