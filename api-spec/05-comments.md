# 05. 댓글 (Comments)

`app_comments` 테이블 + `validate_app_comment_parent()` 트리거 기준.

## AppComment 모델

```ts
{
  id: string
  appId: string
  userId: string
  parentId: string | null   // null이면 최상위 댓글
  title: string              // 최상위 댓글만 사용 가능(≤60자), 답글은 항상 ""
  content: string            // 1~500자
  createdAt: string
  author: Maker               // 작성 시점이 아니라 "조회 시점" 기준 최신 프로필로 조인
}
```

> `title`은 DB 스키마(`app_comments.title`)에는 있지만 현재 프런트엔드 코드(`supabaseData.ts`의
> `APP_COLUMNS`)는 이 컬럼을 select/insert하지 않아 사실상 미사용 상태다. API 명세에는 향후
> "댓글 제목" 기능을 열 수 있도록 필드를 살려뒀다 — 프런트에서 안 쓰면 그냥 빈 문자열로 두면 된다.

---

### `GET /apps/:appId/comments`

앱 하나의 댓글+답글 전체 목록.

| 항목 | 내용 |
|---|---|
| 인증 | 불필요 (삭제되지 않은 앱이면 누구나 조회 가능) |
| 응답 (200) | `{ "items": AppComment[] }` — `createdAt desc` 정렬, 최상위/답글이 평탄화된 배열로 옴(프런트에서 `parentId`로 그룹핑) |
| 에러 | `404 NOT_FOUND` (앱이 없거나 삭제됨) |

---

### `POST /apps/:appId/comments`

댓글 또는 답글 작성.

| 항목 | 내용 |
|---|---|
| 인증 | 필요 |
| Body | `{ "content": string, "parentId"?: string, "title"?: string }` |
| 검증 | `content` 1~500자 · 답글(=`parentId` 있음)일 때 `title`은 반드시 빈 문자열(있으면 `400`) · `parentId`가 가리키는 댓글은 반드시 같은 `appId`의 **최상위 댓글**이어야 함(답글에 또 답글 금지 — 트리거 `validate_app_comment_parent`가 강제하는 "1단계 depth 제한") |
| 응답 (201) | 생성된 `AppComment` (`author`는 요청자 본인의 현재 프로필) |
| 에러 | `401`, `404 NOT_FOUND` (앱 없음/삭제됨, 또는 `parentId` 댓글 없음), `400 VALIDATION_ERROR` (제목 규칙 위반, 답글에 답글 시도 등) |

---

### `DELETE /comments/:commentId`

| 항목 | 내용 |
|---|---|
| 인증 | 필요, 작성자 본인만(`user_id === me`) |
| 응답 | `204 No Content` |
| 에러 | `401`, `403 FORBIDDEN`, `404` |
| 비고 | 최상위 댓글을 삭제하면 `app_comments.parent_id`의 `on delete cascade`로 그 아래 답글도 함께 삭제됨 |
