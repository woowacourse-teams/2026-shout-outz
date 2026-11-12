import { QueryClientProvider, type QueryClient } from '@tanstack/react-query';
import { RouterProvider, type AnyRouter } from '@tanstack/react-router';
import { OverlayProvider } from 'overlay-kit';

import { ModalProvider } from '@/components/ModalProvider';

export function App({ router, queryClient }: { router: AnyRouter; queryClient: QueryClient }) {
  return (
    <QueryClientProvider client={queryClient}>
      <ModalProvider>
        {/* 새 디자인의 모달은 overlay-kit으로 연다. 기존 모달은 ModalProvider를 그대로 쓴다. */}
        <OverlayProvider>
          <RouterProvider router={router} />
        </OverlayProvider>
      </ModalProvider>
    </QueryClientProvider>
  );
}
