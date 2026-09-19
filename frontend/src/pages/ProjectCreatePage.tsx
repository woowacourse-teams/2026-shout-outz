import { useState, type ReactNode } from 'react';
import { useMutation, useSuspenseQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import { cohortsQueryOptions, createProjectMutationOptions } from '@/api/project';
import { Button, getButtonStyles } from '@/components/Button';
import { Field } from '@/components/Field';
import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { Input } from '@/components/Input';
import { MemberField } from '@/components/projects/MemberField';
import { Select } from '@/components/Select';
import { TechTagField } from '@/components/projects/TechTagField';
import { ThumbnailField } from '@/components/projects/ThumbnailField';
import { type ProjectFormErrors, type ProjectFormValues } from '@/types/project';
import { toProjectCreateRequest, validateProjectForm } from '@/utils/project';
import { sessionQuery } from '@/apis/session';
import { verificationRequestQuery } from '@/apis/verification';
import { getGithubLoginUrl } from '@/utils/auth';

const EMPTY_FORM: ProjectFormValues = {
  title: '',
  teamName: '',
  tagline: '',
  cohort: null,
  thumbnailMediaId: null,
  githubRepositoryUrl: '',
  deploymentUrl: '',
  descriptionMd: '',
  techTags: [],
  memberHandles: [],
};

/**
 * 프로젝트 등록 페이지.
 *
 * 폼 값과 제출을 담당한다. 필드 종류는 `POST /api/v1/projects` 명세를 따르고, 각 필드의 모양은
 * 디자인을 따른다. 디자인에만 있던 "구분(카테고리)"은 명세에 없어 넣지 않는다.
 *
 * 제출하면 `validateProjectForm`으로 검사하고, 통과하면 `createProject`를 호출한다.
 * 등록에 성공하면 폼 대신 완료 안내와 프로젝트 목록으로 가는 버튼을 보여준다.
 */
export function ProjectCreatePage() {
  const { data: session } = useSuspenseQuery(sessionQuery);

  if (session.status === 'UNAUTHENTICATED') {
    return (
      <ProjectCreateGuard
        title="로그인이 필요해요."
        description="프로젝트를 등록하려면 먼저 GitHub로 로그인해 주세요."
        action={
          <a href={getGithubLoginUrl()} className={getButtonStyles({})}>
            GitHub 로그인
          </a>
        }
      />
    );
  }
  if (session.status === 'SIGNUP_REQUIRED') {
    return (
      <ProjectCreateGuard
        title="가입을 먼저 완료해 주세요."
        description="프로필을 만든 뒤 프로젝트 등록 자격을 확인할 수 있습니다."
        action={
          <Link to="/signup" className={getButtonStyles({})}>
            가입 계속하기
          </Link>
        }
      />
    );
  }
  return <VerifiedProjectCreatePage />;
}

function VerifiedProjectCreatePage() {
  const { data: verification } = useSuspenseQuery(verificationRequestQuery);

  if (verification?.status !== 'APPROVED') {
    const description =
      verification?.status === 'PENDING'
        ? '구성원 인증 신청을 검토하고 있습니다. 승인 후 프로젝트를 등록할 수 있습니다.'
        : verification?.status === 'REJECTED'
          ? `구성원 인증이 반려되었습니다.${verification.reason ? ` ${verification.reason}` : ''}`
          : '우아한테크코스 구성원 인증을 받은 뒤 프로젝트를 등록할 수 있습니다.';
    return (
      <ProjectCreateGuard
        title="구성원 인증이 필요해요."
        description={description}
        action={
          verification?.status !== 'PENDING' ? (
            <Link to="/mypage/verification" className={getButtonStyles({})}>
              구성원 인증 신청
            </Link>
          ) : undefined
        }
      />
    );
  }

  return <ProjectCreateForm />;
}

function ProjectCreateForm() {
  const { data: cohorts } = useSuspenseQuery(cohortsQueryOptions());
  const [values, setValues] = useState<ProjectFormValues>(EMPTY_FORM);
  const [errors, setErrors] = useState<ProjectFormErrors>({});
  const createProject = useMutation(createProjectMutationOptions);

  const setField = <Key extends keyof ProjectFormValues>(
    field: Key,
    value: ProjectFormValues[Key],
  ) => setValues((current) => ({ ...current, [field]: value }));

  const submit = () => {
    const nextErrors = validateProjectForm(values);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    createProject.mutate(toProjectCreateRequest(values));
  };

  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>프로젝트 등록 | shout-outz</title>
      <AppGnb />

      <main className="mx-auto w-full max-w-3xl flex-1 px-4 pt-6 pb-12 md:pt-10 md:pb-20">
        {createProject.isSuccess ? (
          <RegistrationComplete />
        ) : (
          <>
            <header className="flex flex-col gap-2">
              <h1 className="text-xl font-bold md:text-2xl">프로젝트 등록</h1>
              <p className="text-sm leading-relaxed text-gray-500">
                우아한테크코스 크루들과 함께 제작한 멋진 프로젝트를 아카이브에 등록해 보세요.
              </p>
            </header>

            <form
              className="mt-8 flex flex-col gap-6"
              onSubmit={(event) => {
                event.preventDefault();
                submit();
              }}
            >
              <Field label="프로젝트 이름 *" error={errors.title}>
                {(id) => (
                  <Input
                    id={id}
                    value={values.title}
                    onChange={(event) => setField('title', event.target.value)}
                    placeholder="루프 (Loop)"
                  />
                )}
              </Field>

              <Field label="팀 이름" error={errors.teamName}>
                {(id) => (
                  <Input
                    id={id}
                    value={values.teamName}
                    onChange={(event) => setField('teamName', event.target.value)}
                    placeholder="루프팀"
                  />
                )}
              </Field>

              <Field label="한 줄 소개 *" error={errors.tagline}>
                {(id) => (
                  <textarea
                    id={id}
                    rows={2}
                    value={values.tagline}
                    onChange={(event) => setField('tagline', event.target.value)}
                    placeholder="어떤 문제를 어떻게 풀었는지 한 문장으로 소개해 주세요."
                    className="focus-visible:outline-primary-600 w-full resize-none rounded-lg bg-gray-100 px-4 py-3 text-sm text-gray-900 outline-none placeholder:text-gray-500 focus-visible:outline-2"
                  />
                )}
              </Field>

              <Field label="우테코 기수 *" error={errors.cohort}>
                {(id) => (
                  <Select
                    aria-labelledby={`${id}-label`}
                    placeholder="기수를 선택하세요"
                    value={values.cohort === null ? null : String(values.cohort)}
                    onValueChange={(value) => setField('cohort', Number(value))}
                  >
                    {cohorts.map(({ cohort, year }) => (
                      <Select.Item key={cohort} value={String(cohort)}>
                        {`${cohort}기 (${year})`}
                      </Select.Item>
                    ))}
                  </Select>
                )}
              </Field>

              <ThumbnailField
                value={values.thumbnailMediaId}
                onChange={(mediaId) => setField('thumbnailMediaId', mediaId)}
                error={errors.thumbnailMediaId}
              />

              <Field label="GitHub 레포지토리 URL *" error={errors.githubRepositoryUrl}>
                {(id) => (
                  <Input
                    id={id}
                    value={values.githubRepositoryUrl}
                    onChange={(event) => setField('githubRepositoryUrl', event.target.value)}
                    placeholder="https://github.com/woowacourse-teams/2026-loop"
                  />
                )}
              </Field>

              <Field label="서비스 배포 URL" error={errors.deploymentUrl}>
                {(id) => (
                  <Input
                    id={id}
                    value={values.deploymentUrl}
                    onChange={(event) => setField('deploymentUrl', event.target.value)}
                    placeholder="https://loop.team"
                  />
                )}
              </Field>

              <Field label="상세 설명" error={errors.descriptionMd}>
                {(id) => (
                  <textarea
                    id={id}
                    rows={8}
                    value={values.descriptionMd}
                    onChange={(event) => setField('descriptionMd', event.target.value)}
                    placeholder="Markdown으로 프로젝트를 자세히 소개해 주세요."
                    className="focus-visible:outline-primary-600 w-full rounded-lg bg-gray-100 px-4 py-3 text-sm text-gray-900 outline-none placeholder:text-gray-500 focus-visible:outline-2"
                  />
                )}
              </Field>

              <div className="flex flex-col gap-2">
                <p className="text-sm font-medium text-gray-900">기술 스택 *</p>
                <TechTagField
                  value={values.techTags}
                  onChange={(techTags) => setField('techTags', techTags)}
                  error={errors.techTags}
                />
              </div>

              <div className="flex flex-col gap-2">
                <p className="text-sm font-medium text-gray-900">참여 팀원 *</p>
                <MemberField
                  value={values.memberHandles}
                  onChange={(handles) => setField('memberHandles', handles)}
                  error={errors.memberHandles}
                />
              </div>

              {createProject.isError && (
                <p role="alert" className="text-sm text-red-600">
                  프로젝트 등록에 실패했습니다.
                </p>
              )}

              <Button type="submit" size="lg" disabled={createProject.isPending}>
                프로젝트 등록하기
              </Button>
            </form>
          </>
        )}
      </main>

      <Footer />
    </div>
  );
}

function ProjectCreateGuard({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>프로젝트 등록 | shout-outz</title>
      <AppGnb />
      <main className="mx-auto flex w-full max-w-xl flex-1 items-center px-4 py-12">
        <section className="w-full rounded-xl border border-gray-200 p-6 text-center md:p-8">
          <h1 className="text-xl font-bold">{title}</h1>
          <p className="mt-2 text-sm leading-relaxed text-gray-600">{description}</p>
          {action && <div className="mt-6">{action}</div>}
        </section>
      </main>
      <Footer />
    </div>
  );
}

function RegistrationComplete() {
  return (
    <div className="flex flex-col items-center gap-4 py-20 text-center">
      <h1 className="text-xl font-bold md:text-2xl">등록이 완료됐어요.</h1>
      <p className="text-sm leading-relaxed text-gray-500">
        운영진 승인이 끝나면 아카이브에서 볼 수 있어요.
      </p>
      <Link
        to="/projects"
        className="bg-primary-600 focus-visible:outline-primary-600 mt-2 rounded-lg px-5 py-3 text-sm font-medium text-white focus-visible:outline-2 focus-visible:outline-offset-2"
      >
        프로젝트 목록으로
      </Link>
    </div>
  );
}
