import { Suspense, useId, useState } from 'react';
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { IconCheck, IconX } from '@tabler/icons-react';

import { handleAvailabilityQuery, sessionQuery, signupMutation } from '@/apis/session';
import {
  createVerificationRequestMutation,
  verificationRequestQuery,
  type VerificationTrack,
  type VerificationUserType,
} from '@/apis/verification';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Button, getButtonStyles } from '@/components/Button';
import { Field } from '@/components/Field';
import { Footer } from '@/components/Footer';
import { Input } from '@/components/Input';
import { CrewInfoFields } from '@/components/users/CrewInfoFields';
import { ProfileIntroFields } from '@/components/users/ProfileIntroFields';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { getGithubLoginUrl } from '@/utils/auth';
import { getApiErrorMessage, isApiResponseError } from '@/utils/error';
import { analytics, toPathPattern } from '@/utils/analytics';
import { cn } from '@/utils/cn';
import {
  toProfileIntroBody,
  validateProfileIntro,
  type ProfileIntroErrors,
  type ProfileIntroInput,
} from '@/utils/user';

interface SignupPageProps {
  onComplete: () => void;
}

interface SignupErrors extends ProfileIntroErrors {
  handle?: string;
  displayName?: string;
  memberType?: string;
  cohort?: string;
  track?: string;
}

/** 우테코 구성원 여부. 크루나 코치라면 가입하면서 우아한테크코스 소속 인증까지 신청한다. */
type MemberType = VerificationUserType | 'GENERAL';

const MEMBER_LABEL: Record<VerificationUserType, string> = {
  WOOWACOURSE_CREW: '크루',
  WOOWACOURSE_COACH: '코치',
};

