import type { QuestionRecommendation } from '@/types/recommendation';

export const mockRecommendations: QuestionRecommendation[] = [
  {
    recommendationId: 9001,
    title: '캐시를 도입할 때 데이터 정합성은 어떻게 지키나요?',
    content:
      '조회 성능을 높이기 위해 캐시 도입을 고민하고 있어요.\n\nDB의 데이터가 변경됐을 때 **캐시를 언제, 어떻게 갱신하는지** 궁금합니다.\n\n- TTL만으로 충분한 경우는 언제인가요?\n- 캐시 무효화와 갱신 중 어떤 전략을 사용하시나요?\n\n실제 프로젝트에서 겪은 경험을 듣고 싶어요.',
  },
  {
    recommendationId: 9002,
    title: '비동기 작업이 실패했을 때 재시도는 어디서 처리하나요?',
    content:
      '메시지 큐로 비동기 작업을 처리하고 있습니다. 외부 API가 잠시 실패했을 때 재시도를 넣으려고 해요.\n\n**재시도 횟수와 간격**은 어떤 기준으로 정하시나요? 중복 처리 방지와 실패 메시지 관리 경험도 궁금합니다.',
  },
  {
    recommendationId: 9003,
    title: '서버 상태와 화면 상태를 나누는 기준이 궁금해요',
    content:
      'TanStack Query로 서버 데이터를 관리하면서 필터, 정렬, 작성 중인 내용을 어디에 둘지 고민이 됩니다.\n\nURL, 컴포넌트 상태, Query 캐시를 어떤 기준으로 구분하시나요?',
  },
  {
    recommendationId: 9004,
    title: '코드 리뷰에서 설계 의견이 다를 때 어떻게 조율하나요?',
    content:
      '동작에는 문제가 없지만 서로 선호하는 설계가 다를 때 리뷰가 길어지곤 합니다.\n\n팀에서 합의하는 기준이나 효과적이었던 대화 방법이 있다면 공유해 주세요.',
  },
  {
    recommendationId: 9005,
    title: '느린 API의 병목을 찾을 때 무엇부터 확인하나요?',
    content:
      '특정 목록 API의 응답 시간이 길어졌어요.\n\n쿼리, 외부 호출, 커넥션 풀 중 어떤 순서로 확인하는지 궁금합니다. 성능 개선 전후를 비교할 때 사용한 지표도 알고 싶어요.',
  },
];
