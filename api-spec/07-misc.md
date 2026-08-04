# 07. 기타 (Misc)

`site_visitors` 테이블 + `record_site_visit` RPC 기준.

---

### `POST /site-visits`

방문 기록. 하루에 같은 `visitorId`가 여러 번 호출해도 그날은 1회만 카운트
(`primary key (visitor_id, visited_on)`, `on conflict do nothing`). 날짜 기준은 `Asia/Seoul`.

| 항목 | 내용 |
|---|---|
| 인증 | 불필요 |
| Body | `{ "visitorId": string }` (1~128자, 프런트가 `localStorage`에 저장해둔 UUID) |
| 응답 (200) | `{ "dailyVisitors": number, "totalVisitors": number }` |
| 에러 | `400 VALIDATION_ERROR` (`visitorId` 누락/길이 위반) |
| 비고 | `site_visitors` 원본 테이블은 `anon`/`authenticated` 모두 직접 접근 불가 — 이 엔드포인트를 통해서만 기록/집계됨. 로그인 여부와 무관하게 동작. |
