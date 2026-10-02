/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { http, HttpResponse } from 'msw';

import { uploadAvatar, waitUntilReady } from '@/api/media';
import { server } from '@/test/renderRoute';

const MEDIA_ID = 7;
const UPLOAD_URL = 'https://storage.test/put';

type Status = 'PENDING_UPLOAD' | 'PROCESSING' | 'READY' | 'FAILED' | 'EXPIRED';

/**
 * 업로드 3단계를 받아 주고, 상태 조회는 주어진 순서대로 돌려준다.
 * 목록을 다 쓰면 마지막 값을 계속 준다.
 */
const mockUpload = (statuses: Status[]) => {
  const polls = { count: 0 };

  server.use(
    http.post('/api/v1/media/uploads', () =>
      HttpResponse.json({ mediaId: MEDIA_ID, uploadUrl: UPLOAD_URL, contentType: 'image/png' }),
    ),
    http.put(UPLOAD_URL, () => new HttpResponse(null)),
    http.post(`/api/v1/media/${MEDIA_ID}/complete`, () => new HttpResponse(null, { status: 204 })),
    http.get(`/api/v1/media/${MEDIA_ID}/status`, () => {
      const status = statuses[Math.min(polls.count, statuses.length - 1)]!;
      polls.count += 1;
      return HttpResponse.json({ status: 'success', data: { mediaId: MEDIA_ID, status } });
    }),
  );

  return polls;
};

const file = () => new File(['x'], 'me.png', { type: 'image/png' });

describe('uploadAvatar', () => {
  it('처리가 끝나면 mediaId를 돌려준다', async () => {
    mockUpload(['READY']);

    await expect(uploadAvatar(file())).resolves.toBe(MEDIA_ID);
  });

  // complete는 처리를 큐에 넣고 바로 응답한다. 그래서 곧바로 READY가 아닐 수 있다.
  it('아직 처리 중이면 READY가 될 때까지 기다린다', async () => {
    const polls = mockUpload(['PROCESSING', 'PROCESSING', 'READY']);

    await expect(uploadAvatar(file())).resolves.toBe(MEDIA_ID);
    expect(polls.count).toBe(3);
  });

  // 더 기다려도 바뀌지 않는 상태다. 예산을 끝까지 쓰지 않고 바로 알린다.
  it.each(['FAILED', 'EXPIRED'] as const)('%s면 기다리지 않고 실패한다', async (status) => {
    const polls = mockUpload([status]);

    await expect(uploadAvatar(file())).rejects.toThrow(status);
    expect(polls.count).toBe(1);
  });

  it('끝내 안 끝나면 mediaId를 담은 오류로 끊는다', async () => {
    // PROCESSING만 계속 오면 예산을 다 쓰고 멈춘다. 무한히 매달리지 않는다.
    mockUpload(['PROCESSING']);

    // 업로드는 이미 끝났으므로, 호출부가 파일을 다시 올리지 않고 이어받을 수 있어야 한다.
    await expect(uploadAvatar(file(), { timeoutMs: 400 })).rejects.toMatchObject({
      name: 'MediaNotReadyError',
      mediaId: MEDIA_ID,
    });
  });

  it('오래 걸리면 기다리는 도중에 알린다', async () => {
    mockUpload(['PROCESSING']);
    const onSlow = jest.fn();

    await expect(
      uploadAvatar(file(), { timeoutMs: 600, slowNoticeMs: 100, onSlow }),
    ).rejects.toThrow();

    // 한 번만 알린다. 폴링마다 화면이 깜빡이면 안 된다.
    expect(onSlow).toHaveBeenCalledTimes(1);
  });
});

describe('waitUntilReady', () => {
  it('업로드 없이 상태만 이어서 확인한다', async () => {
    const polls = mockUpload(['PROCESSING', 'READY']);

    await expect(waitUntilReady(MEDIA_ID)).resolves.toBeUndefined();
    expect(polls.count).toBe(2);
  });
});
