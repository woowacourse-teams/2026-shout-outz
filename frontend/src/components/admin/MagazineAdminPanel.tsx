import { useRef, useState, type ChangeEvent } from 'react';
import {
  useMutation,
  useQuery,
  useQueryClient,
  useSuspenseInfiniteQuery,
  useSuspenseQuery,
} from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import {
  adminMagazineQuery,
  adminMagazinesQuery,
  adminQueryKeys,
  createMagazineMutation,
  deleteMagazineMutation,
  publishMagazineMutation,
  unpublishMagazineMutation,
  updateMagazineMutation,
} from '@/apis/admin';
import { crewSearchQueryOptions } from '@/api/project';
import { uploadMagazineContentImage, uploadMagazineThumbnail } from '@/api/media';
import type { MagazineListItem } from '@/apis/magazine';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Avatar } from '@/components/Avatar';
import { Badge } from '@/components/Badge';
import { Button, getButtonStyles } from '@/components/Button';
import { Field } from '@/components/Field';
import { Image } from '@/components/Image';
import { Input } from '@/components/Input';
import { MarkdownContent } from '@/components/MarkdownContent';
import { ThumbnailField } from '@/components/projects/ThumbnailField';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import type { AdminMagazine, MagazineSaveBody } from '@/types/admin';
import { formatDateTime } from '@/utils/date';
import { getApiErrorMessage } from '@/utils/error';

const TEXTAREA_CLASS =
  'focus-visible:outline-primary-600 w-full rounded-lg bg-gray-100 px-4 py-3 text-sm text-gray-900 outline-none placeholder:text-gray-500 focus-visible:outline-2';

/** 서버 제약과 같은 길이 한도. */
const TITLE_MAX = 200;
const SUMMARY_MAX = 500;
const INTRODUCTION_MAX = 200;
const CONTENT_MAX = 100_000;

/** 화면에 보여줄 인터뷰이. 상세 응답은 ID만 주므로 목록이나 검색 결과에서 이름을 가져온다. */
interface FeaturedUser {
  userId: number;
  displayName: string;
  handle?: string | null;
}

interface Editing {
  magazineId: number;
  /** 목록에서 가져온 대상 이름. 상세 응답에는 ID만 있다. */
  featuredUser?: Pick<FeaturedUser, 'displayName' | 'handle'>;
}

/** 매거진 목록과 초안 등록·수정, 발행 관리. */
export function MagazineAdminPanel() {
  const [editing, setEditing] = useState<Editing | null>(null);
  // 등록이 끝나면 key를 바꿔 이미지 필드까지 새로 그린다.
  const [formKey, setFormKey] = useState(0);
  const resetForm = () => {
    setEditing(null);
    setFormKey((key) => key + 1);
  };

  return (
    <div className="flex flex-col gap-10">
      <MagazineList editingId={editing?.magazineId} onEdit={setEditing} />
      {editing ? (
        <AsyncBoundary key={editing.magazineId}>
          <MagazineEditForm editing={editing} onDone={resetForm} />
        </AsyncBoundary>
      ) : (
        <MagazineForm key={formKey} onDone={resetForm} />
      )}
    </div>
  );
}

function MagazineList({
  editingId,
  onEdit,
}: {
  editingId?: number;
  onEdit: (editing: Editing) => void;
}) {
  const query = useSuspenseInfiniteQuery(adminMagazinesQuery);
  const magazines = query.data.pages.flatMap((page) => page.data);

  return (
    <section className="flex flex-col gap-3">
      <h2 className="text-lg font-bold">매거진 ({query.data.pages[0]?.meta?.totalCount ?? 0})</h2>
      {magazines.length === 0 ? (
        <p className="py-6 text-center text-sm text-gray-500">등록된 매거진이 없습니다.</p>
      ) : (
        <ul className="flex flex-col divide-y divide-gray-200 rounded-xl border border-gray-200">
          {magazines.map((magazine) => (
            <MagazineRow
              key={magazine.id}
              magazine={magazine}
              editing={magazine.id === editingId}
              onEdit={onEdit}
            />
          ))}
        </ul>
      )}
      {query.hasNextPage && (
        <Button
          variant="outline"
          disabled={query.isFetchingNextPage}
          onClick={() => void query.fetchNextPage()}
        >
          {query.isFetchingNextPage ? '불러오는 중…' : '매거진 더 보기'}
        </Button>
      )}
    </section>
  );
}

