import snapshot from '@/pages/about/prototype-snapshot.json';

// 공개 프로토타입 2026-10-29 스냅샷. 갱신 시 원본과 날짜를 함께 확인합니다.
export const topicSamples = snapshot.topicSamples;

export const matches: Record<'concurrency' | 'performance', [string, string]> = {
  concurrency: ['중복 주문을 막은\n잠금 전략', '트랜잭션과 잠금을 선택한\n이유를 배울 수 있어요.'],
  performance: ['반복 조회를 줄인\n쿼리 개선', 'N+1 문제를 찾고\n조회 범위를 줄인 과정을 배워요.'],
};
