import { useNavigate } from '@tanstack/react-router';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { IconDotsVertical } from '@tabler/icons-react';
import { useState } from 'react';
import { deleteFeedMutation } from '@/apis/feed';
import { sessionQuery } from '@/apis/session';
import { myProfileSummaryQuery } from '@/apis/user';
import { Button } from '@/components/Button';
import { Dropdown } from '@/components/Dropdown';
import { getApiErrorMessage } from '@/utils/error';

export function FeedMenu({ authorHandle, feedId }: { authorHandle: string; feedId: number }) {
  const { data: session } = useSuspenseQuery(sessionQuery);
  if (session.status !== 'AUTHENTICATED' || session.userId === null) return null;

  return <AuthorMenu feedId={feedId} authorHandle={authorHandle} userId={session.userId} />;
}

function AuthorMenu({
  authorHandle,
  userId,
  feedId,
}: {
  authorHandle: string;
  userId: number;
  feedId: number;
}) {
  const navigate = useNavigate();
  const client = useQueryClient();
  const mutation = useMutation(deleteFeedMutation(feedId));
  const [confirming, setConfirming] = useState(false);
  const { data: profile } = useSuspenseQuery(myProfileSummaryQuery(userId));
  if (profile.handle !== authorHandle) return null;

  async function remove() {
    try {
      await mutation.mutateAsync();
      client.removeQueries({ queryKey: ['feed', feedId], exact: true });
      void client.invalidateQueries({ queryKey: ['feeds'] });
      // TODO 프로필 피드 탭은 ['users', handle, 'feeds']로 따로 캐시된다.
      // api 폴더를 정리할 때 피드 캐시 키를 한 규칙으로 맞추고 이 줄을 없앤다.
      void client.invalidateQueries({ queryKey: ['users'] });
      void navigate({ to: '/feeds', search: { sort: 'LATEST' } });
    } catch {
      // Mutation의 오류를 확인 UI에 표시한다.
    }
  }

  return (
    <div className="relative shrink-0">
      <Dropdown
        trigger={<IconDotsVertical className="size-4" aria-hidden="true" />}
        aria-label="피드 메뉴"
      >
        <Dropdown.Item
          onSelect={() =>
            void navigate({ to: '/feeds/$feedId/edit', params: { feedId: String(feedId) } })
          }
        >
          수정
        </Dropdown.Item>
        <Dropdown.Item onSelect={() => setConfirming(true)}>삭제</Dropdown.Item>
      </Dropdown>
      {confirming && (
        <div
          role="group"
          aria-label="피드 삭제 확인"
          className="bg-background absolute right-0 z-50 mt-2 w-64 rounded-lg border border-gray-200 p-3 shadow-lg"
        >
          <p className="text-sm text-gray-900">이 피드를 삭제할까요?</p>
          {mutation.isError && (
            <p role="alert" className="mt-2 text-sm text-red-600">
              {getApiErrorMessage(mutation.error)}
            </p>
          )}
          <div className="mt-3 flex justify-end gap-2">
            <Button
              variant="ghost"
              size="sm"
              disabled={mutation.isPending}
              onClick={() => setConfirming(false)}
            >
              취소
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={mutation.isPending}
              onClick={() => void remove()}
            >
              {mutation.isPending ? '삭제 중…' : '삭제 확인'}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
