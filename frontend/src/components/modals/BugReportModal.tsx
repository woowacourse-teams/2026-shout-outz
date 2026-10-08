import { useId, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { IconCircleCheckFilled } from '@tabler/icons-react';

import { BUG_REPORT_MAX_LENGTH, createBugReportMutation } from '@/apis/bug-report';
import { sessionQuery } from '@/apis/session';
import { Button } from '@/components/Button';
import { SelectionModal } from '@/components/modals/SelectionModal';
import { getApiErrorMessage } from '@/utils/error';

export interface BugReportModalProps {
  /** 제보한 화면의 경로. 운영진이 어디서 생긴 문제인지 알 수 있게 내용 끝에 붙인다. */
  pagePath: string;
  onClose: () => void;
}

const PAGE_NOTE_PREFIX = '\n\n---\n제보한 화면: ';

/**
 * 버그 제보 입력 창. 로그인하지 않아도 보낼 수 있다.
 *
 * 보내고 나면 같은 창에서 접수됐다고 알리고 닫기만 남긴다.
 */
export function BugReportModal({ pagePath, onClose }: BugReportModalProps) {
  const formId = useId();
  const contentId = useId();
  const client = useQueryClient();
  const mutation = useMutation(createBugReportMutation);
  const [content, setContent] = useState('');
  const [error, setError] = useState<string>();
  const pageNote = `${PAGE_NOTE_PREFIX}${pagePath}`;
  // 서버는 붙인 화면 경로까지 합쳐 5000자를 센다.
  const maxLength = BUG_REPORT_MAX_LENGTH - Array.from(pageNote).length;
  const length = Array.from(content.trim()).length;

  async function submit() {
    if (mutation.isPending) return;
    if (length === 0) return setError('어떤 문제가 있었는지 적어 주세요.');
    if (length > maxLength) return setError(`${maxLength}자 이하로 적어 주세요.`);
    setError(undefined);

    try {
      // 로그인하지 않은 사람도 CSRF 토큰이 있어야 보낼 수 있다. 세션 조회가 토큰을 받아 둔다.
      await client.ensureQueryData(sessionQuery);
      await mutation.mutateAsync({ content: `${content.trim()}${pageNote}` });
    } catch {
      // 오류 문구는 mutation.error로 보여 준다.
    }
  }

  if (mutation.isSuccess) {
    return (
      <SelectionModal title="버그 제보" onClose={onClose}>
        <div className="flex flex-col items-center gap-3 py-6 text-center">
          <IconCircleCheckFilled className="size-12 text-green-500" aria-hidden="true" />
          <p role="status" className="text-lg font-bold">
            제보가 접수됐어요
          </p>
          <p className="text-sm text-gray-600">알려 주셔서 고마워요. 확인한 뒤 고칠게요.</p>
          <Button className="mt-2 min-w-24" onClick={onClose}>
            확인
          </Button>
        </div>
      </SelectionModal>
    );
  }

  return (
    <SelectionModal
      title="버그 제보"
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
        <label htmlFor={contentId} className="text-sm font-medium text-gray-900">
          어떤 문제가 있었나요?
        </label>
        <textarea
          id={contentId}
          data-modal-autofocus
          value={content}
          rows={6}
          placeholder="어느 화면에서 무엇을 했을 때 어떤 일이 생겼는지 적어 주세요."
          disabled={mutation.isPending}
          aria-invalid={error ? true : undefined}
          aria-describedby={`${contentId}-help`}
          onChange={(event) => setContent(event.target.value)}
          className="focus-visible:outline-primary-600 block w-full resize-y rounded-lg bg-gray-100 px-4 py-3 text-sm leading-6 text-gray-900 outline-none placeholder:text-gray-500 focus-visible:outline-2"
        />
        <div
          id={`${contentId}-help`}
          className="flex items-start justify-between gap-3 text-xs leading-5 text-gray-500"
        >
          <span>지금 보고 있는 화면 주소({pagePath})가 함께 전송돼요.</span>
          <span className={length > maxLength ? 'shrink-0 text-red-600' : 'shrink-0'}>
            {length.toLocaleString()} / {maxLength.toLocaleString()}
          </span>
        </div>
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
