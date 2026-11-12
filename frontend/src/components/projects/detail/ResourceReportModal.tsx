import { useId, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { IconCircleCheckFilled } from '@tabler/icons-react';

import {
  myResourceReportsQuery,
  RESOURCE_REPORT_MEMO_MAX_LENGTH,
  resourceAdditionMutation,
  resourceRemovalMutation,
} from '@/apis/resource-report';
import { Button } from '@/components/Button';
import { SelectionModal } from '@/components/modals/SelectionModal';
import { getApiErrorMessage } from '@/utils/error';

const fieldClass =
  'focus-visible:outline-primary-600 block w-full rounded-lg bg-gray-100 px-4 py-3 text-sm leading-6 text-gray-900 outline-none placeholder:text-gray-500 focus-visible:outline-2';

type ResourceReportModalProps = {
  slug: string;
  onClose: () => void;
} & (
  | { kind: 'addition' }
  | {
      kind: 'removal';
      /** 필요 없다고 고른 자료 ID */
      resourceIds: number[];
      /** 다 보낸 뒤 고르기를 끝낸다. */
      onDone: () => void;
    }
);

/**
 * 프로젝트 팀원의 자료 제보 창.
 *
 * - addition: 빠진 PR·팀 문서의 GitHub 주소와 메모를 보낸다.
 * - removal: 고른 PR·팀 문서에 붙일 메모를 받아 "필요 없어요"로 보낸다.
 *
 * 보내고 나면 버그 제보처럼 같은 창에서 접수됐다고 알린다.
 */
export function ResourceReportModal(props: ResourceReportModalProps) {
  const { slug, onClose } = props;
  const formId = useId();
  const urlId = useId();
  const memoId = useId();
  const client = useQueryClient();
  const addition = useMutation(resourceAdditionMutation(slug));
  const removal = useMutation(resourceRemovalMutation(slug));
  const mutation = props.kind === 'addition' ? addition : removal;
  const [url, setUrl] = useState('');
  const [memo, setMemo] = useState('');
  const [error, setError] = useState<string>();
  const memoLength = Array.from(memo.trim()).length;
  const title = props.kind === 'addition' ? '빠진 자료 알려 주기' : '필요 없는 자료 알려 주기';

  async function submit() {
    if (mutation.isPending) return;
    if (props.kind === 'addition' && !url.trim()) return setError('GitHub 주소를 적어 주세요.');
    if (memoLength > RESOURCE_REPORT_MEMO_MAX_LENGTH)
      return setError(`메모는 ${RESOURCE_REPORT_MEMO_MAX_LENGTH}자 이하로 적어 주세요.`);
    setError(undefined);

    const trimmedMemo = memo.trim() || null;
    try {
      if (props.kind === 'addition') {
        await addition.mutateAsync({ url: url.trim(), memo: trimmedMemo });
      } else {
        await removal.mutateAsync({ resourceIds: props.resourceIds, memo: trimmedMemo });
      }
      // 보낸 자료에 "필요 없어요 보냄"을 붙이도록 내 제보 목록을 다시 받는다.
      await client.invalidateQueries({ queryKey: myResourceReportsQuery(slug).queryKey });
    } catch {
      // 오류 문구는 mutation.error로 보여 준다.
    }
  }

  const close = () => {
    if (props.kind === 'removal' && mutation.isSuccess) props.onDone();
    onClose();
  };

  if (mutation.isSuccess) {
    return (
      <SelectionModal title={title} onClose={close}>
        <div className="flex flex-col items-center gap-3 py-6 text-center">
          <IconCircleCheckFilled className="size-12 text-green-500" aria-hidden="true" />
          <p role="status" className="text-lg font-bold">
            제보가 접수됐어요
          </p>
          <p className="text-sm text-gray-600">관리자가 확인한 뒤 자료에 반영할게요.</p>
          <Button className="mt-2 min-w-24" onClick={close}>
            확인
          </Button>
        </div>
      </SelectionModal>
    );
  }

  return (
    <SelectionModal
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            취소
          </Button>
          <Button
            type="submit"
            form={formId}
            size="lg"
            className="flex-1 md:flex-none"
            disabled={mutation.isPending}
          >
            {mutation.isPending ? '보내는 중…' : '보내기'}
          </Button>
        </>
      }
    >
      <form
        id={formId}
        className="flex flex-col gap-2"
        onSubmit={(event) => {
          event.preventDefault();
          void submit();
        }}
      >
        {props.kind === 'addition' ? (
          <>
            <label htmlFor={urlId} className="text-sm font-medium text-gray-900">
              빠진 PR이나 팀 문서의 GitHub 주소
            </label>
            <input
              id={urlId}
              type="url"
              data-modal-autofocus
              value={url}
              maxLength={500}
              placeholder="https://github.com/woowacourse-teams/…/pull/123"
              disabled={mutation.isPending}
              onChange={(event) => setUrl(event.target.value)}
              className={fieldClass}
            />
            <p className="text-xs leading-5 text-gray-500">
              이 프로젝트 리포지토리의 PR, 위키, 디스커션, 문서 주소만 받아요.
            </p>
          </>
        ) : (
          <p className="text-sm text-gray-700">
            고른 자료 <b className="font-semibold text-gray-900">{props.resourceIds.length}개</b>를
            필요 없는 자료로 알려요.
          </p>
        )}
        <label htmlFor={memoId} className="mt-3 text-sm font-medium text-gray-900">
          {props.kind === 'addition' ? '왜 들어가면 좋은지' : '왜 필요 없는지'}{' '}
          <span className="font-normal text-gray-500">(선택)</span>
        </label>
        <textarea
          id={memoId}
          data-modal-autofocus={props.kind === 'removal' ? true : undefined}
          value={memo}
          rows={4}
          disabled={mutation.isPending}
          onChange={(event) => setMemo(event.target.value)}
          className={`${fieldClass} resize-y`}
        />
        <p className="text-right text-xs text-gray-500">
          <span className={memoLength > RESOURCE_REPORT_MEMO_MAX_LENGTH ? 'text-red-600' : ''}>
            {memoLength}
          </span>{' '}
          / {RESOURCE_REPORT_MEMO_MAX_LENGTH}
        </p>
        {error && <p className="text-xs text-red-600">{error}</p>}
        {mutation.isError && (
          <p role="alert" className="text-sm text-red-600">
            {getApiErrorMessage(mutation.error)}
          </p>
        )}
      </form>
    </SelectionModal>
  );
}
