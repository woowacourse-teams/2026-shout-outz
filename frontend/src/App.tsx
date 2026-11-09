import { QueryClientProvider, type QueryClient } from '@tanstack/react-query';
import { RouterProvider, type AnyRouter } from '@tanstack/react-router';

import { ModalProvider } from '@/components/ModalProvider';

export function App({ router, queryClient }: { router: AnyRouter; queryClient: QueryClient }) {
  return (
    <QueryClientProvider client={queryClient}>
      <ModalProvider>
        <RouterProvider router={router} />
      </ModalProvider>
    </QueryClientProvider>
  );
}
