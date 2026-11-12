import { useState } from 'react';
import {
  useMutation,
  useQuery,
  useQueryClient,
  useSuspenseInfiniteQuery,
} from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import { deleteNewsMutation, updateNewsMutation } from '@/apis/admin';
import { newsDetailQueryOptions, newsInfiniteQueryOptions } from '@/api/news';
import type { NewsFindAllSuccessResponse, NewsUpdateRequest } from '@/api/generated/schema';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { Field } from '@/components/Field';
import { Input } from '@/components/Input';
import type { NewsDetail } from '@/types/news';
import { formatDotDate } from '@/utils/date';
import { getApiErrorMessage } from '@/utils/error';

type NewsItem = NewsFindAllSuccessResponse['data'][number];

const TEXTAREA_CLASS =
  'focus-visible:outline-primary-600 w-full rounded-lg bg-gray-100 px-4 py-3 text-sm text-gray-900 outline-none placeholder:text-gray-500 focus-visible:outline-2';

/** ISO 시각을 `datetime-local` 입력값(브라우저 시간대)으로 바꾼다. */
const toLocalInput = (iso?: string | null) => {
  if (!iso) return '';
  const date = new Date(iso);
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

const toIsoOrNull = (local: string) => (local ? new Date(local).toISOString() : null);

/** 등록한 공지·이벤트 목록. 수정·삭제한다. */
export function NewsManagePanel() {
  return (
    <section className="flex flex-col gap-4">
      <h2 className="text-lg font-bold">등록한 소식</h2>
      <AsyncBoundary>
        <NewsList />
      </AsyncBoundary>
    </section>
  );
}

function NewsList() {
  const { data, hasNextPage, fetchNextPage, isFetchingNextPage } = useSuspenseInfiniteQuery(
    newsInfiniteQueryOptions('ALL', 'LATEST'),
  );
  const items = data.pages.flatMap((page) => page.data);

  if (items.length === 0) {
    return <p className="py-10 text-center text-sm text-gray-500">등록한 소식이 없습니다.</p>;
  }

  return (
    <>
      <ul className="flex flex-col divide-y divide-gray-200 rounded-xl border border-gray-200">
        {items.map((item) => (
          <NewsRow key={item.id} item={item} />
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

function NewsRow({ item }: { item: NewsItem }) {
  const client = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const detail = useQuery({ ...newsDetailQueryOptions(item.id), enabled: editing });
  const remove = useMutation({
    ...deleteNewsMutation,
    onSuccess: () => client.invalidateQueries({ queryKey: ['news'] }),
  });

  return (
    <li className="flex flex-col gap-3 p-4">
      <div className="flex flex-wrap items-center gap-2">
        <Badge tone={item.type === 'EVENT' ? 'green' : 'primary'}>
          {item.type === 'EVENT' ? '이벤트' : '공지'}
        </Badge>
        <Link
          to="/news/$newsId"
          params={{ newsId: String(item.id) }}
          className="font-bold hover:underline"
        >
          {item.title}
        </Link>
        <span className="text-xs text-gray-500">{formatDotDate(item.publishedAt)}</span>
      </div>
      <p className="text-sm text-gray-600">{item.summary}</p>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" onClick={() => setEditing((prev) => !prev)}>
          {editing ? '수정 취소' : '수정'}
        </Button>
        {confirmingDelete ? (
          <>
            <Button size="sm" disabled={remove.isPending} onClick={() => remove.mutate(item.id)}>
              삭제 확정
            </Button>
            <Button variant="ghost" size="sm" onClick={() => setConfirmingDelete(false)}>
              취소
            </Button>
          </>
        ) : (
          <Button variant="ghost" size="sm" onClick={() => setConfirmingDelete(true)}>
            삭제
          </Button>
        )}
      </div>
      {remove.error != null && (
        <p role="alert" className="text-xs text-red-600">
          {getApiErrorMessage(remove.error)}
        </p>
      )}
      {editing && detail.isPending && <p className="text-sm text-gray-500">불러오는 중…</p>}
      {editing && detail.isError && (
        <p role="alert" className="text-sm text-red-600">
          {getApiErrorMessage(detail.error)}
        </p>
      )}
      {editing && detail.data && (
        <NewsEditForm
          newsId={item.id}
          summary={item.summary}
          detail={detail.data}
          onDone={() => setEditing(false)}
        />
      )}
    </li>
  );
}

function NewsEditForm({
  newsId,
  summary,
  detail,
  onDone,
}: {
  newsId: number;
  /** 상세 응답에는 요약이 없어 목록 값을 쓴다. */
  summary: string;
  detail: NewsDetail;
  onDone: () => void;
}) {
  const client = useQueryClient();
  const isEvent = detail.type === 'EVENT';
  const [values, setValues] = useState({
    title: detail.title,
    summary,
    body: detail.body,
    authorName: detail.author.name,
    eventStartAt: toLocalInput(detail.eventStartAt),
    eventEndAt: toLocalInput(detail.eventEndAt),
    ctaLabel: detail.cta?.label ?? '',
    ctaUrl: detail.cta?.url ?? '',
  });
  const update = useMutation({
    ...updateNewsMutation,
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['news'] });
      onDone();
    },
  });
  const setField = (key: keyof typeof values, value: string) =>
    setValues((prev) => ({ ...prev, [key]: value }));
  const canSave =
    values.title.trim() && values.summary.trim() && values.body.trim() && values.authorName.trim();

  return (
    <form
      className="flex flex-col gap-3 rounded-xl border border-gray-200 p-4"
      onSubmit={(event) => {
        event.preventDefault();
        if (!canSave || update.isPending) return;
        const input: NewsUpdateRequest = {
          title: values.title.trim(),
          summary: values.summary.trim(),
          body: values.body,
          authorName: values.authorName.trim(),
          ...(isEvent && {
            eventStartAt: toIsoOrNull(values.eventStartAt),
            eventEndAt: toIsoOrNull(values.eventEndAt),
          }),
          ...(values.ctaLabel.trim() &&
            values.ctaUrl.trim() && {
              cta: { label: values.ctaLabel.trim(), url: values.ctaUrl.trim() },
            }),
        };
        update.mutate({ newsId, input });
      }}
    >
      <Field label="제목 *">
        {(id) => (
          <Input id={id} value={values.title} onChange={(e) => setField('title', e.target.value)} />
        )}
      </Field>
      <Field label="요약 *">
        {(id) => (
          <Input
            id={id}
            value={values.summary}
            onChange={(e) => setField('summary', e.target.value)}
          />
        )}
      </Field>
      <Field label="본문 *">
        {(id) => (
          <textarea
            id={id}
            rows={8}
            className={TEXTAREA_CLASS}
            value={values.body}
            onChange={(e) => setField('body', e.target.value)}
          />
        )}
      </Field>
      <Field label="작성자 *">
        {(id) => (
          <Input
            id={id}
            value={values.authorName}
            onChange={(e) => setField('authorName', e.target.value)}
          />
        )}
      </Field>
      {isEvent && (
        <div className="grid gap-3 md:grid-cols-2">
          <Field label="시작 시각">
            {(id) => (
              <Input
                id={id}
                type="datetime-local"
                value={values.eventStartAt}
                onChange={(e) => setField('eventStartAt', e.target.value)}
              />
            )}
          </Field>
          <Field label="종료 시각">
            {(id) => (
              <Input
                id={id}
                type="datetime-local"
                value={values.eventEndAt}
                onChange={(e) => setField('eventEndAt', e.target.value)}
              />
            )}
          </Field>
        </div>
      )}
      <div className="grid gap-3 md:grid-cols-2">
        <Field label="CTA 문구">
          {(id) => (
            <Input
              id={id}
              value={values.ctaLabel}
              onChange={(e) => setField('ctaLabel', e.target.value)}
            />
          )}
        </Field>
        <Field label="CTA 주소">
          {(id) => (
            <Input
              id={id}
              value={values.ctaUrl}
              onChange={(e) => setField('ctaUrl', e.target.value)}
            />
          )}
        </Field>
      </div>
      {update.error != null && (
        <p role="alert" className="text-xs text-red-600">
          {getApiErrorMessage(update.error)}
        </p>
      )}
      <div className="flex gap-2">
        <Button type="submit" disabled={!canSave || update.isPending}>
          저장
        </Button>
        <Button type="button" variant="ghost" onClick={onDone}>
          닫기
        </Button>
      </div>
    </form>
  );
}
