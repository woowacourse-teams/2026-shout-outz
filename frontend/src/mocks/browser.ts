import { setupWorker } from 'msw/browser';
import { http, HttpResponse } from 'msw';
import { handlers } from '@/api/mock/handlers';
import { createFeedHandlers } from '@/mocks/handlers';
import { adminInterviewHandlers, interviewHandlers } from '@/mocks/interviews';

const MOCK_LOGGED_OUT_KEY = 'shoutoutz:mock-logged-out';

export const worker = setupWorker(
  // 브라우저 목에서도 로그아웃 후 비로그인 UI를 확인할 수 있게 합니다.
  // 같은 탭에서 새로고침해도 유지하며, 공용 테스트 핸들러에는 영향을 주지 않습니다.
  http.get('/api/v1/auth/session', () =>
    HttpResponse.json({
      status: 'success',
      data:
        sessionStorage.getItem(MOCK_LOGGED_OUT_KEY) === 'true'
          ? { status: 'UNAUTHENTICATED', userId: null, role: null, csrfToken: null }
          : { status: 'AUTHENTICATED', userId: 1, role: 'USER', csrfToken: 'development-token' },
    }),
  ),
  http.post('/api/v1/auth/logout', () => {
    sessionStorage.setItem(MOCK_LOGGED_OUT_KEY, 'true');
    return new HttpResponse(null, { status: 204 });
  }),
  ...createFeedHandlers({ includeProfile: false, profileCases: true, recommendations: true }),
  ...interviewHandlers,
  ...adminInterviewHandlers,
  ...handlers,
);
