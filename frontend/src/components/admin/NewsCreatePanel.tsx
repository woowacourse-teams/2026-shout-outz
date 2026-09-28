import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import { createEventMutation, createNoticeMutation } from '@/apis/admin';
import { Button, getButtonStyles } from '@/components/Button';
import { Field } from '@/components/Field';
import { Input } from '@/components/Input';
import { Tab } from '@/components/Tab';
import type { NoticeCreateBody } from '@/types/admin';
import type { NewsType } from '@/types/news';
import { getApiErrorMessage } from '@/utils/error';

const DEFAULT_AUTHOR = '샤라웃 운영팀';

const TEXTAREA_CLASS =
  'focus-visible:outline-primary-600 w-full rounded-lg bg-gray-100 px-4 py-3 text-sm text-gray-900 outline-none placeholder:text-gray-500 focus-visible:outline-2';

interface NewsFormValues {
  title: string;
  summary: string;
  body: string;
  authorName: string;
  eventStartAt: string;
  eventEndAt: string;
  ctaLabel: string;
  ctaUrl: string;
}

const EMPTY_FORM: NewsFormValues = {
  title: '',
  summary: '',
  body: '',
  authorName: DEFAULT_AUTHOR,
  eventStartAt: '',
  eventEndAt: '',
  ctaLabel: '',
  ctaUrl: '',
};

/** 폼 값을 검사해 필드별 오류 문구를 돌려준다. 비어 있으면 통과다. */
function validate(type: NewsType, values: NewsFormValues) {
  const errors: Partial<Record<keyof NewsFormValues, string>> = {};
  if (!values.title.trim()) errors.title = '제목을 입력해 주세요.';
  if (!values.summary.trim()) errors.summary = '요약을 입력해 주세요.';
  if (!values.body.trim()) errors.body = '본문을 입력해 주세요.';
  if (!values.authorName.trim()) errors.authorName = '작성자를 입력해 주세요.';
  if (!values.ctaLabel.trim() !== !values.ctaUrl.trim()) {
    errors.ctaUrl = 'CTA는 문구와 주소를 함께 입력해 주세요.';
  }
  if (type === 'EVENT') {
    if (!values.eventStartAt) errors.eventStartAt = '시작 시각을 입력해 주세요.';
    if (!values.eventEndAt) errors.eventEndAt = '종료 시각을 입력해 주세요.';
    if (values.eventStartAt && values.eventEndAt && values.eventStartAt > values.eventEndAt) {
      errors.eventEndAt = '종료 시각은 시작 시각보다 뒤여야 합니다.';
    }
  }
  return errors;
}

/** `datetime-local` 값(브라우저 시간대)을 ISO-8601 UTC로 바꾼다. */
const toIsoString = (localDateTime: string) => new Date(localDateTime).toISOString();

