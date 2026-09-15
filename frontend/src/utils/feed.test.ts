import { type FeedAuthor } from '@/types/feed';
import { formatAuthorLabel } from '@/utils/feed';

const CREW: FeedAuthor = {
  handle: 'hoik',
  displayName: '황호익',
  userType: 'WOOWACOURSE_CREW',
  track: 'BACKEND',
  cohort: 6,
  avatarImageId: null,
};

describe('formatAuthorLabel', () => {
  it('크루는 이름 옆에 기수와 트랙을 붙인다', () => {
    expect(formatAuthorLabel(CREW)).toBe('황호익 · 6기 백엔드');
  });

  it.each([
    ['BACKEND', '백엔드'],
    ['FRONTEND', '프론트엔드'],
  ])('트랙 %s를 %s로 옮긴다', (track, label) => {
    expect(formatAuthorLabel({ ...CREW, track })).toBe(`황호익 · 6기 ${label}`);
  });

  it('기수와 트랙이 없는 일반 사용자는 이름만 보여준다', () => {
    expect(formatAuthorLabel({ ...CREW, userType: 'GENERAL', track: null, cohort: null })).toBe(
      '황호익',
    );
  });

  it('한글 표기를 모르는 트랙이면 이름만 보여준다', () => {
    expect(formatAuthorLabel({ ...CREW, track: 'ANDROID' })).toBe('황호익');
  });

  it('기수와 트랙 중 하나라도 없으면 이름만 보여준다', () => {
    expect(formatAuthorLabel({ ...CREW, track: null })).toBe('황호익');
    expect(formatAuthorLabel({ ...CREW, cohort: null })).toBe('황호익');
  });
});
