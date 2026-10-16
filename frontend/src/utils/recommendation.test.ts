import { mockRecommendations } from '@/mocks/recommendations';
import {
  clearLoginQuestionDraft,
  pickRecommendations,
  readLoginQuestionDraft,
  readRecommendationNavigation,
  saveLoginQuestionDraft,
} from '@/utils/recommendation';

const context = {
  recommendation: mockRecommendations[0]!,
  from: '/community?type=QUESTION&sort=POPULAR',
};

afterEach(() => {
  sessionStorage.clear();
  jest.restoreAllMocks();
});

it('원본을 변경하지 않고 중복 없는 추천 두 개를 무작위로 고른다', () => {
  const original = [...mockRecommendations];
  jest.spyOn(Math, 'random').mockReturnValue(0);
  const first = pickRecommendations(mockRecommendations);
  jest.mocked(Math.random).mockReturnValue(0.99);
  const second = pickRecommendations(mockRecommendations);
  expect(first).toHaveLength(2);
  expect(new Set(first.map((item) => item.recommendationId)).size).toBe(2);
  expect(first).not.toEqual(second);
  expect(mockRecommendations).toEqual(original);
  expect(pickRecommendations([])).toEqual([]);
  expect(pickRecommendations([original[0]!, original[0]!])).toEqual([original[0]]);
});

it('잘못된 이동 상태와 외부 복귀 주소는 사용하지 않는다', () => {
  expect(readRecommendationNavigation(context)).toEqual(context);
  for (const value of [
    null,
    {},
    { ...context, from: '//evil.example' },
    { ...context, recommendation: { recommendationId: 1 } },
  ]) {
    expect(readRecommendationNavigation(value)).toBeUndefined();
  }
});

it('로그인 직전 초안을 저장하고 복원 후 삭제할 수 있다', () => {
  expect(saveLoginQuestionDraft(context)).toBe(true);
  expect(readLoginQuestionDraft()).toEqual(context);
  clearLoginQuestionDraft();
  expect(readLoginQuestionDraft()).toBeUndefined();
});

it('오래된 로그인 초안은 자동 복원하지 않는다', () => {
  const now = Date.now();
  jest.spyOn(Date, 'now').mockReturnValue(now);
  saveLoginQuestionDraft(context);
  jest.mocked(Date.now).mockReturnValue(now + 61 * 60 * 1000);
  expect(readLoginQuestionDraft()).toBeUndefined();
});

it('브라우저 저장소가 차단되면 초안 저장 실패를 반환한다', () => {
  jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
    throw new Error('blocked');
  });
  expect(saveLoginQuestionDraft(context)).toBe(false);
});
