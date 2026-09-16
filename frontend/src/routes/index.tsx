import { Suspense } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { HomePage } from '@/pages/HomePage';

export const Route = createFileRoute('/')({
  component: RouteComponent,
  errorComponent: HomeError,
});

function RouteComponent() {
  return (
    <Suspense fallback={<HomeMessage>홈 화면을 불러오는 중…</HomeMessage>}>
      <HomePage />
    </Suspense>
  );
}

function HomeError() {
  return <HomeMessage>홈 화면을 불러오지 못했습니다.</HomeMessage>;
}

function HomeMessage({ children }: { children: string }) {
  return (
    <main className="px-4 pt-5 pb-7 md:px-16 md:pt-10 md:pb-20">
      <p className="text-sm text-gray-600">{children}</p>
    </main>
  );
}
