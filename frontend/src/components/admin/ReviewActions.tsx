import { useState } from 'react';

import { Button } from '@/components/Button';
import { Input } from '@/components/Input';
import { getApiErrorMessage } from '@/utils/error';

const REASON_MAX_LENGTH = 100;

/**
 * 심사 대기 항목의 승인·반려 버튼.
 *
 * 반려를 누르면 사유 입력이 열리고, 사유를 적어야 반려가 확정된다. 우아한테크코스 소속 인증과 프로젝트 심사가 함께 쓴다.
 */
export interface ReviewActionsProps {
  /** 스크린리더가 어느 항목의 버튼인지 알 수 있게 붙이는 이름 */
  subject: string;
  pending: boolean;
  error: unknown;
  onApprove: () => void;
  onReject: (reason: string) => void;
}

export function ReviewActions({
  subject,
  pending,
  error,
  onApprove,
  onReject,
}: ReviewActionsProps) {
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState('');
  const trimmed = reason.trim();

  return (
    <div className="flex flex-col gap-2">
      {rejecting ? (
        <form
          className="flex flex-col gap-2 md:flex-row"
          onSubmit={(event) => {
            event.preventDefault();
            if (!trimmed || pending) return;
            onReject(trimmed);
          }}
        >
          <Input
            size="sm"
            value={reason}
            maxLength={REASON_MAX_LENGTH}
            onChange={(event) => setReason(event.target.value)}
            placeholder="반려 사유 (100자 이내)"
            aria-label={`${subject} 반려 사유`}
          />
          <div className="flex gap-2">
            <Button type="submit" size="sm" disabled={!trimmed || pending}>
              반려 확정
            </Button>
            <Button variant="outline" size="sm" onClick={() => setRejecting(false)}>
              취소
            </Button>
          </div>
        </form>
      ) : (
        <div className="flex gap-2">
          <Button size="sm" disabled={pending} onClick={onApprove} aria-label={`${subject} 승인`}>
            승인
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={pending}
            onClick={() => setRejecting(true)}
            aria-label={`${subject} 반려`}
          >
            반려
          </Button>
        </div>
      )}
      {error != null && (
        <p role="alert" className="text-xs text-red-600">
          {getApiErrorMessage(error)}
        </p>
      )}
    </div>
  );
}
