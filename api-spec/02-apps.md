# 02. 앱 (Apps)

`apps` 테이블 + RLS 정책(`schema.sql:187-211`) + `increment_app_plays` RPC 기준.
RLS는 `deleted_at is null or auth.uid() = owner_id`로 조회를 제한하므로, 삭제된 앱은
소유자만 볼 수 있다 — API에서도 동일하게 강제한다.

---

### `GET /apps`

공개 앱 목록. 현재 프런트엔드는 전체를 한 번에 받아 클라이언트에서 카테고리/검색어/정렬을
처리한다(`HomePage.tsx`). API로 옮기면서 서버 사이드 필터링/페이지네이션을 지원하도록 제안.

| 항목 | 내용 |
|---|---|
| 인증 | 불필요 (있으면 `mine=true` 등 확장 가능) |
| Query | `category` (CATEGORY_ENUM, 미지정 시 전체) |
| | `q` (string, name/tagline/techTags 부분일치 검색) |
| | `sort` (`popular` \| `latest`, 기본 `popular`) — popular는 `likes desc, createdAt desc` |
| | `makerId` (string) 🆕 — 특정 메이커가 등록한 앱만 (현재는 프런트에서 `apps` 전체를 받아 클라이언트에서 `maker.id`로 필터링, `MakerPage.tsx`) |
| | `limit` (number, 기본 40, 최대 100) 🆕 |
| | `offset` (number, 기본 0) 🆕 |
| 응답 (200) | `{ "items": AppItem[], "total": number }` |
| 비고 | `deletedAt`이 있는 앱은 절대 포함되지 않음 |

---

### `GET /apps/trash`

로그인한 사용자가 삭제한(복구 가능한) 앱 목록. `MakerPage.tsx`의 "삭제한 앱" 섹션.

| 항목 | 내용 |
|---|---|
| 인증 | 필요 |
| 응답 (200) | `{ "items": AppItem[] }` — `ownerId === me` && `deletedAt != null`인 것만 |

---

### `GET /apps/:appId`

| 항목 | 내용 |
|---|---|
| 인증 | 불필요 (삭제된 앱은 소유자만 조회 가능) |
| 응답 (200) | `AppItem` |
| 에러 | `404 NOT_FOUND` (없거나, 삭제됐는데 소유자가 아님) |

---

### `POST /apps`

앱 등록. RLS 상 **크루 멤버(`crew_members`에 존재)만** 생성 가능 (`schema.sql:192-202`).

| 항목 | 내용 |
|---|---|
| 인증 | 필요 + 크루 인증 완료 상태 |
| Body | `{ name, tagline, description, appUrl, githubUrl?, categories, techTags, thumbnailVariant, thumbnailUrl? }` |
| 검증 | `name` 1~40자 · `tagline` 1~80자 · `description` ≤5000자 · `categories` 1~2개(CATEGORY_ENUM) · `thumbnailVariant`는 ENUM 값 |
| 응답 (201) | 생성된 `AppItem` (`id`는 서버 생성, `maker`는 요청자의 현재 `Maker` 프로필 스냅샷, `ownerId` = 로그인 사용자, `plays`/`likes` = 0, `source` = `"submitted"`) |
| 에러 | `401`, `403 FORBIDDEN` (크루 미인증), `400 VALIDATION_ERROR` |

---

### `PATCH /apps/:appId`

앱 수정. 소유자만 가능(`schema.sql:204-208`).

| 항목 | 내용 |
|---|---|
| 인증 | 필요, `ownerId === me` |
| Body | `POST /apps`와 동일한 필드 중 변경할 것만 (partial) |
| 응답 (200) | 수정된 `AppItem` |
| 에러 | `401`, `403 FORBIDDEN` (소유자 아님), `404`, `400 VALIDATION_ERROR` |

---

### `DELETE /apps/:appId`

소프트 삭제. `deletedAt`을 현재 시각으로 설정한다. 실제 row 삭제는 없음
(`revoke delete on public.apps from authenticated`).

| 항목 | 내용 |
|---|---|
| 인증 | 필요, `ownerId === me`, 이미 삭제되지 않은 앱만 |
| 응답 | `204 No Content` |
| 에러 | `401`, `403 FORBIDDEN`, `404` (이미 삭제됨 포함) |

---

### `POST /apps/:appId/restore`

삭제한 앱을 되돌림. `MakerPage.tsx`의 복구 버튼.

| 항목 | 내용 |
|---|---|
| 인증 | 필요, `ownerId === me`, 삭제된 상태인 앱만 |
| 응답 (200) | 복구된 `AppItem` |
| 에러 | `401`, `403 FORBIDDEN`, `404` (삭제 상태가 아님 포함) |

---

### `POST /apps/:appId/play`

앱 실행수 +1. `increment_app_plays(p_app_id)` RPC 대응. 누구나 호출 가능(익명 포함), 카운터만 증가.

| 항목 | 내용 |
|---|---|
| 인증 | 불필요 |
| 응답 (200) | `{ "plays": number }` |
| 에러 | `404 NOT_FOUND` |
| 비고 | rate limit 없음(원본 RPC도 동일) — 어뷰징 방지가 필요하면 IP/세션 기준 스로틀링 추가 검토 |
