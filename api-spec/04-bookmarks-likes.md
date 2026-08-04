# 04. 북마크 & 좋아요 (Bookmarks & Likes)

`app_bookmarks`, `app_likes` 테이블 + `toggle_app_like` RPC 기준.

## 북마크

북마크는 RPC 없이 단순 insert/delete라서, 토글이 아니라 REST-답게 `PUT`/`DELETE`로 설계.

### `GET /users/me/bookmarks`

| 항목 | 내용 |
|---|---|
| 인증 | 필요 (`app_bookmarks` select는 본인 것만 RLS로 허용) |
| 응답 (200) | `{ "appIds": string[] }` |

### `PUT /apps/:appId/bookmark`

북마크 추가 (idempotent).

| 항목 | 내용 |
|---|---|
| 인증 | 필요 |
| 응답 | `204 No Content` |
| 에러 | `401`, `404` (앱 없음) |

### `DELETE /apps/:appId/bookmark`

북마크 해제 (idempotent — 없어도 204).

| 항목 | 내용 |
|---|---|
| 인증 | 필요 |
| 응답 | `204 No Content` |

---

## 좋아요

좋아요는 `toggle_app_like(p_app_id)` RPC로 "좋아요 여부 확인 → insert/delete → apps.likes 증감"을
하나의 트랜잭션으로 원자적으로 처리한다(동시 클릭 시 카운트 꼬임 방지). API도 토글 하나로 유지.

### `GET /users/me/likes`

| 항목 | 내용 |
|---|---|
| 인증 | 필요 |
| 응답 (200) | `{ "appIds": string[] }` |

### `POST /apps/:appId/like`

좋아요 토글 (누르면 좋아요, 다시 누르면 취소).

| 항목 | 내용 |
|---|---|
| 인증 | 필요 |
| 응답 (200) | `{ "liked": boolean, "likes": number }` |
| 에러 | `401`, `404` |
| 비고 | 서버에서 원자적으로 처리(동시성 문제 방지). 별도 `DELETE` 엔드포인트를 두지 않는 이유는 원본 RPC와 동일한 토글 시맨틱을 유지하기 위함. |
