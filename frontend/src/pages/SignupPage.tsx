import { Suspense, useId, useState } from 'react';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import { sessionQuery, signupMutation } from '@/apis/session';
import {
  createVerificationRequestMutation,
  verificationRequestQuery,
  type VerificationTrack,
} from '@/apis/verification';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Button, getButtonStyles } from '@/components/Button';
import { Field } from '@/components/Field';
import { Footer } from '@/components/Footer';
import { Input } from '@/components/Input';
import { CrewInfoFields } from '@/components/users/CrewInfoFields';
import { getGithubLoginUrl } from '@/utils/auth';
import { getApiErrorMessage, isApiResponseError } from '@/utils/error';
import { analytics, toPathPattern } from '@/utils/analytics';

interface SignupPageProps {
  onComplete: () => void;
}

interface SignupErrors {
  handle?: string;
  displayName?: string;
  crew?: string;
  cohort?: string;
  track?: string;
}

/** 크루로 가입했을 때 이어서 보낸 인증 신청의 결과 */
type VerificationResult = 'requested' | 'failed';

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
  // 가입하면 세션이 AUTHENTICATED로 바뀐다. 이어서 크루 인증을 신청하고 결과를 안내해야 하므로
  // 가입을 시작한 뒤에는 세션 상태와 상관없이 폼을 유지한다.
  const [signingUp, setSigningUp] = useState(false);

  if (session.status === 'SIGNUP_REQUIRED' || signingUp) {
    return <SignupForm onComplete={onComplete} onSubmit={() => setSigningUp(true)} />;
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

function SignupForm({ onComplete, onSubmit }: SignupPageProps & { onSubmit: () => void }) {
  const queryClient = useQueryClient();
  const mutation = useMutation(signupMutation);
  const verification = useMutation(createVerificationRequestMutation);
  const policyId = useId();
  // 입력칸에는 @ 뒤의 이름만 둔다. @는 칸 앞에 고정으로 보여 주고, 서버에 보낼 때 붙인다.
  const [handleName, setHandleName] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [crew, setCrew] = useState<boolean | null>(null);
  const [cohort, setCohort] = useState<string | null>(null);
  const [track, setTrack] = useState<VerificationTrack | null>(null);
  const [errors, setErrors] = useState<SignupErrors>({});
  // 가입과 인증 신청을 이어서 보내는 동안 입력을 막는다. 두 요청 사이에도 풀리지 않게 따로 둔다.
  const [submitting, setSubmitting] = useState(false);
  const [verificationResult, setVerificationResult] = useState<VerificationResult | null>(null);

  function validate() {
    const next: SignupErrors = {};
    // 서버 `Handle.HANDLE_FORMAT_REGEX`(`^@[A-Za-z0-9_-]{2,30}$`)에서 @ 뒤 부분과 같은 규칙.
    if (!/^[A-Za-z0-9_-]{2,30}$/.test(handleName)) {
      next.handle = '2~30자의 영문, 숫자, 밑줄, 하이픈으로 입력해 주세요.';
    }
    if (!displayName.trim()) next.displayName = '닉네임을 입력해 주세요.';
    else if (Array.from(displayName).length > 50) {
      next.displayName = '닉네임은 50자 이하로 입력해 주세요.';
    }
    if (crew === null) next.crew = '우테코 크루인지 선택해 주세요.';
    if (crew && !cohort) next.cohort = '기수를 선택해 주세요.';
    if (crew && !track) next.track = '트랙을 선택해 주세요.';
    return next;
  }

  async function submit() {
    const nextErrors = validate();
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setSubmitting(true);
    onSubmit();
    try {
      await mutation.mutateAsync({ handle: `@${handleName}`, displayName: displayName.trim() });
      analytics.track({ name: 'signup_submitted' });
      // 가입하면 서버가 인증된 세션을 새로 만든다. 인증 신청에 쓸 CSRF 토큰도 여기서 다시 받는다.
      await queryClient.fetchQuery(sessionQuery);
    } catch (error) {
      setSubmitting(false);
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
      return;
    }

    if (!crew) {
      onComplete();
      return;
    }

    // 가입이 끝난 뒤에만 인증을 신청한다. 실패해도 가입은 되돌리지 않고 다시 신청하도록 안내한다.
    try {
      const created = await verification.mutateAsync({
        userType: 'WOOWACOURSE_CREW',
        nickname: displayName.trim(),
        cohort: Number(cohort),
        track,
      });
      queryClient.setQueryData(verificationRequestQuery.queryKey, created);
      analytics.track({
        name: 'verification_requested',
        userType: 'WOOWACOURSE_CREW',
        track,
        cohort: Number(cohort),
      });
      setVerificationResult('requested');
    } catch {
      setVerificationResult('failed');
    }
  }

  if (verificationResult) {
    return <SignupCompleted verificationResult={verificationResult} onComplete={onComplete} />;
  }

  return (
    <div className="w-full rounded-xl border border-gray-200 p-6 md:p-8">
      <h1 className="text-2xl font-bold">프로필 만들기</h1>
      <p className="mt-2 text-sm text-gray-600">샤라웃에서 사용할 닉네임과 아이디를 정해 주세요.</p>
      <form
        aria-describedby={crew ? policyId : undefined}
        className="mt-7 flex flex-col gap-5"
        onSubmit={(event) => {
          event.preventDefault();
          void submit();
        }}
      >
        <Field label="닉네임" error={errors.displayName}>
          {(id) => (
            <>
              <Input
                id={id}
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                placeholder="코딩하는 곰돌이"
                autoComplete="nickname"
                aria-describedby={`${id}-help`}
                aria-invalid={Boolean(errors.displayName)}
                disabled={submitting}
              />
              <ul
                id={`${id}-help`}
                className="flex list-disc flex-col gap-1 pl-4 text-xs leading-5 text-gray-500"
              >
                <li>글과 댓글에 표시되는 이름이에요.</li>
                {crew && (
                  <li>크루 인증에도 이 닉네임을 써요. 우테코에서 쓰는 닉네임을 입력해 주세요.</li>
                )}
              </ul>
            </>
          )}
        </Field>
        <Field label="사용자 아이디" error={errors.handle}>
          {(id) => (
            <>
              <div className="flex flex-col gap-1">
                <div className="relative">
                  <span
                    aria-hidden="true"
                    className="pointer-events-none absolute inset-y-0 left-4 flex items-center text-sm text-gray-500"
                  >
                    @
                  </span>
                  <Input
                    id={id}
                    value={handleName}
                    // @는 자동으로 붙이므로 붙여넣은 값의 접두사는 제거한다.
                    onChange={(event) => setHandleName(event.target.value.replace(/^@+/, ''))}
                    placeholder="woowa_crew"
                    autoComplete="username"
                    autoCapitalize="none"
                    spellCheck={false}
                    aria-describedby={`${id}-help ${id}-preview`}
                    aria-invalid={Boolean(errors.handle)}
                    disabled={submitting}
                    className="pl-8"
                  />
                </div>
                <p id={`${id}-preview`} className="px-2 text-xs leading-5 break-all text-gray-400">
                  https://shout-ou.tz/users/@{handleName || 'woowa_crew'}
                </p>
              </div>
              <ul
                id={`${id}-help`}
                className="flex list-disc flex-col gap-1 pl-4 text-xs leading-5 text-gray-500"
              >
                <li>프로필 주소에 사용되는 고유한 아이디예요.</li>
                <li>GitHub 아이디와 달라도 괜찮아요.</li>
                <li>영문·숫자·밑줄(_)·하이픈(-)으로 2~30자</li>
              </ul>
            </>
          )}
        </Field>
        <CrewQuestion
          value={crew}
          error={errors.crew}
          disabled={submitting}
          onChange={(next) => {
            setCrew(next);
            setErrors((previous) => ({ ...previous, crew: undefined }));
          }}
        />
        {crew && (
          <AsyncBoundary>
            <CrewInfoFields
              cohort={cohort}
              track={track}
              onCohortChange={setCohort}
              onTrackChange={setTrack}
              policyId={policyId}
              errors={errors}
              disabled={submitting}
            />
          </AsyncBoundary>
        )}
        {mutation.isError && (
          <p role="alert" className="text-sm text-red-600">
            {getApiErrorMessage(mutation.error)}
          </p>
        )}
        <Button type="submit" size="lg" className="mt-1 w-full" disabled={submitting}>
          {submitting ? '가입 중…' : crew ? '가입하고 크루 인증 신청하기' : '가입하기'}
        </Button>
      </form>
    </div>
  );
}

/**
 * 우테코 크루 여부. 크루라면 가입하면서 크루 인증까지 신청한다.
 *
 * 처음에는 아무것도 고르지 않은 상태로 두고, 가입 전에 꼭 고르게 한다.
 */
function CrewQuestion({
  value,
  error,
  disabled,
  onChange,
}: {
  value: boolean | null;
  error?: string;
  disabled?: boolean;
  onChange: (crew: boolean) => void;
}) {
  const labelId = useId();

  return (
    <div className="flex flex-col gap-2">
      <p id={labelId} className="text-sm font-medium text-gray-900">
        우테코 크루이신가요?
      </p>
      <div role="group" aria-labelledby={labelId} className="grid grid-cols-2 gap-2">
        {[
          { crew: true, label: '네, 크루예요' },
          { crew: false, label: '아니요' },
        ].map((option) => (
          <Button
            key={option.label}
            variant={value === option.crew ? 'primary' : 'outline'}
            aria-pressed={value === option.crew}
            disabled={disabled}
            onClick={() => onChange(option.crew)}
          >
            {option.label}
          </Button>
        ))}
      </div>
      <p className="text-xs leading-5 text-gray-500">
        크루라면 가입과 함께 크루 인증을 신청해, 가입 후 따로 신청하지 않아도 돼요.
      </p>
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );
}

/** 크루로 가입한 뒤 보여 주는 완료 안내. 인증은 신청만 됐고 승인 전이라는 점을 분명히 한다. */
function SignupCompleted({
  verificationResult,
  onComplete,
}: {
  verificationResult: VerificationResult;
  onComplete: () => void;
}) {
  if (verificationResult === 'failed') {
    return (
      <div className="w-full rounded-xl border border-gray-200 p-6 text-center md:p-8">
        <h1 className="text-xl font-bold">가입이 완료됐어요.</h1>
        <p role="alert" className="mt-2 text-sm leading-relaxed text-gray-600">
          다만 크루 인증 신청은 접수되지 않았어요. 구성원 인증 화면에서 다시 신청해 주세요.
        </p>
        <div className="mt-6 flex flex-col gap-2">
          <Link to="/mypage/verification" className={getButtonStyles({ size: 'lg' })}>
            크루 인증 다시 신청하기
          </Link>
          <Button variant="ghost" onClick={onComplete}>
            홈으로 가기
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full rounded-xl border border-gray-200 p-6 text-center md:p-8">
      <h1 className="text-xl font-bold">가입이 완료됐어요.</h1>
      <p className="mt-4 rounded-lg bg-gray-100 p-4 text-sm leading-relaxed text-gray-700">
        <strong className="font-semibold">
          크루 인증 신청이 접수됐고, 지금은 승인 대기 중이에요.
        </strong>
        <br />
        운영진이 확인해 승인하기 전까지는 크루 인증이 완료되지 않아요. 승인되면 프로젝트를 등록할 수
        있어요.
      </p>
      <Button size="lg" className="mt-6 w-full" onClick={onComplete}>
        홈으로 가기
      </Button>
    </div>
  );
}
