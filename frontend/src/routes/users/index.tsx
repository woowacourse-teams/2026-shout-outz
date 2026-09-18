import { createFileRoute, redirect } from '@tanstack/react-router';

import { fetchMyProfileSummary } from '@/apis/user';

/**
 * `/users`로 들어오면 내 공개 프로필(`/users/{내 handle}`)로 보낸다.
 *
 * 로그인한 사람의 handle은 `GET /api/v1/users/me/summary`로만 알 수 있어서 이동 전에 한 번 조회한다.
 * 비로그인이면 서버가 401을 주고 아래 안내 화면이 뜬다.
 */
export const Route = createFileRoute('/users/')({
  beforeLoad: async () => {
    const me = await fetchMyProfileSummary();

    throw redirect({ to: '/users/$handle', params: { handle: me.handle } });
  },
  errorComponent: MyProfileError,
});

function MyProfileError() {
  return (
    <main className="px-4 pt-5 pb-7 md:px-16 md:pt-10 md:pb-20">
      <p className="text-sm text-gray-600">로그인이 필요합니다.</p>
    </main>
  );
}