function MagazineRow({
  magazine,
  editing,
  onEdit,
}: {
  magazine: MagazineListItem;
  editing: boolean;
  onEdit: (editing: Editing) => void;
}) {
  const client = useQueryClient();
  const onSuccess = () => {
    void client.invalidateQueries({ queryKey: adminQueryKeys.magazines });
    void client.invalidateQueries({ queryKey: adminQueryKeys.magazine(magazine.id) });
    void client.invalidateQueries({ queryKey: ['magazines'] });
    void client.invalidateQueries({ queryKey: ['magazine', magazine.id] });
  };
  const publish = useMutation({ ...publishMagazineMutation, onSuccess });
  const unpublish = useMutation({ ...unpublishMagazineMutation, onSuccess });
  const remove = useMutation({ ...deleteMagazineMutation, onSuccess });
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const published = magazine.status === 'PUBLISHED';
  const pending = publish.isPending || unpublish.isPending || remove.isPending;
  const error = publish.error ?? unpublish.error ?? remove.error;

  return (
    <li className="flex flex-col gap-3 p-4 md:flex-row md:items-center">
      <Image
        src={magazine.thumbnailUrl ?? undefined}
        alt=""
        className="aspect-video w-full rounded-lg bg-gray-100 md:w-40"
        fallback={<div className="aspect-video w-full rounded-lg bg-gray-100 md:w-40" />}
      />
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2">
          <Badge tone={published ? 'green' : 'gray'}>{published ? '발행됨' : '초안'}</Badge>
          {editing && <Badge tone="primary">수정 중</Badge>}
          <span className="truncate font-bold">{magazine.title}</span>
        </div>
        <p className="text-sm text-gray-600">
          {magazine.featuredUser?.displayName ?? '인터뷰이 없음'}
        </p>
        <p className="text-xs text-gray-500">
          {published && magazine.publishedAt
            ? `발행 ${formatDateTime(magazine.publishedAt)}`
            : `수정 ${formatDateTime(magazine.updatedAt)}`}
        </p>
        {error != null && (
          <p role="alert" className="text-xs text-red-600">
            {getApiErrorMessage(error)}
          </p>
        )}
      </div>
      <div className="flex flex-wrap gap-2">
        {published && (
          <Link
            to="/magazines/$magazineId"
            params={{ magazineId: String(magazine.id) }}
            className={getButtonStyles({ variant: 'ghost', size: 'sm' })}
          >
            보기
          </Link>
        )}
        <Button
          variant="outline"
          size="sm"
          disabled={pending}
          onClick={() => onEdit({ magazineId: magazine.id, featuredUser: magazine.featuredUser })}
        >
          수정
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={pending}
          onClick={() => (published ? unpublish.mutate(magazine.id) : publish.mutate(magazine.id))}
        >
          {published ? '발행 취소' : '발행'}
        </Button>
        {confirmingDelete ? (
          <>
            <Button size="sm" disabled={pending} onClick={() => remove.mutate(magazine.id)}>
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
    </li>
  );
}

/** 수정할 매거진 원본을 받아 폼을 채운다. */
function MagazineEditForm({ editing, onDone }: { editing: Editing; onDone: () => void }) {
  const { data: magazine } = useSuspenseQuery(adminMagazineQuery(editing.magazineId));
  return (
    <MagazineForm
      magazine={magazine}
      featuredUser={{
        userId: magazine.featuredUserId,
        displayName: editing.featuredUser?.displayName ?? `사용자 #${magazine.featuredUserId}`,
        handle: editing.featuredUser?.handle,
      }}
      onDone={onDone}
    />
  );
}

interface MagazineFormValues {
  featuredUser: FeaturedUser | null;
  featuredUserIntroduction: string;
  title: string;
  summary: string;
  thumbnailMediaId: number | null;
  contentMd: string;
}

const EMPTY_FORM: MagazineFormValues = {
  featuredUser: null,
  featuredUserIntroduction: '',
  title: '',
  summary: '',
  thumbnailMediaId: null,
  contentMd: '',
};

/** 소개문은 초안에서는 선택이지만, 발행된 글을 수정할 때는 서버가 필수로 받는다. */
function validate(values: MagazineFormValues, published: boolean) {
  const errors: Partial<Record<keyof MagazineFormValues, string>> = {};
  if (!values.featuredUser) errors.featuredUser = '인터뷰이를 골라 주세요.';
  if (published && !values.featuredUserIntroduction.trim())
    errors.featuredUserIntroduction = '발행된 매거진은 인터뷰이 소개를 입력해야 해요.';
  else if (values.featuredUserIntroduction.length > INTRODUCTION_MAX)
    errors.featuredUserIntroduction = `인터뷰이 소개는 ${INTRODUCTION_MAX}자까지 입력할 수 있어요.`;
  if (!values.title.trim()) errors.title = '제목을 입력해 주세요.';
  else if (values.title.length > TITLE_MAX)
    errors.title = `제목은 ${TITLE_MAX}자까지 입력할 수 있어요.`;
  if (values.summary.length > SUMMARY_MAX)
    errors.summary = `요약은 ${SUMMARY_MAX}자까지 입력할 수 있어요.`;
  if (values.contentMd.length > CONTENT_MAX)
    errors.contentMd = `본문은 ${CONTENT_MAX.toLocaleString()}자까지 입력할 수 있어요.`;
  return errors;
}

/** 비어 있는 선택 필드는 null로 보낸다. 수정도 전체 교체라 같은 본문을 쓴다. */
function toSaveBody(values: MagazineFormValues): MagazineSaveBody {
  return {
    featuredUserId: values.featuredUser!.userId,
    featuredUserIntroduction: values.featuredUserIntroduction.trim() || null,
    title: values.title.trim(),
    summary: values.summary.trim() || null,
    thumbnailMediaId: values.thumbnailMediaId,
    contentMd: values.contentMd.trim() ? values.contentMd : null,
  };
}

function MagazineForm({
  magazine,
  featuredUser = null,
  onDone,
}: {
  magazine?: AdminMagazine;
  featuredUser?: FeaturedUser | null;
  onDone: () => void;
}) {
  const client = useQueryClient();
  const [values, setValues] = useState<MagazineFormValues>(() =>
    magazine
      ? {
          featuredUser,
          featuredUserIntroduction: magazine.featuredUserIntroduction ?? '',
          title: magazine.title,
          summary: magazine.summary ?? '',
          thumbnailMediaId: magazine.thumbnailMediaId ?? null,
          contentMd: magazine.contentMd ?? '',
        }
      : EMPTY_FORM,
  );
  const [errors, setErrors] = useState<ReturnType<typeof validate>>({});
  const published = magazine?.status === 'PUBLISHED';
  const onSuccess = () => {
    void client.invalidateQueries({ queryKey: adminQueryKeys.magazines });
    if (magazine) {
      void client.invalidateQueries({ queryKey: adminQueryKeys.magazine(magazine.id) });
      void client.invalidateQueries({ queryKey: ['magazine', magazine.id] });
    }
    void client.invalidateQueries({ queryKey: ['magazines'] });
    onDone();
  };
  const create = useMutation({ ...createMagazineMutation, onSuccess });
  const update = useMutation({ ...updateMagazineMutation, onSuccess });
  const save = magazine ? update : create;
  const baselineThumbnail = magazine?.thumbnailMediaId ?? null;

  const setField = <Key extends keyof MagazineFormValues>(
    field: Key,
    value: MagazineFormValues[Key],
  ) => setValues((current) => ({ ...current, [field]: value }));

  return (
    <section className="flex max-w-2xl flex-col gap-5">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-bold">
          {magazine ? `매거진 수정 #${magazine.id}` : '매거진 초안 등록'}
        </h2>
        {magazine && (
          <Button variant="ghost" size="sm" onClick={onDone}>
            수정 취소
          </Button>
        )}
      </div>
      <form
        className="flex flex-col gap-5"
        onSubmit={(event) => {
          event.preventDefault();
          if (save.isPending) return;
          const nextErrors = validate(values, published);
          setErrors(nextErrors);
          if (Object.keys(nextErrors).length > 0) return;
          const body = toSaveBody(values);
          if (magazine) update.mutate({ magazineId: magazine.id, body });
          else create.mutate(body);
        }}
      >
        <FeaturedUserField
          value={values.featuredUser}
          onChange={(featuredUser) => setField('featuredUser', featuredUser)}
          error={errors.featuredUser}
        />

        <Field
          label={published ? '인터뷰이 소개 *' : '인터뷰이 소개 (발행에 필요)'}
          error={errors.featuredUserIntroduction}
        >
          {(id) => (
            <textarea
              id={id}
              rows={2}
              maxLength={INTRODUCTION_MAX}
              value={values.featuredUserIntroduction}
              onChange={(event) => setField('featuredUserIntroduction', event.target.value)}
              placeholder="예: 우테코 4기 출신, 전 프론트엔드 코치"
              className={TEXTAREA_CLASS}
              aria-invalid={Boolean(errors.featuredUserIntroduction)}
            />
          )}
        </Field>

        <Field label="제목 *" error={errors.title}>
          {(id) => (
            <Input
              id={id}
              value={values.title}
              maxLength={TITLE_MAX}
              onChange={(event) => setField('title', event.target.value)}
              aria-invalid={Boolean(errors.title)}
            />
          )}
        </Field>

        <Field label="요약 (발행에 필요)" error={errors.summary}>
          {(id) => (
            <textarea
              id={id}
              rows={3}
              maxLength={SUMMARY_MAX}
              value={values.summary}
              onChange={(event) => setField('summary', event.target.value)}
              className={TEXTAREA_CLASS}
            />
          )}
        </Field>

        <div className="flex flex-col gap-2">
          {magazine?.thumbnailUrl && values.thumbnailMediaId === baselineThumbnail && (
            <Image
              src={magazine.thumbnailUrl}
              alt={`${magazine.title} 현재 대표 이미지`}
              className="aspect-video w-full max-w-xs rounded-lg bg-gray-100"
              fallback={<div className="aspect-video w-full max-w-xs rounded-lg bg-gray-100" />}
            />
          )}
          <ThumbnailField
            label={magazine ? '새 대표 이미지 올리기' : '대표 이미지 (발행에 필요)'}
            value={values.thumbnailMediaId === baselineThumbnail ? null : values.thumbnailMediaId}
            onChange={(mediaId) => setField('thumbnailMediaId', mediaId)}
            upload={uploadMagazineThumbnail}
          />
        </div>

        <ContentField
          value={values.contentMd}
          onChange={(contentMd) => setField('contentMd', contentMd)}
          error={errors.contentMd}
        />

        {magazine?.previewContentMd && (
          <details className="rounded-lg border border-gray-200 p-4">
            <summary className="cursor-pointer text-sm font-medium text-gray-900">
              저장된 본문 미리보기
            </summary>
            <div className="mt-4">
              <MarkdownContent preserveBlankLines>{magazine.previewContentMd}</MarkdownContent>
            </div>
          </details>
        )}

        {save.isError && (
          <p role="alert" className="text-sm text-red-600">
            {getApiErrorMessage(save.error)}
          </p>
        )}
        <Button type="submit" size="lg" disabled={save.isPending}>
          {save.isPending ? '저장 중…' : magazine ? '수정 저장' : '초안 등록'}
        </Button>
      </form>
    </section>
  );
}

function FeaturedUserField({
  value,
  onChange,
  error,
}: {
  value: FeaturedUser | null;
  onChange: (next: FeaturedUser | null) => void;
  error?: string;
}) {
  const [keyword, setKeyword] = useState('');
  const trimmed = useDebouncedValue(keyword.trim(), 300);
  const { data: users = [], isFetching } = useQuery({
    ...crewSearchQueryOptions(trimmed),
    enabled: value === null && trimmed.length > 0,
  });

  if (value) {
    return (
      <div className="flex flex-col gap-2">
        <p className="text-sm font-medium text-gray-900">인터뷰이 *</p>
        <div className="flex items-center gap-3 rounded-lg bg-gray-50 px-4 py-3">
          <span className="min-w-0 flex-1 truncate text-sm font-semibold text-gray-900">
            {value.displayName}
            {value.handle && (
              <span className="ml-1.5 font-normal text-gray-500">
                @{value.handle.replace(/^@/, '')}
              </span>
            )}
          </span>
          <Button variant="ghost" size="sm" onClick={() => onChange(null)}>
            다시 고르기
          </Button>
        </div>
      </div>
    );
  }

  return (
    <Field label="인터뷰이 *" error={error}>
      {(id) => (
        <div className="flex flex-col gap-2">
          <Input
            id={id}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="이름 또는 닉네임 검색"
            aria-invalid={Boolean(error)}
          />
          {trimmed &&
            (isFetching ? (
              <p role="status" className="text-xs text-gray-500">
                검색 중…
              </p>
            ) : users.length === 0 ? (
              <p className="text-xs text-gray-500">검색 결과가 없습니다.</p>
            ) : (
              <ul aria-label="인터뷰이 검색 결과" className="flex flex-col gap-1">
                {users.map((user) => (
                  <li key={user.userId}>
                    <button
                      type="button"
                      onClick={() =>
                        onChange({
                          userId: user.userId,
                          displayName: user.displayName,
                          handle: user.handle,
                        })
                      }
                      className="focus-visible:outline-primary-600 flex w-full cursor-pointer items-center gap-3 rounded-lg px-3 py-2 text-left hover:bg-gray-100 focus-visible:outline-2"
                    >
                      <Avatar size="sm" src={user.avatarUrl} name={user.displayName} alt="" />
                      <span className="truncate text-sm font-semibold text-gray-900">
                        {user.displayName}
                      </span>
                      <span className="truncate text-xs text-gray-500">
                        @{user.handle.replace(/^@/, '')}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            ))}
        </div>
      )}
    </Field>
  );
}

/**
 * 본문 Markdown 입력. 이미지를 고르면 올린 뒤 커서 위치에 `media://ID` 참조를 넣는다.
 * 서버는 본문 이미지를 이 참조로만 받는다.
 */
function ContentField({
  value,
  onChange,
  error,
}: {
  value: string;
  onChange: (next: string) => void;
  error?: string;
}) {
  const textarea = useRef<HTMLTextAreaElement>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [failed, setFailed] = useState(false);

  const insertImage = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    // 업로드를 기다리는 동안 입력이 바뀔 수 있어, 고른 순간의 커서 위치를 기억한다.
    const position = textarea.current?.selectionStart ?? value.length;
    setUploading(true);
    setFailed(false);
    try {
      const mediaId = await uploadMagazineContentImage(file);
      const snippet = `\n![이미지 설명](media://${mediaId} "캡션")\n`;
      onChange(value.slice(0, position) + snippet + value.slice(position));
    } catch {
      setFailed(true);
    } finally {
      setUploading(false);
    }
  };

  return (
    <Field label="본문 (Markdown, 발행에 필요)" error={error}>
      {(id) => (
        <div className="flex flex-col gap-2">
          <div className="flex flex-wrap items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={uploading}
              onClick={() => fileInput.current?.click()}
            >
              {uploading ? '이미지 올리는 중…' : '본문 이미지 넣기'}
            </Button>
            <input
              ref={fileInput}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              className="hidden"
              aria-label="본문 이미지 파일"
              onChange={insertImage}
            />
            {failed && <p className="text-xs text-red-600">이미지 업로드에 실패했습니다.</p>}
          </div>
          <textarea
            ref={textarea}
            id={id}
            rows={16}
            value={value}
            onChange={(event) => onChange(event.target.value)}
            placeholder="Markdown"
            className={`${TEXTAREA_CLASS} font-mono`}
            aria-invalid={Boolean(error)}
          />
          <p className="text-xs leading-5 text-gray-500">
            이미지 캡션: <code>![설명](media://ID "캡션")</code> · 이미지 크기:{' '}
            <code>![설명|300](...)</code>, <code>|50%</code>, <code>|300x200</code> · 글자 색:{' '}
            <code>&lt;font color="red"&gt;강조&lt;/font&gt;</code>, 다른 태그에도 <code>color</code>{' '}
            속성 사용 가능 (red, blue, green, yellow, gray, primary)
          </p>
        </div>
      )}
    </Field>
  );
}
