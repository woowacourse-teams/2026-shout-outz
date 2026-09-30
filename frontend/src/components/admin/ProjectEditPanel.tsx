import { useState, type ReactNode } from 'react';
import {
  useMutation,
  useQueryClient,
  useSuspenseInfiniteQuery,
  useSuspenseQuery,
} from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { IconAlertTriangle, IconChevronLeft } from '@tabler/icons-react';

import {
  adminProjectDetailQuery,
  adminProjectsQuery,
  updateAdminProjectMutation,
} from '@/apis/admin';
import { cohortsQueryOptions } from '@/api/project';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { Field } from '@/components/Field';
import { Image } from '@/components/Image';
import { Input } from '@/components/Input';
import { Select } from '@/components/Select';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { ReviewStatusTab } from '@/components/admin/ReviewStatusTab';
import { TechTagField } from '@/components/projects/TechTagField';
import { ThumbnailField } from '@/components/projects/ThumbnailField';
import { REVIEW_STATUS_LABELS } from '@/constants/admin';
import type {
  AdminProject,
  AdminProjectDetail,
  AdminProjectStatus,
  AdminProjectUpdateBody,
} from '@/types/admin';
import {
  toAdminProjectFormErrors,
  toAdminProjectFormValues,
  toAdminProjectUpdateBody,
  validateAdminProjectForm,
  type AdminProjectFormErrors,
  type AdminProjectFormValues,
} from '@/utils/admin-project';
import { getApiErrorMessage } from '@/utils/error';
import { toProjectSlugParam } from '@/utils/project';

/** 저장 막대에 "무엇이 바뀌었는지" 보여줄 때 쓰는 이름. */
const FIELD_LABELS: Record<keyof AdminProjectUpdateBody, string> = {
  title: '프로젝트 이름',
  teamName: '팀 이름',
  tagline: '한 줄 소개',
  cohort: '기수',
  slug: 'slug',
  thumbnailImageId: '썸네일',
  githubRepositoryUrl: 'GitHub URL',
  deploymentUrl: '배포 URL',
  descriptionMd: '상세 설명',
  serviceStatus: '운영 상태',
  approvalStatus: '승인 상태',
  techTagIds: '기술 스택',
  createdAt: '등록 시각',
  updatedAt: '수정 시각',
  starCount: 'GitHub star 수',
  starSyncedAt: 'star 동기화 시각',
  viewCount: '조회수',
};

const TEXTAREA_CLASS =
  'focus-visible:outline-primary-600 w-full rounded-lg bg-gray-100 px-4 py-3 text-sm text-gray-900 outline-none placeholder:text-gray-500 focus-visible:outline-2';

/**
 * 등록된 프로젝트를 관리자가 직접 고치는 탭.
 *
 * 목록에서 프로젝트를 고르면 같은 자리에 수정 폼을 연다. 작성자가 아니어도 고칠 수 있고,
 * 승인 상태 전이 검사도 거치지 않으므로 저장하면 바로 공개 화면에 반영된다.
 */
