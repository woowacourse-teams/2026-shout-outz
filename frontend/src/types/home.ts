/**
 * `GET /api/v1/home/statistics` 응답의 data.
 *
 * 이 엔드포인트는 서버 문서에 없어 `schema.ts`에서 파생할 수 없다. 손으로 적은 몇 안 되는 타입이다.
 * 문서에 올라오면 `src/types/api.ts`의 파생 타입으로 바꾼다(docs/api-types.md).
 */
export interface HomeStatistics {
  projectCount: number;
  feedCount: number;
  currentCohort: number;
  ongoingEventCount: number;
}
