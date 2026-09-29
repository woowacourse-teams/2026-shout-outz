/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { http, HttpResponse } from 'msw';

import { updateMyProfile } from '@/apis/user';
import { server } from '@/test/renderRoute';

const INPUT = { displayName: '정우진', avatarImageId: 7 };

const notReady = () =>
  HttpResponse.json(
    {
      status: 'error',
      code: 'AVATAR_IMAGE_NOT_READY',
      message: '프로필 이미지 처리가 완료되지 않았습니다.',
    },
    { status: 400 },
  );

/** 앞의 `failures`번만 실패시키고 그 뒤에는 성공한다. 호출 횟수를 함께 돌려준다. */
const respondNotReady = (failures: number) => {
  const calls = { count: 0 };
  server.use(
    http.put('/api/v1/users/me', () => {
      calls.count += 1;
      if (calls.count <= failures) return notReady();
      return HttpResponse.json({ status: 'success', data: { displayName: '정우진' } });
    }),
  );
  return calls;
};

describe('updateMyProfile', () => {
  // complete가 200을 줘도 서버 안에서는 아직 이미지를 처리하는 중일 수 있다.
  it('이미지 처리가 안 끝났으면 기다렸다 다시 보낸다', async () => {
    const calls = respondNotReady(1);

    await expect(updateMyProfile(INPUT)).resolves.toEqual({ displayName: '정우진' });
    expect(calls.count).toBe(2);
  });

  it('여러 번 안 끝나도 끝날 때까지 기다린다', async () => {
    const calls = respondNotReady(3);

    await expect(updateMyProfile(INPUT)).resolves.toBeDefined();
    expect(calls.count).toBe(4);
  });

  it('끝내 처리되지 않으면 오류를 그대로 올린다', async () => {
    const calls = respondNotReady(99);

    await expect(updateMyProfile(INPUT)).rejects.toThrow();
    // 첫 시도 + 대기 간격 수만큼만 다시 보낸다. 무한히 매달리지 않는다.
    expect(calls.count).toBe(4);
  });

  it('다른 오류는 기다리지 않고 바로 올린다', async () => {
    const calls = { count: 0 };
    server.use(
      http.put('/api/v1/users/me', () => {
        calls.count += 1;
        return HttpResponse.json(
          { status: 'error', code: 'VALIDATION_FAILED', message: '표시 이름을 확인해 주세요.' },
          { status: 400 },
        );
      }),
    );

    await expect(updateMyProfile(INPUT)).rejects.toThrow();
    expect(calls.count).toBe(1);
  });
});
