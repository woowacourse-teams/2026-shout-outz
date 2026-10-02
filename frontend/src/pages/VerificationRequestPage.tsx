import { useState, type ReactNode } from 'react';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import {
  createVerificationRequestMutation,
  verificationRequestQuery,
  type VerificationTrack,
  type VerificationUserType,
} from '@/apis/verification';
import { sessionQuery } from '@/apis/session';
import { cohortsQueryOptions } from '@/api/project';
import { AppGnb } from '@/components/AppGnb';
import { Button, getButtonStyles } from '@/components/Button';
import { Field } from '@/components/Field';
import { Footer } from '@/components/Footer';
import { Input } from '@/components/Input';
import { Select } from '@/components/Select';
import { getGithubLoginUrl } from '@/utils/auth';
import { getApiErrorMessage } from '@/utils/error';
import { analytics, toPathPattern } from '@/utils/analytics';

export function VerificationRequestPage() {
  const { data: session } = useSuspenseQuery(sessionQuery);

  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>구성원 인증 | shout-outz</title>
      <AppGnb />
      <main className="mx-auto w-full max-w-xl flex-1 px-4 py-10 md:py-16">
        {session.status === 'AUTHENTICATED' ? (
          <VerificationContent />
        ) : session.status === 'SIGNUP_REQUIRED' ? (
          <VerificationMessage
            title="가입을 먼저 완료해 주세요."
            description="서비스에서 사용할 프로필을 만든 뒤 구성원 인증을 신청할 수 있습니다."
            action={
              <Link to="/signup" className={getButtonStyles({})}>
                가입 계속하기
              </Link>
            }
          />
        ) : (
          <VerificationMessage
            title="로그인이 필요해요."
            description="GitHub 로그인 후 우아한테크코스 구성원 인증을 신청할 수 있습니다."
            action={
              <a
                href={getGithubLoginUrl()}
                onClick={() =>
                  analytics.track({
                    name: 'login_started',
                    from: toPathPattern(window.location.pathname),
                  })
                }
                className={getButtonStyles({})}
              >
                GitHub 로그인
              </a>
            }
          />
        )}
      </main>
      <Footer />
    </div>
  );
}

function VerificationContent() {
  const { data: request } = useSuspenseQuery(verificationRequestQuery);

  if (request?.status === 'APPROVED') {
    return (
      <VerificationMessage
        title="구성원 인증이 완료됐어요."
        description="이제 프로젝트를 등록할 수 있습니다."
        action={
          <Link to="/projects/new" className={getButtonStyles({})}>
            프로젝트 등록하기
          </Link>
        }
      />
    );
  }

  if (request?.status === 'PENDING') {
    return (
      <VerificationMessage
        title="인증 신청을 검토하고 있어요."
        description={`${request.nickname}님의 신청을 운영진이 확인하고 있습니다.`}
      />
    );
  }

  return <VerificationForm rejectionReason={request?.reason ?? null} />;
}

function VerificationForm({ rejectionReason }: { rejectionReason: string | null }) {
  const client = useQueryClient();
  const { data: cohorts } = useSuspenseQuery(cohortsQueryOptions());
  const mutation = useMutation({
    ...createVerificationRequestMutation,
    onSuccess: (createdRequest) => {
      client.setQueryData(verificationRequestQuery.queryKey, createdRequest);
    },
  });
  const [userType, setUserType] = useState<VerificationUserType>('WOOWACOURSE_CREW');
  const [nickname, setNickname] = useState('');
  const [cohort, setCohort] = useState<string | null>(null);
  const [track, setTrack] = useState<VerificationTrack | null>(null);
  const crew = userType === 'WOOWACOURSE_CREW';
  const invalid = !nickname.trim() || (crew && (!cohort || !track));

  return (
    <section className="rounded-xl border border-gray-200 p-6 md:p-8">
      <h1 className="text-2xl font-bold">우테코 구성원 인증</h1>
      <p className="mt-2 text-sm leading-relaxed text-gray-600">
        프로젝트 등록 권한을 위해 운영진이 확인할 정보를 입력해 주세요.
      </p>
      {rejectionReason && (
        <p role="alert" className="mt-5 rounded-lg bg-red-50 p-4 text-sm text-red-700">
          이전 신청 반려 사유: {rejectionReason}
        </p>
      )}
      <form
        className="mt-7 flex flex-col gap-5"
        onSubmit={(event) => {
          event.preventDefault();
          if (invalid || mutation.isPending) return;
          analytics.track({
            name: 'verification_requested',
            userType,
            track: crew ? track : null,
            cohort: crew ? Number(cohort) : null,
          });
          mutation.mutate({
            userType,
            nickname: nickname.trim(),
            cohort: crew ? Number(cohort) : null,
            track: crew ? track : null,
          });
        }}
      >
        <Field label="구성원 유형">
          {(id) => (
            <Select
              id={id}
              value={userType}
              onValueChange={(value) => setUserType(value as VerificationUserType)}
            >
              <Select.Item value="WOOWACOURSE_CREW">크루</Select.Item>
              <Select.Item value="WOOWACOURSE_COACH">코치</Select.Item>
            </Select>
          )}
        </Field>
        <Field label="우테코 닉네임">
          {(id) => (
            <Input
              id={id}
              value={nickname}
              maxLength={50}
              onChange={(event) => setNickname(event.target.value)}
              placeholder="닉네임"
            />
          )}
        </Field>
        {crew && (
          <>
            <Field label="기수">
              {(id) => (
                <Select
                  id={id}
                  value={cohort}
                  placeholder="기수를 선택하세요"
                  onValueChange={setCohort}
                >
                  {cohorts.map((item) => (
                    <Select.Item key={item.cohort} value={String(item.cohort)}>
                      {item.cohort}기 ({item.year})
                    </Select.Item>
                  ))}
                </Select>
              )}
            </Field>
            <Field label="트랙">
              {(id) => (
                <Select
                  id={id}
                  value={track}
                  placeholder="트랙을 선택하세요"
                  onValueChange={(value) => setTrack(value as VerificationTrack)}
                >
                  <Select.Item value="BACKEND">백엔드</Select.Item>
                  <Select.Item value="FRONTEND">프론트엔드</Select.Item>
                  <Select.Item value="ANDROID">안드로이드</Select.Item>
                </Select>
              )}
            </Field>
          </>
        )}
        {mutation.isError && (
          <p role="alert" className="text-sm text-red-600">
            {getApiErrorMessage(mutation.error)}
          </p>
        )}
        <Button type="submit" size="lg" disabled={invalid || mutation.isPending}>
          {mutation.isPending ? '신청 중…' : rejectionReason ? '다시 신청하기' : '인증 신청하기'}
        </Button>
      </form>
    </section>
  );
}

function VerificationMessage({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <section className="rounded-xl border border-gray-200 p-6 text-center md:p-8">
      <h1 className="text-xl font-bold">{title}</h1>
      <p className="mt-2 text-sm leading-relaxed text-gray-600">{description}</p>
      {action && <div className="mt-6">{action}</div>}
    </section>
  );
}