export function ProjectEditPanel() {
  const [status, setStatus] = useState<AdminProjectStatus>('APPROVED');
  const [editingId, setEditingId] = useState<number | null>(null);

  if (editingId !== null) {
    return (
      <AsyncBoundary>
        <ProjectEditor projectId={editingId} onBack={() => setEditingId(null)} />
      </AsyncBoundary>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <ReviewStatusTab value={status} onChange={setStatus} />
      <AsyncBoundary>
        <ProjectList status={status} onEdit={setEditingId} />
      </AsyncBoundary>
    </div>
  );
}

function ProjectList({
  status,
  onEdit,
}: {
  status: AdminProjectStatus;
  onEdit: (projectId: number) => void;
}) {
  const { data, hasNextPage, fetchNextPage, isFetchingNextPage } = useSuspenseInfiniteQuery(
    adminProjectsQuery(status),
  );
  const items = data.pages.flatMap((page) => page.items);

  if (items.length === 0) {
    return (
      <p className="py-10 text-center text-sm text-gray-500">
        {REVIEW_STATUS_LABELS[status]} 상태의 프로젝트가 없습니다.
      </p>
    );
  }

  return (
    <>
      <ul className="flex flex-col divide-y divide-gray-200 rounded-xl border border-gray-200">
        {items.map((item) => (
          <ProjectRow key={item.id} item={item} onEdit={() => onEdit(item.id)} />
        ))}
      </ul>
      {hasNextPage && (
        <Button
          variant="outline"
          className="self-center"
          disabled={isFetchingNextPage}
          onClick={() => fetchNextPage()}
        >
          더 보기
        </Button>
      )}
    </>
  );
}

function ProjectRow({ item, onEdit }: { item: AdminProject; onEdit: () => void }) {
  return (
    <li className="flex flex-col gap-3 p-4 md:flex-row md:items-center md:justify-between">
      <div className="flex min-w-0 flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-bold">{item.title}</span>
          <Badge tone="primary">{item.cohort}기</Badge>
          <span className="text-xs text-gray-500">@{item.slug}</span>
        </div>
        <p className="truncate text-sm text-gray-600">{item.tagline}</p>
        <p className="text-xs text-gray-500">
          {item.members.map((member) => member.displayName).join(', ')}
        </p>
      </div>
      <Button
        variant="outline"
        size="sm"
        className="shrink-0 self-start md:self-center"
        aria-label={`${item.title} 수정`}
        onClick={onEdit}
      >
        수정
      </Button>
    </li>
  );
}

function ProjectEditor({ projectId, onBack }: { projectId: number; onBack: () => void }) {
  const { data: project } = useSuspenseQuery({
    ...adminProjectDetailQuery(projectId),
    // 다른 관리자나 작성자가 방금 고친 값 위에서 시작하도록 매번 새로 받는다.
    staleTime: 0,
  });

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3">
        <Button variant="ghost" size="sm" className="w-fit gap-1 px-0" onClick={onBack}>
          <IconChevronLeft className="size-4" aria-hidden="true" />
          목록으로
        </Button>
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="text-lg font-bold">{project.title} 수정</h2>
          <Badge>{REVIEW_STATUS_LABELS[project.approvalStatus]}</Badge>
          {project.approvalStatus === 'APPROVED' && (
            <Link
              to="/projects/$slug"
              params={{ slug: toProjectSlugParam(project.slug) }}
              target="_blank"
              className="text-sm text-gray-600 underline"
            >
              공개 페이지 보기
            </Link>
          )}
        </div>
      </div>

      <p className="flex gap-2 rounded-lg bg-yellow-50 p-4 text-sm leading-relaxed text-yellow-800">
        <IconAlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
        <span>
          작성자 확인 없이 저장 즉시 반영됩니다. 바꾼 칸만 서버로 보내고, 참여 팀원은 여기서 바꿀 수
          없습니다.
        </span>
      </p>

      <ProjectEditForm project={project} />
    </div>
  );
}

