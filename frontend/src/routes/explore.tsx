import { createFileRoute, redirect } from '@tanstack/react-router';

// 탐색 화면은 홈(/)으로 옮겼다. 예전 주소로 들어와도 검색어와 탭을 그대로 들고 홈으로 간다.
export const Route = createFileRoute('/explore')({
  beforeLoad: ({ search }) => {
    throw redirect({ to: '/', search, replace: true });
  },
});
