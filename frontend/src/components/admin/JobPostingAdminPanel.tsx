import { useState } from 'react';
import {
  useMutation,
  useQuery,
  useQueryClient,
  useSuspenseInfiniteQuery,
} from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import {
  adminJobPostingQuery,
  adminJobPostingsQuery,
  type AdminJobPostingCreateBody,
  type AdminJobPostingDetail,
  createJobPostingMutation,
  deleteJobPostingMutation,
  extractJobPostingMutation,
  reanalyzeJobPostingMutation,
  updateJobPostingMutation,
  updateJobPostingVisibilityMutation,
} from '@/apis/admin';
import { uploadJobPostingLogo } from '@/api/media';
import type {
  AdminJobPostingFindAllSuccessResponse,
  AdminJobPostingSaveRequest,
} from '@/api/generated/schema';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { Field } from '@/components/Field';
import { Input } from '@/components/Input';
import { Select } from '@/components/Select';
import { ThumbnailField } from '@/components/projects/ThumbnailField';
import { formatDotDate } from '@/utils/date';
import { getApiErrorMessage } from '@/utils/error';

type JobPostingItem = AdminJobPostingFindAllSuccessResponse['data'][number];
type JobPostingDetail = AdminJobPostingDetail;
type JobPostingFormValues = AdminJobPostingCreateBody;
type JobRole = AdminJobPostingSaveRequest['jobRole'];
type RequirementCategory = AdminJobPostingSaveRequest['requirements'][number]['category'];

const JOB_ROLE_LABELS: Record<JobRole, string> = {
  BACKEND: '백엔드',
  FRONTEND: '프론트엔드',
  ANDROID: '안드로이드',
  IOS: 'iOS',
  DEVOPS: '데브옵스',
  DATA: '데이터',
  AI: 'AI',
  ETC: '기타',
};

const CATEGORY_LABELS: Record<RequirementCategory, string> = {
  REQUIRED: '자격 요건',
  PREFERRED: '우대 사항',
  DUTY_OR_STACK: '업무·기술',
};

const STATUS_LABELS: Record<JobPostingItem['status'], string> = {
  PENDING: '분석 대기',
  PROCESSING: '분석 중',
  READY: '분석 완료',
  FAILED: '분석 실패',
};

const TEXTAREA_CLASS =
  'focus-visible:outline-primary-600 w-full rounded-lg bg-gray-100 px-4 py-3 text-sm text-gray-900 outline-none placeholder:text-gray-500 focus-visible:outline-2';

const EMPTY_FORM: JobPostingFormValues = {
  logoImageId: null,
  sourceUrl: '',
  content: '',
  title: '',
  companyName: '',
  jobRole: 'BACKEND',
  careerRequirement: '',
  employmentType: '',
  deadline: '',
  requirements: [],
};

/** 빈 문자열은 null로 보내 서버가 "값 없음"으로 저장하게 한다. */
const toSaveBody = (values: JobPostingFormValues): JobPostingFormValues => ({
  ...values,
  careerRequirement: values.careerRequirement?.trim() || null,
  employmentType: values.employmentType?.trim() || null,
  deadline: values.deadline || null,
  requirements: values.requirements.filter((item) => item.name.trim()),
});

const fromDetail = (detail: JobPostingDetail): JobPostingFormValues => ({
  sourceUrl: detail.sourceUrl,
  // 수정은 전체 교체라 지금 로고를 다시 보내야 지워지지 않는다.
  logoImageId: detail.logoImageId ?? null,
  // 서버가 원문을 저장하지 않아 상세에 없다. 비워 두면 수정 때 보내지 않는다.
  content: '',
  title: detail.title,
  companyName: detail.companyName,
  jobRole: detail.jobRole,
  careerRequirement: detail.careerRequirement ?? '',
  employmentType: detail.employmentType ?? '',
  deadline: detail.deadline ?? '',
  requirements: [...detail.requirements]
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .map(({ category, name, summary }) => ({ category, name, summary })),
});

/** 채용공고 관리. 본문으로 초안을 뽑아 등록하고, 목록에서 수정·숨김·재분석·삭제한다. */
export function JobPostingAdminPanel() {
  const [creating, setCreating] = useState(false);

  return (
    <div className="flex flex-col gap-6">
      {creating ? (
        <JobPostingForm onDone={() => setCreating(false)} />
      ) : (
        <Button className="self-start" onClick={() => setCreating(true)}>
          새 채용공고
        </Button>
      )}
      <AsyncBoundary>
        <JobPostingList />
      </AsyncBoundary>
    </div>
  );
}

