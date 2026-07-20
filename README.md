# Dropit

작은 웹앱을 발견하고 바로 실행하는 프론트엔드 프로토타입입니다.

## 실행

```bash
npm install
npm run dev
```

빌드 검증:

```bash
npm run build
```

## 구현 범위

- 홈 피드, 카테고리 필터, 검색, 인기순/최신순 정렬
- 앱 상세, 외부 앱 실행, 좋아요, 북마크
- 앱 등록 폼과 실시간 카드 미리보기
- GitHub 로그인 후 앱 등록 및 내 앱 수정
- 제작자 프로필과 제작자 앱 목록
- 내 프로필 생성·수정 및 앱 등록 제작자 정보 연결
- 잘못된 앱/제작자 주소, 검색 결과 없음, 빈 북마크, 등록 성공 상태
- Supabase 기반 GitHub 로그인과 앱·프로필·좋아요·북마크·실행 수 저장
- Supabase 연결이 없을 때 로컬 데이터로 대체하지 않고 연결 안내 표시
- 작성 중인 앱 등록 폼만 브라우저 `localStorage`에 임시 저장하며, 등록 완료 시 삭제
- `supabase/schema.sql`의 RLS 정책과 좋아요/실행 수 RPC

### GitHub 로그인 설정

1. Supabase 프로젝트에서 `Authentication > Providers > GitHub`을 켭니다.
2. `.env.example`을 `.env.local`로 복사하고 Supabase URL과 anon key를 입력합니다.
3. GitHub OAuth 앱의 callback URL에 Supabase가 안내하는 `/auth/v1/callback` 주소를 등록합니다.
4. Supabase의 Redirect URLs에는 사용하는 주소만 추가합니다. 로컬에서는 `http://127.0.0.1:4173/submit`, `http://127.0.0.1:4173/makers/me`와 앱 수정 경로를 등록합니다.

GitHub Client Secret은 GitHub와 Supabase 대시보드에만 입력합니다. `.env.local`에는 Supabase URL과 브라우저용 공개 키만 넣고, 저장소 권한(scope)은 요청하지 않습니다. GitHub 설정에서 연결된 OAuth 앱을 철회할 수 있다는 점도 사용자에게 안내하세요. 실제 배포에서는 HTTPS를 사용해야 합니다.

### Supabase 데이터 설정

1. Supabase SQL Editor에서 최신 [`supabase/schema.sql`](supabase/schema.sql)을 실행합니다. 기존 스키마를 다시 실행해도 `if not exists`와 정책 교체 구문으로 안전하게 반영됩니다.
2. `.env.example`을 `.env.local`로 복사하고 `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`를 입력합니다.
3. Authentication에서 GitHub Provider를 켜고 Client ID와 Client Secret은 Supabase 대시보드에만 입력합니다.
4. 앱의 Redirect URL과 GitHub OAuth callback URL을 각각 정확히 등록합니다.

### 크루 인증 코드 설정

앱 등록은 GitHub 로그인뿐 아니라 크루 인증까지 완료한 계정만 가능합니다. 인증 코드는 프론트 코드나 `.env.local`에 넣지 않고 Supabase에 해시로 저장합니다. SQL Editor에서 원하는 코드를 넣어 한 번 실행하세요.

```sql
insert into public.crew_access_codes (code_hash)
values (crypt('여기에_사용할_인증코드', gen_salt('bf')));
```

인증 코드를 확인하면 해당 계정이 `crew_members`에 등록되고, 이후 앱 등록 요청은 Supabase RLS에서도 한 번 더 검증됩니다.

환경변수가 없으면 로그인 버튼이 비활성화되고, 앱 데이터는 표시하거나 로컬에 저장하지 않습니다. `.env.local`에 Supabase URL과 브라우저용 공개 키를 입력한 뒤 개발 서버를 다시 시작해야 합니다. 작성 중인 앱 등록 폼은 예외적으로 브라우저에 임시 저장됩니다.

## 디자인

`apple-design-analysis.md`의 토큰을 바탕으로 검은 글로벌 내비게이션, 단일 Action Blue, 흰색·파치먼트·근흑색 풀블리드 타일, SF Pro 계열 시스템 폰트 스택을 사용했습니다. 앱 썸네일은 외부 이미지 요청 없이 CSS 그래픽으로 구성했습니다.
