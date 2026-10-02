import { Suspense } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { VerificationRequestPage } from '@/pages/VerificationRequestPage';

export const Route = createFileRoute('/mypage/verification')({
  component: VerificationRequestRoute,
});

function VerificationRequestRoute() {
  return (
    <Suspense
      fallback={<main className="p-8 text-sm text-gray-600">인증 상태를 확인하는 중…</main>}
    >
      <VerificationRequestPage />
    </Suspense>
  );
}
