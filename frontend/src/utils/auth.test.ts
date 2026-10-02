import { normalizeApiOrigin } from '@/utils/auth';

describe('normalizeApiOrigin', () => {
  it('프로토콜이 없는 로컬 주소에 http 프로토콜을 추가한다', () => {
    expect(normalizeApiOrigin('localhost:8080')).toBe('http://localhost:8080');
  });

  it('기존 프로토콜은 유지하고 마지막 슬래시는 제거한다', () => {
    expect(normalizeApiOrigin('https://api.example.com/')).toBe('https://api.example.com');
  });
});
