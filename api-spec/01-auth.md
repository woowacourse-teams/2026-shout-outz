# 01. 인증 (Auth)

현재 프런트엔드는 `supabase.auth.signInWithOAuth({ provider: 'github' })`로 GitHub OAuth를 시작하고,
`supabase.auth.getSession()` / `onAuthStateChange()`로 세션을 추적하며, `signOut()`으로 로그아웃한다
(`src/App.tsx`, `src/utils/auth.ts`). 백엔드 API로 감싸면 아래처럼 된다.

---

### `GET /auth/github/authorize`

GitHub OAuth 로그인을 시작한다. 브라우저를 GitHub 인가 화면으로 리다이렉트.

| 항목 | 내용 |
|---|---|
| 인증 | 불필요 |
| Query | `redirectTo` (string, 로그인 완료 후 돌아올 프런트엔드 URL) |
| 응답 | `302 Found` → GitHub OAuth authorize URL로 리다이렉트 |

---

### `GET /auth/github/callback`

GitHub이 인가 코드와 함께 리다이렉트해오는 콜백. 코드를 토큰으로 교환하고 세션을 발급한다.

| 항목 | 내용 |
|---|---|
| 인증 | 불필요 |
| Query | `code` (string), `state` (string) |
| 응답 | `302 Found` → `redirectTo`로 리다이렉트, `access_token`/`refresh_token`을 쿼리 또는 `Set-Cookie`로 전달 |
| 에러 | `401 UNAUTHORIZED` (코드 검증 실패) |

---

### `POST /auth/refresh`  🆕 제안

만료된 access token을 refresh token으로 갱신. Supabase SDK가 내부적으로 자동 처리하던 부분을
직접 API로 노출할 때 필요.

| 항목 | 내용 |
|---|---|
| 인증 | 불필요 (refresh token을 본문으로 전달) |
| Body | `{ "refreshToken": "string" }` |
| 응답 (200) | `{ "accessToken": "string", "refreshToken": "string", "expiresAt": "string" }` |
| 에러 | `401 UNAUTHORIZED` |

---

### `POST /auth/logout`

| 항목 | 내용 |
|---|---|
| 인증 | 필요 |
| 응답 | `204 No Content` |

---

### `GET /auth/session`

현재 로그인한 사용자 정보. `toAuthUser()`가 GitHub OAuth 메타데이터(`full_name`/`user_name`/`avatar_url`)에서
파생하던 값과 동일하다.

| 항목 | 내용 |
|---|---|
| 인증 | 필요 |
| 응답 (200) | `{ "id": "uuid", "name": "string", "email": "string \| null", "avatarUrl": "string \| null" }` |
| 에러 | `401 UNAUTHORIZED` (토큰 없음/만료) |

> 참고: 이건 GitHub 계정 정보이며, Dropit 서비스 프로필(`Maker`)과는 별개다.
> 서비스 프로필은 [03-makers.md](./03-makers.md)의 `GET /makers/me`를 사용한다.
