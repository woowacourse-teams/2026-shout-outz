# 03. 메이커 프로필 (Makers)

`makers` 테이블 기준. `id`는 `auth.users(id)`를 그대로 참조하므로 별도 프로필 id가 없다.

---

### `GET /makers/:makerId`

메이커 공개 프로필. `MakerPage.tsx`가 남의 프로필을 볼 때 사용 (현재는 `apps` 목록에서
`maker` 스냅샷을 뽑아 쓰지만, API 전환 시 최신 프로필을 직접 조회하는 이 엔드포인트를 쓰는 게 맞다).

| 항목 | 내용 |
|---|---|
| 인증 | 불필요 (`makers`는 `select`가 `anon, authenticated` 모두에 공개) |
| 응답 (200) | `Maker` |
| 에러 | `404 NOT_FOUND` (프로필을 아직 만들지 않은 사용자) |

---

### `GET /makers/me`

내 프로필. 아직 프로필을 등록하지 않았다면 404 — `MakerPage.tsx`가 이 경우 프로필 등록 폼을 띄운다.

| 항목 | 내용 |
|---|---|
| 인증 | 필요 |
| 응답 (200) | `Maker` |
| 에러 | `401`, `404 NOT_FOUND` (미등록) |

---

### `PUT /makers/me`

프로필 생성/수정 (upsert). `upsertRemoteProfile()` 대응 — 신규/기존 여부를 프런트가 신경 쓰지 않도록 upsert로 설계.

| 항목 | 내용 |
|---|---|
| 인증 | 필요 (`auth.uid() = id`인 본인만) |
| Body | `{ "name": string, "role": string, "bio": string, "avatarUrl"?: string \| null, "tone"?: string }` |
| 검증 | `name` 1~20자 · `role` 1~40자 · `bio` 1~100자(최대 4줄, 프런트에서 정규화) |
| 응답 (200) | 저장된 `Maker` |
| 에러 | `401`, `400 VALIDATION_ERROR` |
| 비고 | `initials`는 서버가 `name`에서 자동 계산(프런트 `initialsFromName()`과 동일 규칙)하거나, 클라이언트가 계산해 보내는 값을 그대로 저장 — 현재는 후자(클라이언트 계산). `tone`은 신규 생성 시에만 랜덤/기본값을 배정하고 이후 고정. |