/** 공지·이벤트 등록. */
export function NewsCreatePanel() {
  const client = useQueryClient();
  const [type, setType] = useState<NewsType>('NOTICE');
  const [values, setValues] = useState<NewsFormValues>(EMPTY_FORM);
  const [errors, setErrors] = useState<ReturnType<typeof validate>>({});
  const [createdId, setCreatedId] = useState<number | null>(null);

  const onSuccess = ({ id }: { id: number }) => {
    client.invalidateQueries({ queryKey: ['news'] });
    setCreatedId(id);
    setValues(EMPTY_FORM);
  };
  const createNotice = useMutation({ ...createNoticeMutation, onSuccess });
  const createEvent = useMutation({ ...createEventMutation, onSuccess });
  const mutation = type === 'NOTICE' ? createNotice : createEvent;

  const setField = (field: keyof NewsFormValues, value: string) =>
    setValues((current) => ({ ...current, [field]: value }));

  const submit = () => {
    const nextErrors = validate(type, values);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    const base: NoticeCreateBody = {
      title: values.title.trim(),
      summary: values.summary.trim(),
      body: values.body,
      authorName: values.authorName.trim(),
      ...(values.ctaLabel.trim()
        ? { cta: { label: values.ctaLabel.trim(), url: values.ctaUrl.trim() } }
        : {}),
    };
    setCreatedId(null);
    if (type === 'NOTICE') {
      createNotice.mutate(base);
    } else {
      createEvent.mutate({
        ...base,
        eventStartAt: toIsoString(values.eventStartAt),
        eventEndAt: toIsoString(values.eventEndAt),
      });
    }
  };

  return (
    <form
      className="flex max-w-2xl flex-col gap-5"
      onSubmit={(event) => {
        event.preventDefault();
        if (!mutation.isPending) submit();
      }}
    >
      <Tab
        variant="chip"
        size="sm"
        value={type}
        onChange={(next) => {
          setType(next as NewsType);
          setErrors({});
        }}
        aria-label="소식 유형"
      >
        <Tab.Item value="NOTICE">공지</Tab.Item>
        <Tab.Item value="EVENT">이벤트</Tab.Item>
      </Tab>

      <Field label="제목 *" error={errors.title}>
        {(id) => (
          <Input
            id={id}
            value={values.title}
            onChange={(event) => setField('title', event.target.value)}
            aria-invalid={Boolean(errors.title)}
          />
        )}
      </Field>
      <Field label="요약 *" error={errors.summary}>
        {(id) => (
          <Input
            id={id}
            value={values.summary}
            onChange={(event) => setField('summary', event.target.value)}
            placeholder="목록에 보이는 한 줄 요약"
            aria-invalid={Boolean(errors.summary)}
          />
        )}
      </Field>
      <Field label="본문 *" error={errors.body}>
        {(id) => (
          <textarea
            id={id}
            rows={10}
            value={values.body}
            onChange={(event) => setField('body', event.target.value)}
            placeholder="Markdown으로 작성할 수 있어요."
            className={TEXTAREA_CLASS}
          />
        )}
      </Field>
      <Field label="작성자 *" error={errors.authorName}>
        {(id) => (
          <Input
            id={id}
            value={values.authorName}
            onChange={(event) => setField('authorName', event.target.value)}
            aria-invalid={Boolean(errors.authorName)}
          />
        )}
      </Field>

      {type === 'EVENT' && (
        <div className="grid gap-5 md:grid-cols-2">
          <Field label="시작 시각 *" error={errors.eventStartAt}>
            {(id) => (
              <Input
                id={id}
                type="datetime-local"
                value={values.eventStartAt}
                onChange={(event) => setField('eventStartAt', event.target.value)}
                aria-invalid={Boolean(errors.eventStartAt)}
              />
            )}
          </Field>
          <Field label="종료 시각 *" error={errors.eventEndAt}>
            {(id) => (
              <Input
                id={id}
                type="datetime-local"
                value={values.eventEndAt}
                onChange={(event) => setField('eventEndAt', event.target.value)}
                aria-invalid={Boolean(errors.eventEndAt)}
              />
            )}
          </Field>
        </div>
      )}

      <div className="grid gap-5 md:grid-cols-2">
        <Field label="CTA 문구">
          {(id) => (
            <Input
              id={id}
              value={values.ctaLabel}
              onChange={(event) => setField('ctaLabel', event.target.value)}
              placeholder="예: 프로젝트 등록하기"
            />
          )}
        </Field>
        <Field label="CTA 주소" error={errors.ctaUrl}>
          {(id) => (
            <Input
              id={id}
              value={values.ctaUrl}
              onChange={(event) => setField('ctaUrl', event.target.value)}
              placeholder="/projects/new 또는 https://…"
              aria-invalid={Boolean(errors.ctaUrl)}
            />
          )}
        </Field>
      </div>

      {mutation.isError && (
        <p role="alert" className="text-sm text-red-600">
          {getApiErrorMessage(mutation.error)}
        </p>
      )}
      {createdId !== null && (
        <p role="status" className="flex items-center gap-3 text-sm text-gray-700">
          소식을 등록했어요.
          <Link
            to="/news/$newsId"
            params={{ newsId: String(createdId) }}
            className={getButtonStyles({ variant: 'outline', size: 'sm' })}
          >
            보러 가기
          </Link>
        </p>
      )}
      <Button type="submit" size="lg" disabled={mutation.isPending}>
        {mutation.isPending ? '등록 중…' : type === 'NOTICE' ? '공지 등록' : '이벤트 등록'}
      </Button>
    </form>
  );
}
