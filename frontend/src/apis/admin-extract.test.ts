import { extractJobPosting, JOB_POSTING_EXTRACT_TIMEOUT_MS } from '@/apis/admin';
import { httpClient } from '@/utils/client';

jest.mock('@/utils/client', () => ({ httpClient: jest.fn() }));

test('채용공고 추출 요청만 제한 시간을 60초로 늘리고 전체 제한 시간도 함께 늘린다', async () => {
  jest.mocked(httpClient).mockResolvedValue({
    status: 'success',
    data: { jobRole: 'BACKEND', requirements: [] },
  });

  await extractJobPosting({ sourceUrl: 'https://example.com/jobs/1', content: '본문' });

  expect(JOB_POSTING_EXTRACT_TIMEOUT_MS).toBe(60_000);
  expect(httpClient).toHaveBeenCalledWith(
    '/api/v1/admin/job-postings/extractions',
    expect.objectContaining({ method: 'post', timeout: 60_000, totalTimeout: 60_000 }),
  );
});
