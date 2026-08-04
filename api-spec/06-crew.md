# 06. 크루 인증 (Crew)

`crew_access_codes`, `crew_members` 테이블 + `verify_crew_access_code` RPC 기준.
크루로 인증돼야만 앱을 등록할 수 있다(`02-apps.md`의 `POST /apps` 참고).

---

### `POST /crew/verify`

발급받은 크루 코드로 본인을 크루 멤버로 등록. 코드 자체는 `crypt()` 해시로 비교하고,
`crew_access_codes` 원본 테이블은 `anon`/`authenticated` 모두 직접 접근 불가
(`revoke all on public.crew_access_codes`) — 오직 이 RPC를 통해서만 검증 가능.

| 항목 | 내용 |
|---|---|
| 인증 | 필요 |
| Body | `{ "code": string }` |
| 응답 (200) | `{ "verified": boolean }` |
| 에러 | `401`, `400 VALIDATION_ERROR` (코드 형식 이상) |
| 비고 | 성공 시 서버가 `crew_members`에 `(user_id, verified_at=now())`를 upsert. 이미 크루면 재호출해도 안전(idempotent). |

---

### `GET /users/me/crew-status`

현재 사용자가 크루 인증을 마쳤는지 확인. `SubmitPage.tsx` 진입 시 크루 여부를 미리 확인하는 용도로 사용 가능.

| 항목 | 내용 |
|---|---|
| 인증 | 필요 |
| 응답 (200) | `{ "isCrewMember": boolean, "verifiedAt": string \| null }` |
| 에러 | `401` |
