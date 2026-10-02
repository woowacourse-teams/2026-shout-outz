import { useRouter, type ErrorComponentProps } from '@tanstack/react-router';
import { isHTTPError } from 'ky';

import { Button } from '@/components/Button';

export function RouteError({ error }: ErrorComponentProps) {
  const router = useRouter();
  const loginRequired = isHTTPError(error) && error.response.status === 401;

  return (
    <main role="alert" className="px-4 pt-5 pb-7 md:px-16 md:pt-10 md:pb-20">
      <p className="text-sm text-gray-600">
        {loginRequired ? '로그인이 필요합니다.' : '화면을 불러오지 못했습니다.'}
      </p>
      {!loginRequired && (
        <Button variant="outline" className="mt-3" onClick={() => void router.invalidate()}>
          다시 시도
        </Button>
      )}
    </main>
  );
}