function ProjectEditForm({ project }: { project: AdminProjectDetail }) {
  const client = useQueryClient();
  const { data: cohorts } = useSuspenseQuery(cohortsQueryOptions());
  // 저장에 성공하면 기준값을 방금 저장한 값으로 옮겨, 다시 저장할 때 바뀐 칸만 보내게 한다.
  const [baseline, setBaseline] = useState(() => toAdminProjectFormValues(project));
  const [values, setValues] = useState(baseline);
  const [errors, setErrors] = useState<AdminProjectFormErrors>({});
  const [saved, setSaved] = useState(false);
  const update = useMutation(updateAdminProjectMutation);

  const changes = toAdminProjectUpdateBody(baseline, values);
  const changedLabels = (Object.keys(changes) as (keyof AdminProjectUpdateBody)[]).map(
    (field) => FIELD_LABELS[field],
  );
  const slugChanged = values.slug.trim() !== baseline.slug;

  const setField = <Key extends keyof AdminProjectFormValues>(
    field: Key,
    value: AdminProjectFormValues[Key],
  ) => {
    setSaved(false);
    setValues((current) => ({ ...current, [field]: value }));
  };

  const submit = () => {
    if (update.isPending || changedLabels.length === 0) return;
    const nextErrors = validateAdminProjectForm(values);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    update.mutate(
      { projectId: project.id, body: changes },
      {
        onSuccess: () => {
          setBaseline(values);
          setSaved(true);
          void client.invalidateQueries({ queryKey: ['admin', 'projects'] });
          void client.invalidateQueries({ queryKey: ['project-list'] });
          void client.invalidateQueries({ queryKey: ['project-detail'] });
          void client.invalidateQueries({ queryKey: ['users'] });
          void client.invalidateQueries({ queryKey: ['home'] });
        },
        onError: (error) => setErrors(toAdminProjectFormErrors(error)),
      },
    );
  };

  return (
    <form
      className="flex flex-col gap-6"
      onSubmit={(event) => {
        event.preventDefault();
        submit();
      }}
    >
      <FormSection title="기본 정보">
        <Field label="프로젝트 이름 *" error={errors.title}>
          {(id) => (
            <Input
              id={id}
              value={values.title}
              onChange={(event) => setField('title', event.target.value)}
            />
          )}
        </Field>
        <div className="grid gap-5 md:grid-cols-2">
          <Field label="팀 이름" error={errors.teamName}>
            {(id) => (
              <Input
                id={id}
                value={values.teamName}
                onChange={(event) => setField('teamName', event.target.value)}
              />
            )}
          </Field>
          <Field label="우테코 기수 *" error={errors.cohort}>
            {(id) => (
              <Select
                aria-labelledby={`${id}-label`}
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
        </div>
        <Field label="한 줄 소개 *" error={errors.tagline}>
          {(id) => (
            <textarea
              id={id}
              rows={2}
              value={values.tagline}
              onChange={(event) => setField('tagline', event.target.value)}
              className={`${TEXTAREA_CLASS} resize-none`}
            />
          )}
        </Field>
      </FormSection>

      <FormSection title="상태">
        <div className="grid gap-5 md:grid-cols-2">
          <Field label="승인 상태">
            {(id) => (
              <Select
                aria-labelledby={`${id}-label`}
                value={values.approvalStatus}
                onValueChange={(value) =>
                  setField('approvalStatus', value as AdminProjectFormValues['approvalStatus'])
                }
              >
                {Object.entries(REVIEW_STATUS_LABELS).map(([value, label]) => (
                  <Select.Item key={value} value={value}>
                    {label}
                  </Select.Item>
                ))}
              </Select>
            )}
          </Field>
          <Field label="서비스 운영 상태">
            {(id) => (
              <Select
                aria-labelledby={`${id}-label`}
                value={values.serviceStatus}
                onValueChange={(value) =>
                  setField('serviceStatus', value as AdminProjectFormValues['serviceStatus'])
                }
              >
                <Select.Item value="OPERATING">운영 중</Select.Item>
                <Select.Item value="CLOSED">운영 종료</Select.Item>
              </Select>
            )}
          </Field>
        </div>
      </FormSection>

      <FormSection title="주소">
        <Field label="slug *" error={errors.slug}>
          {(id) => (
            <Input
              id={id}
              value={values.slug}
              onChange={(event) => setField('slug', event.target.value)}
              aria-invalid={Boolean(errors.slug)}
            />
          )}
        </Field>
        {slugChanged && (
          <p className="-mt-3 text-xs text-yellow-700">
            저장하면 기존 주소 /projects/@{baseline.slug}로 들어오는 링크가 끊깁니다.
          </p>
        )}
        <Field label="GitHub 레포지토리 URL *" error={errors.githubRepositoryUrl}>
          {(id) => (
            <Input
              id={id}
              value={values.githubRepositoryUrl}
              onChange={(event) => setField('githubRepositoryUrl', event.target.value)}
            />
          )}
        </Field>
        <Field label="서비스 배포 URL" error={errors.deploymentUrl}>
          {(id) => (
            <Input
              id={id}
              value={values.deploymentUrl}
              onChange={(event) => setField('deploymentUrl', event.target.value)}
              placeholder="없으면 비워 두세요"
            />
          )}
        </Field>
      </FormSection>

      <FormSection title="썸네일">
        {project.imageUrl && values.thumbnailImageId === baseline.thumbnailImageId && (
          <Image
            src={project.imageUrl}
            alt={`${project.title} 현재 썸네일`}
            className="aspect-video w-full max-w-xs rounded-lg bg-gray-100 object-cover"
            fallback={<div className="aspect-video w-full max-w-xs rounded-lg bg-gray-100" />}
          />
        )}
        <ThumbnailField
          label="새 썸네일 올리기"
          value={
            values.thumbnailImageId === baseline.thumbnailImageId ? null : values.thumbnailImageId
          }
          onChange={(mediaId) => setField('thumbnailImageId', mediaId)}
          error={errors.thumbnailImageId}
        />
        {values.thumbnailImageId === null && baseline.thumbnailImageId !== null && (
          <p className="text-xs text-yellow-700">저장하면 썸네일이 지워집니다.</p>
        )}
        {values.thumbnailImageId !== null && (
          <Button
            variant="ghost"
            size="sm"
            className="w-fit text-red-600"
            onClick={() => setField('thumbnailImageId', null)}
          >
            썸네일 지우기
          </Button>
        )}
      </FormSection>

      <FormSection title="소개">
        <Field label="상세 설명" error={errors.descriptionMd}>
          {(id) => (
            <textarea
              id={id}
              rows={12}
              value={values.descriptionMd}
              onChange={(event) => setField('descriptionMd', event.target.value)}
              placeholder="Markdown"
              className={`${TEXTAREA_CLASS} font-mono`}
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
      </FormSection>

      <FormSection title="참여 팀원">
        <p className="text-xs text-gray-500">팀원은 관리자 수정으로 바꿀 수 없습니다.</p>
        <ul className="flex flex-wrap gap-2">
          {project.members.map((member, index) => (
            <li key={member.userId ?? `${member.displayName}-${index}`}>
              <Badge>{member.displayName}</Badge>
            </li>
          ))}
        </ul>
      </FormSection>

      <div className="bg-background sticky bottom-0 flex flex-col gap-2 border-t border-gray-200 py-4 md:flex-row md:items-center md:justify-between">
        <div className="min-w-0 text-sm" aria-live="polite">
          {update.isError ? (
            <p role="alert" className="text-red-600">
              {getApiErrorMessage(update.error)}
            </p>
          ) : saved && changedLabels.length === 0 ? (
            <p role="status" className="text-green-600">
              저장했어요.
            </p>
          ) : changedLabels.length > 0 ? (
            <p className="truncate text-gray-600">바뀐 항목: {changedLabels.join(', ')}</p>
          ) : (
            <p className="text-gray-500">바뀐 항목이 없습니다.</p>
          )}
        </div>
        <div className="flex shrink-0 gap-2">
          <Button
            variant="outline"
            disabled={changedLabels.length === 0 || update.isPending}
            onClick={() => {
              setValues(baseline);
              setErrors({});
              update.reset();
            }}
          >
            되돌리기
          </Button>
          <Button type="submit" disabled={changedLabels.length === 0 || update.isPending}>
            {update.isPending ? '저장 중…' : '저장'}
          </Button>
        </div>
      </div>
    </form>
  );
}

function FormSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset className="flex flex-col gap-5 rounded-xl border border-gray-200 p-5">
      <legend className="px-1 text-sm font-bold text-gray-900">{title}</legend>
      {children}
    </fieldset>
  );
}
