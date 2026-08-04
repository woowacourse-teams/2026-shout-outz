# Dropit API 명세

현재 프런트엔드(`src/utils/supabaseData.ts`, `src/utils/auth.ts`)는 Supabase JS 클라이언트로
`apps`, `makers`, `app_bookmarks`, `app_likes`, `app_comments`, `crew_members` 테이블과
`toggle_app_like` 등의 RPC 함수를 브라우저에서 직접 호출하고 있다.

이 문서는 그 접근을 **백엔드 REST API 뒤로 감췄을 때** 나오게 될 엔드포인트 명세다.
실제 코드(스키마·쿼리·RLS 정책)를 기준으로 도출했고, 현재 앱에는 없지만 API로 전환하면
자연스럽게 필요해지는 항목은 "🆕 제안"으로 표시했다.

## 1차 Spring API 구현 기준

- GitHub OAuth 로그인·토큰 갱신·로그아웃은 1차에서도 Supabase Auth가 담당한다.
- 프론트엔드는 Supabase access token을 `Authorization: Bearer`로 Spring API에 전달한다.
- Spring은 JWT 서명과 `sub` 사용자 ID를 검증하고, 소유자·크루 권한을 서비스 계층에서 확인한다.
- Spring API는 `/v1` prefix를 사용한다.
- DB 마이그레이션과 S3 파일 업로드는 실제 데이터 안전성 때문에 후속 작업으로 분리한다.
- 실행 중 생성되는 기계용 문서는 `GET /v1/openapi.json`, 브라우저 문서는 `/v1/swagger-ui.html`에서 제공한다.

## 목차

| 파일 | 내용 |
|---|---|
| [01-auth.md](./01-auth.md) | GitHub OAuth 로그인, 세션 |
| [02-apps.md](./02-apps.md) | 앱 목록/상세/등록/수정/삭제/복구, 실행수 |
| [03-makers.md](./03-makers.md) | 메이커(크루) 프로필 |
| [04-bookmarks-likes.md](./04-bookmarks-likes.md) | 북마크, 좋아요 |
| [05-comments.md](./05-comments.md) | 댓글/답글 |
| [06-crew.md](./06-crew.md) | 크루 인증코드, 크루 멤버십 |
| [07-misc.md](./07-misc.md) | 사이트 방문 통계 |

## 공통 규칙

- **Base URL**: `https://api.dropit.app/v1` (예시)
- **인증**: `Authorization: Bearer <access_token>` (GitHub OAuth로 발급된 세션 JWT)
  - 토큰이 없으면 비로그인(anon) 사용자로 취급. 로그인이 필요한 엔드포인트는 각 문서에 표시.
- **Content-Type**: `application/json` (요청/응답 모두)
- **날짜/시간**: ISO 8601 UTC 문자열 (예: `2026-08-04T09:00:00.000Z`)
- **삭제(soft delete)**: `apps`는 실제로 행을 지우지 않고 `deletedAt`을 채운다. DB에는 삭제 정책 자체가 없음(`revoke delete`).

### 공통 에러 포맷

```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "해당 리소스를 찾을 수 없습니다."
  }
}
```

| HTTP | code | 의미 |
|---|---|---|
| 400 | `VALIDATION_ERROR` | 요청 본문/쿼리가 스키마 제약(길이·enum 등)을 위반 |
| 401 | `UNAUTHORIZED` | 로그인이 필요한데 토큰이 없거나 만료됨 |
| 403 | `FORBIDDEN` | 로그인은 했지만 권한이 없음 (소유자 아님, 크루 아님 등) |
| 404 | `NOT_FOUND` | 리소스가 없거나(또는 삭제되어) 조회 권한이 없음 |
| 409 | `CONFLICT` | 이미 존재하는 관계(예: 중복 북마크) |
| 500 | `INTERNAL_ERROR` | 서버 오류 |

### 공통 데이터 모델

