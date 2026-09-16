// TODO 대체 GET /api/v1/home/statistics 응답의 data (명세는 모든 필드 nullable 미확정)
export interface HomeStatistics {
  projectCount: number;
  feedCount: number;
  currentCohort: number;
  ongoingEventCount: number;
}