function JobPostingList() {
  const { data, hasNextPage, fetchNextPage, isFetchingNextPage } =
    useSuspenseInfiniteQuery(adminJobPostingsQuery);
  const items = data.pages.flatMap((page) => page.data);
  const totalCount = data.pages[0]?.meta.totalCount ?? 0;

  if (items.length === 0) {
    return <p className="py-10 text-center text-sm text-gray-500">등록한 채용공고가 없습니다.</p>;
  }

  return (
    <>
      <p className="text-sm text-gray-500">전체 {totalCount}건</p>
      <ul className="flex flex-col divide-y divide-gray-200 rounded-xl border border-gray-200">
        {items.map((item) => (
          <JobPostingRow key={item.jobPostingId} item={item} />
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

function JobPostingRow({ item }: { item: JobPostingItem }) {
  const client = useQueryClient();
  const [mode, setMode] = useState<'closed' | 'detail' | 'edit'>('closed');
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [savedMessage, setSavedMessage] = useState<string | null>(null);
  const detail = useQuery({
    ...adminJobPostingQuery(item.jobPostingId),
    enabled: mode !== 'closed',
  });
  const refresh = () => {
    void client.invalidateQueries({ queryKey: ['admin', 'job-postings'] });
    void client.invalidateQueries({ queryKey: ['admin', 'job-posting', item.jobPostingId] });
    void client.invalidateQueries({ queryKey: ['job-postings'] });
  };
  const visibility = useMutation({ ...updateJobPostingVisibilityMutation, onSuccess: refresh });
  const reanalyze = useMutation({ ...reanalyzeJobPostingMutation, onSuccess: refresh });
  const remove = useMutation({ ...deleteJobPostingMutation, onSuccess: refresh });
  const pending = visibility.isPending || reanalyze.isPending || remove.isPending;
  const error = visibility.error ?? reanalyze.error ?? remove.error;
  const hidden = item.hiddenAt != null;

  return (
    <li className="flex flex-col gap-3 p-4">
      <div className="flex flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-bold">{item.title}</span>
          <span className="text-sm text-gray-500">{item.companyName}</span>
          <Badge>{JOB_ROLE_LABELS[item.jobRole]}</Badge>
          <Badge tone={item.status === 'FAILED' ? 'gray' : 'primary'}>
            {STATUS_LABELS[item.status]}
          </Badge>
          {hidden && <Badge>숨김</Badge>}
        </div>
        <p className="text-xs text-gray-500">
          등록 {formatDotDate(item.createdAt)}
          {item.failedRequirementCount > 0 &&
            ` · 분석 실패 요구사항 ${item.failedRequirementCount}개`}
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        <Button
          variant="outline"
          size="sm"
          aria-expanded={mode === 'detail'}
          onClick={() => setMode((prev) => (prev === 'detail' ? 'closed' : 'detail'))}
        >
          {mode === 'detail' ? '상세 닫기' : '상세'}
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={() => setMode((prev) => (prev === 'edit' ? 'closed' : 'edit'))}
        >
          {mode === 'edit' ? '수정 취소' : '수정'}
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={pending}
          onClick={() => visibility.mutate({ jobPostingId: item.jobPostingId, hidden: !hidden })}
        >
          {hidden ? '보이기' : '숨기기'}
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={pending || item.failedRequirementCount === 0}
          onClick={() => reanalyze.mutate({ jobPostingId: item.jobPostingId, scope: 'FAILED' })}
        >
          실패만 재분석
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={pending}
          onClick={() => reanalyze.mutate({ jobPostingId: item.jobPostingId, scope: 'ALL' })}
        >
          전체 재분석
        </Button>
        {!hidden && (
          <Link
            to="/job-postings/$jobPostingId"
            params={{ jobPostingId: String(item.jobPostingId) }}
            className="self-center text-sm underline"
          >
            공개 페이지
          </Link>
        )}
        {confirmingDelete ? (
          <>
            <Button size="sm" disabled={pending} onClick={() => remove.mutate(item.jobPostingId)}>
              삭제 확정
            </Button>
            <Button variant="ghost" size="sm" onClick={() => setConfirmingDelete(false)}>
              취소
            </Button>
          </>
        ) : (
          <Button
            variant="ghost"
            size="sm"
            disabled={pending}
            onClick={() => setConfirmingDelete(true)}
          >
            삭제
          </Button>
        )}
      </div>

      {savedMessage && (
        <p role="status" className="text-xs text-gray-600">
          {savedMessage}
        </p>
      )}
      {reanalyze.data && (
        <p role="status" className="text-xs text-gray-600">
          요구사항 {reanalyze.data.reanalyzedRequirementCount}개를 다시 분석합니다.
        </p>
      )}
      {error != null && (
        <p role="alert" className="text-xs text-red-600">
          {getApiErrorMessage(error)}
        </p>
      )}

      {mode !== 'closed' && detail.isPending && (
        <p className="text-sm text-gray-500">상세를 불러오는 중…</p>
      )}
      {mode !== 'closed' && detail.isError && (
        <p role="alert" className="text-sm text-red-600">
          {getApiErrorMessage(detail.error)}
        </p>
      )}
      {mode === 'detail' && detail.data && <JobPostingDetailView detail={detail.data} />}
      {mode === 'edit' && detail.data && (
        <JobPostingForm
          jobPostingId={item.jobPostingId}
          initialValues={fromDetail(detail.data)}
          onDone={(message) => {
            setSavedMessage(message ?? null);
            setMode('closed');
          }}
        />
      )}
    </li>
  );
}

function JobPostingDetailView({ detail }: { detail: JobPostingDetail }) {
  const requirements = [...detail.requirements].sort((a, b) => a.displayOrder - b.displayOrder);
  return (
    <div className="flex flex-col gap-3 border-t border-gray-200 pt-3 text-sm">
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-gray-700">
        <dt className="text-gray-500">원문</dt>
        <dd className="min-w-0 truncate">
          <a
            href={detail.sourceUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="underline"
          >
            {detail.sourceUrl}
          </a>
        </dd>
        <dt className="text-gray-500">경력</dt>
        <dd>{detail.careerRequirement ?? '-'}</dd>
        <dt className="text-gray-500">고용 형태</dt>
        <dd>{detail.employmentType ?? '-'}</dd>
        <dt className="text-gray-500">마감일</dt>
        <dd>{detail.deadline ?? '-'}</dd>
      </dl>
      <ul className="flex flex-col gap-2">
        {requirements.map((requirement) => (
          <li key={requirement.requirementId} className="rounded-lg bg-gray-50 p-3">
            <div className="flex flex-wrap items-center gap-2">
              <Badge>{CATEGORY_LABELS[requirement.category]}</Badge>
              <span className="font-semibold">{requirement.name}</span>
              <Badge tone={requirement.status === 'FAILED' ? 'gray' : 'primary'}>
                {STATUS_LABELS[requirement.status]}
              </Badge>
              {requirement.lastError && (
                <span className="text-xs text-red-600">{requirement.lastError}</span>
              )}
            </div>
            <p className="mt-1 text-xs text-gray-600">{requirement.summary}</p>
            {requirement.recommendations.length > 0 && (
              <ul className="mt-2 flex flex-col gap-1 text-xs text-gray-700">
                {requirement.recommendations.map((recommendation) => (
                  <li key={recommendation.externalKey}>
                    [{recommendation.verdict}] {recommendation.title ?? recommendation.externalKey}{' '}
                    <span className="text-gray-500">
                      ({recommendation.source}, {recommendation.similarityScore.toFixed(2)})
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}

function JobPostingForm({
  jobPostingId,
  initialValues = EMPTY_FORM,
  onDone,
}: {
  /** 있으면 수정, 없으면 새로 등록 */
  jobPostingId?: number;
  initialValues?: JobPostingFormValues;
  /** 수정이면 저장 결과 문구(다시 분석했는지)를 넘긴다. */
  onDone: (message?: string) => void;
}) {
  const client = useQueryClient();
  const [values, setValues] = useState(initialValues);
  const [duplicateId, setDuplicateId] = useState<number | null>(null);
  const refresh = () => {
    void client.invalidateQueries({ queryKey: ['admin', 'job-postings'] });
    void client.invalidateQueries({ queryKey: ['admin', 'job-posting'] });
    void client.invalidateQueries({ queryKey: ['job-postings'] });
  };
  const extract = useMutation({
    ...extractJobPostingMutation,
    onSuccess: (draft) => {
      setDuplicateId(draft.registeredJobPostingId ?? null);
      setValues((prev) => ({
        ...prev,
        title: draft.title ?? prev.title,
        companyName: draft.companyName ?? prev.companyName,
        jobRole: draft.jobRole,
        careerRequirement: draft.careerRequirement ?? prev.careerRequirement,
        employmentType: draft.employmentType ?? prev.employmentType,
        deadline: draft.deadline ?? prev.deadline,
        requirements: draft.requirements,
      }));
    },
  });
  const create = useMutation({
    ...createJobPostingMutation,
    onSuccess: () => {
      refresh();
      onDone();
    },
  });
  const update = useMutation({
    ...updateJobPostingMutation,
    onSuccess: ({ reanalyzed }) => {
      refresh();
      onDone(
        reanalyzed
          ? '본문이 바뀌어 역량을 바꾸고 다시 분석합니다.'
          : '공고 정보만 바꿨어요. 역량과 분석 상태는 그대로예요.',
      );
    },
  });
  const save = jobPostingId == null ? create : update;
  const isEdit = jobPostingId != null;
  // 수정에서 본문을 비워 두면 content·requirements를 빼고 보내 공고 정보만 바꾼다.
  // 서버도 빈 문자열·공백을 안 보낸 것으로 보지만, 역량까지 빼야 해서 화면에서 나눈다.
  const changesContent = values.content.trim() !== '';
  const editsRequirements = !isEdit || changesContent;
  const validRequirements = values.requirements.filter((item) => item.name.trim());
  // 본문을 보내면서 역량을 비우면 400(requirementsPresentWithContent)이라 미리 막는다.
  const requirementsMissing = editsRequirements && validRequirements.length === 0;
  const canSave =
    values.sourceUrl.trim() &&
    (isEdit || changesContent) &&
    !requirementsMissing &&
    values.title.trim() &&
    values.companyName.trim();

  const setField = <K extends keyof JobPostingFormValues>(key: K, value: JobPostingFormValues[K]) =>
    setValues((prev) => ({ ...prev, [key]: value }));
  const setRequirement = (
    index: number,
    patch: Partial<AdminJobPostingSaveRequest['requirements'][number]>,
  ) =>
    setField(
      'requirements',
      values.requirements.map((item, i) => (i === index ? { ...item, ...patch } : item)),
    );

  return (
    <form
      className="flex flex-col gap-4 rounded-xl border border-gray-200 p-4"
      onSubmit={(event) => {
        event.preventDefault();
        if (!canSave || save.isPending) return;
        const input = toSaveBody(values);
        if (jobPostingId == null) {
          create.mutate(input);
        } else if (changesContent) {
          update.mutate({ jobPostingId, input });
        } else {
          update.mutate({
            jobPostingId,
            input: { ...input, content: undefined, requirements: undefined },
          });
        }
      }}
    >
      <h2 className="font-bold">{jobPostingId == null ? '새 채용공고' : '채용공고 수정'}</h2>
      <Field label="원문 주소 *">
        {(id) => (
          <Input
            id={id}
            value={values.sourceUrl}
            onChange={(event) => setField('sourceUrl', event.target.value)}
            placeholder="https://"
          />
        )}
      </Field>
      <Field label={jobPostingId == null ? '공고 본문 *' : '공고 본문'}>
        {(id) => (
          <textarea
            id={id}
            rows={6}
            className={TEXTAREA_CLASS}
            value={values.content}
            onChange={(event) => setField('content', event.target.value)}
            placeholder={
              jobPostingId == null
                ? '공고 본문을 붙여 넣고 추출을 누르면 아래 항목을 채워 줘요.'
                : '본문을 바꿀 때만 붙여 넣으세요. 비워 두면 공고 정보만 바뀌고 역량과 분석 상태는 그대로예요.'
            }
          />
        )}
      </Field>
      <div className="flex items-center gap-3">
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={!values.sourceUrl.trim() || !values.content.trim() || extract.isPending}
          onClick={() =>
            extract.mutate({ sourceUrl: values.sourceUrl.trim(), content: values.content })
          }
        >
          {extract.isPending ? '추출하는 중…' : '본문에서 추출'}
        </Button>
        {extract.error != null && (
          <p role="alert" className="text-xs text-red-600">
            {getApiErrorMessage(extract.error)}
          </p>
        )}
      </div>
      {duplicateId != null && duplicateId !== jobPostingId && (
        <p role="status" className="text-xs text-red-600">
          같은 주소로 이미 등록한 공고가 있어요 (#{duplicateId}).
        </p>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        <Field label="제목 *">
          {(id) => (
            <Input
              id={id}
              value={values.title}
              onChange={(event) => setField('title', event.target.value)}
            />
          )}
        </Field>
        <Field label="회사명 *">
          {(id) => (
            <Input
              id={id}
              value={values.companyName}
              onChange={(event) => setField('companyName', event.target.value)}
            />
          )}
        </Field>
        <Field label="직군">
          {(id) => (
            <Select
              id={id}
              value={values.jobRole}
              onValueChange={(value) => setField('jobRole', value as JobRole)}
            >
              {Object.entries(JOB_ROLE_LABELS).map(([value, label]) => (
                <Select.Item key={value} value={value}>
                  {label}
                </Select.Item>
              ))}
            </Select>
          )}
        </Field>
        <Field label="마감일">
          {(id) => (
            <Input
              id={id}
              type="date"
              value={values.deadline ?? ''}
              onChange={(event) => setField('deadline', event.target.value)}
            />
          )}
        </Field>
        <Field label="경력 조건">
          {(id) => (
            <Input
              id={id}
              value={values.careerRequirement ?? ''}
              onChange={(event) => setField('careerRequirement', event.target.value)}
            />
          )}
        </Field>
        <Field label="고용 형태">
          {(id) => (
            <Input
              id={id}
              value={values.employmentType ?? ''}
              onChange={(event) => setField('employmentType', event.target.value)}
            />
          )}
        </Field>
      </div>

      <ThumbnailField
        label="회사 로고"
        value={values.logoImageId ?? null}
        onChange={(logoImageId) => setField('logoImageId', logoImageId)}
        upload={uploadJobPostingLogo}
      />

      {editsRequirements ? (
        <fieldset className="flex flex-col gap-2">
          <legend className="mb-2 text-sm font-semibold">요구사항</legend>
          {isEdit && (
            <p className="text-xs text-gray-500">
              본문이 실제로 바뀌었으면 아래 역량으로 바꾸고 다시 분석해요.
            </p>
          )}
          {values.requirements.map((requirement, index) => (
            <div key={index} className="flex flex-col gap-2 rounded-lg bg-gray-50 p-3 md:flex-row">
              <div className="md:w-36">
                <Select
                  aria-label={`요구사항 ${index + 1} 분류`}
                  value={requirement.category}
                  onValueChange={(value) =>
                    setRequirement(index, { category: value as RequirementCategory })
                  }
                >
                  {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                    <Select.Item key={value} value={value}>
                      {label}
                    </Select.Item>
                  ))}
                </Select>
              </div>
              <Input
                size="sm"
                aria-label={`요구사항 ${index + 1} 이름`}
                value={requirement.name}
                onChange={(event) => setRequirement(index, { name: event.target.value })}
                placeholder="이름"
              />
              <Input
                size="sm"
                aria-label={`요구사항 ${index + 1} 요약`}
                value={requirement.summary}
                onChange={(event) => setRequirement(index, { summary: event.target.value })}
                placeholder="요약"
              />
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() =>
                  setField(
                    'requirements',
                    values.requirements.filter((_, i) => i !== index),
                  )
                }
              >
                빼기
              </Button>
            </div>
          ))}
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="self-start"
            onClick={() =>
              setField('requirements', [
                ...values.requirements,
                { category: 'REQUIRED', name: '', summary: '' },
              ])
            }
          >
            요구사항 추가
          </Button>
          {requirementsMissing && (
            <p className="text-xs text-red-600">요구사항을 1개 이상 넣어 주세요.</p>
          )}
        </fieldset>
      ) : (
        <p className="text-sm text-gray-500">
          요구사항은 본문을 바꿀 때만 고칠 수 있어요. 지금 역량과 분석 상태는 그대로 둬요.
        </p>
      )}

      {save.error != null && (
        <p role="alert" className="text-xs text-red-600">
          {getApiErrorMessage(save.error)}
        </p>
      )}
      <div className="flex gap-2">
        <Button type="submit" disabled={!canSave || save.isPending}>
          {jobPostingId == null ? '등록' : '저장'}
        </Button>
        <Button type="button" variant="ghost" onClick={() => onDone()}>
          닫기
        </Button>
      </div>
    </form>
  );
}