**Maker**
```ts
{
  id: string          // = auth 사용자 id
  name: string         // 1~20자
  initials: string
  avatarUrl: string | null
  role: string         // 1~40자
  bio: string           // 1~100자
  tone: string          // hex color, 기본 "#d9e6ff"
}
```

**AppItem**
```ts
{
  id: string
  name: string              // 1~40자
  tagline: string           // 1~80자
  description: string       // 0~5000자
  categories: Category[]    // 1~2개, 중복 없이 CATEGORY_ENUM 안에서만
  thumbnailVariant: ThumbnailVariant
  thumbnailUrl?: string
  appUrl: string
  githubUrl?: string
  maker: Maker               // ⚠️ 아래 "메이커 스냅샷" 참고
  techTags: string[]
  plays: number
  likes: number
  createdAt: string
  ownerId: string
  deletedAt: string | null
  source: "seed" | "submitted"
}
```

> **메이커 스냅샷 주의**: `apps.maker`는 앱을 등록/수정한 시점의 `Maker` 객체를 그대로 JSON으로
> 박아둔 스냅샷이다(`makers` 테이블과 실시간으로 조인되지 않음). 즉 사용자가 나중에 프로필(이름,
> 아바타 등)을 바꿔도 이미 등록된 앱 카드에는 옛날 정보가 남는다. 댓글의 `author`만 조회 시점에
> `makers` 테이블을 조인해서 최신 값을 채운다. API로 옮길 때 이 비일관성을 유지할지, `apps` 응답도
> `makers`를 조인해서 항상 최신값을 주도록 바꿀지는 결정이 필요하다 — 이 문서는 **현재 동작(스냅샷
> 유지)** 을 기준으로 작성했다.

**CATEGORY_ENUM**
```
게임, 생산성, 학습, 여행, AI, 하네스, 자기개발, 개발, 디자인, 생활, 건강, 생성기, 소셜, 실험
```

**ThumbnailVariant**
```
retro, food, code, roulette, css, temperature, garden, dungeon, naming, http, timer, museum, new
```

### Supabase → API 매핑

| 기존 Supabase 직접 호출 | 대체 API |
|---|---|
| `apps` select, `order(created_at desc)` | `GET /apps` |
| `apps` select 단건 | `GET /apps/:appId` |
| `apps` insert | `POST /apps` |
| `apps` update (일반 필드) | `PATCH /apps/:appId` |
| `apps` update `deleted_at = now()` | `DELETE /apps/:appId` |
| `apps` update `deleted_at = null` | `POST /apps/:appId/restore` |
| `apps` (soft-deleted, `owner_id = me`) 필터링 | `GET /apps/trash` |
| `makers` select by id | `GET /makers/:makerId` |
| `makers` select by id(본인) | `GET /makers/me` |
| `makers` upsert | `PUT /makers/me` |
| `app_bookmarks` select by user | `GET /users/me/bookmarks` |
| `app_bookmarks` insert | `PUT /apps/:appId/bookmark` |
| `app_bookmarks` delete | `DELETE /apps/:appId/bookmark` |
| `app_likes` select by user | `GET /users/me/likes` |
| rpc `toggle_app_like` | `POST /apps/:appId/like` |
| `app_comments` select by app_id | `GET /apps/:appId/comments` |
| `app_comments` insert | `POST /apps/:appId/comments` |
| `app_comments` delete | `DELETE /comments/:commentId` |
| rpc `increment_app_plays` | `POST /apps/:appId/play` |
| rpc `record_site_visit` | `POST /site-visits` |
| rpc `verify_crew_access_code` | `POST /crew/verify` |
| `crew_members` select own | `GET /users/me/crew-status` |
| `supabase.auth.signInWithOAuth('github')` | `GET /auth/github/authorize` |
| `supabase.auth.getSession` / `onAuthStateChange` | `GET /auth/session` |
| `supabase.auth.signOut` | `POST /auth/logout` |