/** 크루나 코치로 가입했을 때 이어서 보낸 인증 신청의 결과 */
type VerificationResult = { status: 'requested' | 'failed'; userType: VerificationUserType };

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
  const availability = useHandleAvailability(handleName);
  const [displayName, setDisplayName] = useState('');
  // 소개와 주소는 선택 항목이라 비워 두면 null로 보낸다. 나중에 프로필 수정에서 바꿀 수 있다.
  const [intro, setIntro] = useState<ProfileIntroInput>({
    bio: '',
    githubProfileUrl: '',
    blogUrl: '',
  });
  const [memberType, setMemberType] = useState<MemberType | null>(null);
  const crew = memberType === 'WOOWACOURSE_CREW';
  const member = memberType !== null && memberType !== 'GENERAL';
  const [cohort, setCohort] = useState<string | null>(null);
  const [track, setTrack] = useState<VerificationTrack | null>(null);
  const [errors, setErrors] = useState<SignupErrors>({});
  // 가입과 인증 신청을 이어서 보내는 동안 입력을 막는다. 두 요청 사이에도 풀리지 않게 따로 둔다.
  // 어느 버튼으로 가입 중인지. 'skip'은 크루나 코치지만 인증 신청 없이 가입만 하는 경우다.
  const [submitMode, setSubmitMode] = useState<'verify' | 'skip' | null>(null);
  const submitting = submitMode !== null;
  const [verificationResult, setVerificationResult] = useState<VerificationResult | null>(null);

  function validate(withVerification: boolean) {
    const next: SignupErrors = validateProfileIntro(intro);
    if (!HANDLE_FORMAT.test(handleName)) {
      next.handle = '2~30자의 영문, 숫자, 밑줄, 하이픈으로 입력해 주세요.';
    } else if (availability === 'taken') {
      next.handle = HANDLE_TAKEN_MESSAGE;
    }
    if (!displayName.trim()) next.displayName = '닉네임을 입력해 주세요.';
    else if (Array.from(displayName).length > 50) {
      next.displayName = '닉네임은 50자 이하로 입력해 주세요.';
    }
    if (memberType === null) next.memberType = '우테코 크루나 코치인지 선택해 주세요.';
    if (withVerification && crew && !cohort) next.cohort = '기수를 선택해 주세요.';
    if (withVerification && crew && !track) next.track = '트랙을 선택해 주세요.';
    return next;
  }

  async function submit({ withVerification }: { withVerification: boolean }) {
    const nextErrors = validate(withVerification);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setSubmitMode(withVerification ? 'verify' : 'skip');
    onSubmit();
    try {
      await mutation.mutateAsync({
        handle: `@${handleName}`,
        displayName: displayName.trim(),
        ...toProfileIntroBody(intro),
      });
      analytics.track({ name: 'signup_submitted' });
      // 가입하면 서버가 인증된 세션을 새로 만든다. 인증 신청에 쓸 CSRF 토큰도 여기서 다시 받는다.
      await queryClient.fetchQuery(sessionQuery);
    } catch (error) {
      setSubmitMode(null);
      analytics.track({
        name: 'signup_failed',
        reason: isApiResponseError(error) ? error.data.code : 'UNKNOWN',
      });
      if (isHandleTakenError(error)) {
        // 확인한 뒤 가입하기 전에 다른 사람이 먼저 가져간 경우다.
        setErrors({ handle: HANDLE_TAKEN_MESSAGE });
      } else if (isApiResponseError(error)) {
        const fieldErrors = Object.fromEntries(
          (error.data.details ?? []).map((detail) => [detail.field, detail.message]),
        );
        setErrors(fieldErrors);
      }
      return;
    }

    if (!withVerification || memberType === null || memberType === 'GENERAL') {
      onComplete();
      return;
    }

    // 가입이 끝난 뒤에만 인증을 신청한다. 실패해도 가입은 되돌리지 않고 다시 신청하도록 안내한다.
    // 코치는 기수·트랙이 없어서 서버 규칙대로 null로 보낸다.
    const course = crew ? { cohort: Number(cohort), track } : { cohort: null, track: null };
    try {
      const created = await verification.mutateAsync({
        userType: memberType,
        nickname: displayName.trim(),
        ...course,
      });
      queryClient.setQueryData(verificationRequestQuery.queryKey, created);
      analytics.track({ name: 'verification_requested', userType: memberType, ...course });
      setVerificationResult({ status: 'requested', userType: memberType });
    } catch {
      setVerificationResult({ status: 'failed', userType: memberType });
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
        aria-describedby={member ? policyId : undefined}
        className="mt-7 flex flex-col gap-5"
        // 주소 칸이 type="url"이라 브라우저 기본 검사 대신 validateProfileIntro로 검사한다.
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          void submit({ withVerification: true });
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
                {member && (
                  <li>
                    우아한테크코스 소속 인증에도 이 닉네임을 써요. 우테코에서 쓰는 닉네임을 입력해
                    주세요.
                  </li>
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
                    aria-describedby={`${id}-help ${id}-rules ${id}-note ${id}-preview`}
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
              </ul>
              <HandleRules id={`${id}-rules`} handleName={handleName} availability={availability} />
              <ul id={`${id}-note`} className="list-disc pl-4 text-xs leading-5 text-gray-500">
                <li>GitHub 아이디와 달라도 괜찮아요.</li>
              </ul>
            </>
          )}
        </Field>
        <ProfileIntroFields
          value={intro}
          onChange={setIntro}
          errors={errors}
          disabled={submitting}
          optional
        />
        <MemberQuestion
          value={memberType}
          error={errors.memberType}
          disabled={submitting}
          onChange={(next) => {
            setMemberType(next);
            setErrors((previous) => ({ ...previous, memberType: undefined }));
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
        {memberType === 'WOOWACOURSE_COACH' && (
          <p id={policyId} className="text-xs leading-5 text-gray-600">
            코치 인증 이후에는{' '}
            <strong className="font-semibold">닉네임을 수정할 수 없습니다.</strong>
          </p>
        )}
        {mutation.isError && !isHandleTakenError(mutation.error) && (
          <p role="alert" className="text-sm text-red-600">
            {getApiErrorMessage(mutation.error)}
          </p>
        )}
        <Button type="submit" size="lg" className="mt-1 w-full" disabled={submitting}>
          {submitMode === 'verify'
            ? '가입 중…'
            : member
              ? `가입하고 ${MEMBER_LABEL[memberType]} 인증 신청하기`
              : '가입하기'}
        </Button>
        {member && (
          // 인증은 나중에 우아한테크코스 소속 인증 화면에서 따로 신청할 수 있다.
          <Button
            variant="ghost"
            size="lg"
            className="-mt-3 w-full"
            disabled={submitting}
            onClick={() => void submit({ withVerification: false })}
          >
            {submitMode === 'skip' ? '가입 중…' : '인증 없이 가입만 하기'}
          </Button>
        )}
      </form>
    </div>
  );
}

/** 서버 `Handle.HANDLE_FORMAT_REGEX`(`^@[A-Za-z0-9_-]{2,30}$`)에서 @ 뒤 부분과 같은 규칙 */
const HANDLE_FORMAT = /^[A-Za-z0-9_-]{2,30}$/;

const HANDLE_TAKEN_MESSAGE = '이미 사용 중인 아이디예요.';

const isHandleTakenError = (error: unknown) =>
  isApiResponseError(error) && error.data.code === 'HANDLE_ALREADY_EXISTS';

/**
 * 아이디 중복 확인 결과.
 *
 * - `idle`: 형식이 맞지 않아 아직 묻지 않았다
 * - `checking`: 입력이 멈추기를 기다리거나 서버에 묻는 중이다
 * - `unknown`: 확인하지 못했다. 가입할 때 서버가 다시 확인하므로 막지는 않는다
 */
type HandleAvailability = 'idle' | 'checking' | 'available' | 'taken' | 'unknown';

/** 형식이 맞는 아이디만, 입력이 0.4초 멈추면 서버에 쓸 수 있는지 묻는다. */
function useHandleAvailability(handleName: string): HandleAvailability {
  const debounced = useDebouncedValue(handleName, 400);
  const query = useQuery({
    ...handleAvailabilityQuery(`@${debounced}`),
    enabled: HANDLE_FORMAT.test(debounced),
  });

  if (!HANDLE_FORMAT.test(handleName)) return 'idle';
  if (handleName !== debounced || query.isPending) return 'checking';
  if (query.isError) return 'unknown';
  return query.data ? 'available' : 'taken';
}

const AVAILABILITY_TEXT: Record<HandleAvailability, string> = {
  idle: '다른 사람이 쓰지 않는 아이디',
  checking: '사용할 수 있는지 확인하는 중…',
  available: '사용할 수 있는 아이디예요.',
  taken: HANDLE_TAKEN_MESSAGE,
  unknown: '사용할 수 있는지 확인하지 못했어요. 가입할 때 다시 확인해요.',
};

/** 서버 `Handle.HANDLE_FORMAT_REGEX`(`^@[A-Za-z0-9_-]{2,30}$`)를 두 조건으로 나눈 것 */
const HANDLE_RULES = [
  {
    label: '영문·숫자·밑줄(_)·하이픈(-)만 사용',
    test: (value: string) => /^[A-Za-z0-9_-]+$/.test(value),
  },
  { label: '2~30자', test: (value: string) => value.length >= 2 && value.length <= 30 },
];

/**
 * 아이디 규칙을 입력값으로 바로 확인해 보여 준다.
 *
 * 아직 입력하지 않았으면 회색으로 두고, 입력을 시작하면 통과 여부를 색과 아이콘, 숨은 문구로 알린다.
 * 마지막 줄은 형식이 맞을 때 서버에 물어본 중복 확인 결과다.
 */
function HandleRules({
  id,
  handleName,
  availability,
}: {
  id: string;
  handleName: string;
  availability: HandleAvailability;
}) {
  return (
    <ul id={id} aria-label="아이디 규칙" className="flex flex-col gap-1 text-xs leading-5">
      {HANDLE_RULES.map((rule) => {
        const state = !handleName ? 'idle' : rule.test(handleName) ? 'passed' : 'failed';
        const Icon = state === 'failed' ? IconX : IconCheck;

        return (
          <li
            key={rule.label}
            className={cn(
              'flex items-center gap-1',
              state === 'idle' && 'text-gray-500',
              state === 'passed' && 'text-green-600',
              state === 'failed' && 'text-red-600',
            )}
          >
            <Icon className="size-3.5 shrink-0" aria-hidden="true" />
            {rule.label}
            {state !== 'idle' && (
              <span className="sr-only">{state === 'passed' ? ' 충족' : ' 미충족'}</span>
            )}
          </li>
        );
      })}
      {/* 서버 응답이 늦게 오므로 바뀔 때 읽어 주도록 status로 둔다. */}
      <li
        role="status"
        className={cn(
          'flex items-center gap-1',
          availability === 'available' && 'text-green-600',
          availability === 'taken' && 'text-red-600',
          !['available', 'taken'].includes(availability) && 'text-gray-500',
        )}
      >
        {availability === 'taken' ? (
          <IconX className="size-3.5 shrink-0" aria-hidden="true" />
        ) : (
          <IconCheck className="size-3.5 shrink-0" aria-hidden="true" />
        )}
        {AVAILABILITY_TEXT[availability]}
      </li>
    </ul>
  );
}

const MEMBER_OPTIONS: { value: MemberType; label: string }[] = [
  { value: 'WOOWACOURSE_CREW', label: '크루예요' },
  { value: 'WOOWACOURSE_COACH', label: '코치예요' },
  { value: 'GENERAL', label: '아니요' },
];

/**
 * 우테코 구성원 여부. 크루나 코치라면 가입하면서 우아한테크코스 소속 인증까지 신청한다.
 *
 * 처음에는 아무것도 고르지 않은 상태로 두고, 가입 전에 꼭 고르게 한다.
 */
function MemberQuestion({
  value,
  error,
  disabled,
  onChange,
}: {
  value: MemberType | null;
  error?: string;
  disabled?: boolean;
  onChange: (memberType: MemberType) => void;
}) {
  const labelId = useId();

  return (
    <div className="flex flex-col gap-2">
      <p id={labelId} className="text-sm font-medium text-gray-900">
        우테코 크루나 코치이신가요?
      </p>
      <div role="group" aria-labelledby={labelId} className="grid grid-cols-3 gap-2">
        {MEMBER_OPTIONS.map((option) => (
          <Button
            key={option.value}
            variant={value === option.value ? 'primary' : 'outline'}
            aria-pressed={value === option.value}
            disabled={disabled}
            onClick={() => onChange(option.value)}
          >
            {option.label}
          </Button>
        ))}
      </div>
      <p className="text-xs leading-5 text-gray-500">
        크루나 코치라면 가입과 함께 우아한테크코스 소속 인증을 신청해, 가입 후 따로 신청하지 않아도
        돼요.
      </p>
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );
}

/** 크루나 코치로 가입한 뒤 보여 주는 완료 안내. 인증은 신청만 됐고 승인 전이라는 점을 분명히 한다. */
function SignupCompleted({
  verificationResult,
  onComplete,
}: {
  verificationResult: VerificationResult;
  onComplete: () => void;
}) {
  const label = MEMBER_LABEL[verificationResult.userType];

  if (verificationResult.status === 'failed') {
    return (
      <div className="w-full rounded-xl border border-gray-200 p-6 text-center md:p-8">
        <h1 className="text-xl font-bold">가입이 완료됐어요.</h1>
        <p role="alert" className="mt-2 text-sm leading-relaxed text-gray-600">
          다만 {label} 인증 신청은 접수되지 않았어요. 우아한테크코스 소속 인증 화면에서 다시 신청해
          주세요.
        </p>
        <div className="mt-6 flex flex-col gap-2">
          <Link to="/mypage/verification" className={getButtonStyles({ size: 'lg' })}>
            {label} 인증 다시 신청하기
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
          {label} 인증 신청이 접수됐고, 지금은 승인 대기 중이에요.
        </strong>
        <br />
        운영진이 확인해 승인하기 전까지는 {label} 인증이 완료되지 않아요. 승인되면 프로젝트를 등록할
        수 있어요.
      </p>
      <Button size="lg" className="mt-6 w-full" onClick={onComplete}>
        홈으로 가기
      </Button>
    </div>
  );
}
