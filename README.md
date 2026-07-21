# Dropit

우아한테크코스 크루들이 직접 만든 작은 웹앱을 발견하고, 바로 실행하고, 서로 공유하는 서비스입니다.

## 주요 기능

- 서비스 목록 조회, 카테고리 필터, 검색, 인기순·최신순 정렬
- 추천 서비스와 최근 등록 서비스 확인
- 게임, 생산성, 학습, 여행, AI, 하네스, 자기개발, 개발, 디자인, 생활, 건강, 생성기, 소셜, 실험 카테고리별 탐색
- 서비스 상세 정보, Markdown 형식의 소개 글, 기술 태그 확인
- 외부 서비스 실행과 GitHub 저장소 바로가기
- 로그인 사용자의 좋아요와 저장한 서비스 관리
- GitHub OAuth 로그인과 GitHub 프로필 이미지 연동
- 로그인한 크루의 서비스 등록·수정
- 크루 인증 코드로 서비스 등록 권한 확인
- 등록 중인 서비스 입력 내용 브라우저 임시 저장
- 서비스 썸네일 URL 입력 또는 이미지 파일 업로드
- 제작자 프로필 등록·수정 및 제작자별 서비스 목록
- 서비스 소프트 삭제와 내 프로필에서 복구
- `/submit`, `/makers/me` 같은 SPA 경로 새로고침 지원

## 기술 스택

- React 19
- TypeScript
- Vite
- React Router
- Supabase Auth, PostgreSQL, RLS, RPC
- Vercel

## 로컬 실행

### 1. 의존성 설치

```bash
npm install
```

### 2. 환경 변수 설정

`.env.example`을 복사해 `.env.local`을 만들고 Supabase 브라우저 공개 설정을 입력합니다.

```bash
cp .env.example .env.local
```

```env
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key
```

`.env.local`은 Git에 올리지 않습니다. GitHub Client Secret과 Supabase `service_role` 키는 프론트엔드 환경 변수에 넣으면 안 됩니다.

### 3. Supabase 스키마 적용

Supabase 대시보드의 `SQL Editor`에서 최신 [`supabase/schema.sql`](supabase/schema.sql)을 처음부터 실행합니다.

스키마에는 다음 테이블과 기능이 포함되어 있습니다.

- `makers`: 제작자 프로필
- `apps`: 서비스 정보와 소프트 삭제 상태
- `app_bookmarks`: 사용자별 저장 목록
- `app_likes`: 사용자별 좋아요 상태
- `crew_access_codes`: 해시된 크루 인증 코드
- `crew_members`: 인증을 완료한 크루 목록
- 좋아요, 실행 수, 인증 코드 확인을 위한 RPC 함수
- 로그인 사용자와 서비스 소유자만 접근할 수 있는 RLS 정책

현재 스키마는 기존 테이블을 재사용하고 필요한 컬럼·정책·제약 조건을 갱신하는 방식입니다. 최신 스키마를 다시 실행해도 서비스 데이터를 지우는 `drop table` 작업은 하지 않습니다.

### 4. GitHub 로그인 설정

1. Supabase 대시보드에서 `Authentication > Providers > GitHub`을 활성화합니다.
2. GitHub OAuth App의 callback URL에 Supabase가 안내하는 주소를 등록합니다.
   ```text
   https://<project-ref>.supabase.co/auth/v1/callback
   ```
3. Supabase의 `Authentication > URL Configuration`에서 로컬·배포 주소를 Redirect URLs에 등록합니다.
4. GitHub Client ID와 Client Secret은 Supabase 대시보드에서만 관리합니다.

로그인에는 GitHub 저장소 접근 권한을 요청하지 않습니다. 실제 서비스 배포 시에는 HTTPS를 사용하고, 사용하지 않는 Redirect URL은 등록하지 않는 것을 권장합니다.

### 5. 크루 인증 코드 등록

서비스 등록은 GitHub 로그인과 크루 인증을 모두 완료해야 가능합니다. 인증 코드는 평문이 아니라 bcrypt 해시로 Supabase에 저장합니다.

```sql
insert into public.crew_access_codes (code_hash)
values (crypt('사용할-인증코드', gen_salt('bf')));
```

실제 인증 코드는 README, 소스 코드, `.env.local`, 커밋에 기록하지 않습니다. 인증 코드가 노출되면 Supabase에서 해당 코드를 `active = false`로 비활성화하고 새 코드를 발급하세요.

### 6. 개발 서버 실행

```bash
npm run dev
```

기본 개발 주소는 `http://127.0.0.1:5173`입니다. 환경 변수나 Supabase 설정을 바꾼 뒤에는 개발 서버를 다시 시작해야 합니다.

## 사용 흐름

1. 홈에서 서비스를 둘러봅니다.
2. 서비스 카드를 눌러 상세 설명과 제작자 정보를 확인합니다.
3. 로그인하면 좋아요와 저장 기능을 사용할 수 있습니다.
4. 프로필을 설정하면 서비스 카드와 상세 페이지에 제작자 정보가 표시됩니다.
5. `서비스 공유하기`에서 서비스 정보를 작성하고 크루 인증 코드를 확인합니다.
6. 등록한 서비스는 상세 페이지에서 수정하거나 소프트 삭제할 수 있습니다.
7. 삭제한 서비스는 내 프로필의 `복구할 수 있는 앱` 목록에서 복구할 수 있습니다.

## 서비스 설명 Markdown

상세 설명에는 다음 Markdown 문법을 사용할 수 있습니다.

```md
## 서비스 소개

해결하려는 문제와 사용 방법을 작성해주세요.

- 주요 기능
- 사용 대상

**강조할 내용**과 `코드`를 표시할 수 있습니다.
```

지원하는 문법은 제목, 문단, 순서·비순서 목록, 인용문, 구분선, 인라인 코드, 코드 블록, 굵은 글씨, 기울임 글씨, 외부 링크입니다. HTML 태그와 저장소 내부의 상대 경로 이미지는 지원하지 않으므로, 썸네일은 별도의 URL이나 이미지 파일 업로드 기능을 사용합니다.

상세 설명은 최대 5,000자까지 입력할 수 있습니다.

## 데이터 저장 방식

서비스, 프로필, 좋아요, 저장 목록, 인증 상태는 Supabase에 저장합니다. 브라우저 `localStorage`에는 다음 항목만 저장합니다.

- 작성 중인 서비스 등록 폼 임시 저장 내용
- 프로필 안내 팝업을 오늘 하루 동안 숨겼는지 여부
- Markdown 안내 팝업을 오늘 하루 동안 숨겼는지 여부

서비스 등록을 완료하면 해당 임시 저장 내용은 삭제됩니다. Supabase 환경 변수가 없을 때 앱 데이터를 별도로 로컬 저장소에 대체 저장하지 않습니다.

## 이미지 업로드

- PNG, JPG, WEBP, GIF 지원
- 파일 크기 최대 2MB
- 권장 크기: `1200 × 750px`, `16:10`
- 이미지 URL 입력도 지원

현재 업로드한 이미지는 별도 Storage 버킷이 아닌 서비스 데이터의 이미지 값으로 저장합니다. 서비스가 커지면 Supabase Storage로 분리하는 것을 고려할 수 있습니다.

## 주요 경로

| 경로 | 설명 |
| --- | --- |
| `/` | 서비스 목록과 추천 서비스 |
| `/login` | GitHub 로그인 |
| `/submit` | 새 서비스 등록 |
| `/apps/:appId` | 서비스 상세 |
| `/apps/:appId/edit` | 내가 등록한 서비스 수정 |
| `/bookmarks` | 로그인 사용자의 저장 목록 |
| `/makers/me` | 내 프로필과 내가 등록한 서비스 |
| `/makers/:makerId` | 제작자 프로필과 서비스 목록 |

## 배포

Vercel에 저장소를 연결하고 다음 환경 변수를 등록합니다.

- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_ANON_KEY`

루트의 [`vercel.json`](vercel.json)은 Vite SPA 경로를 `/`로 rewrite해 상세 페이지나 등록 페이지를 새로고침해도 404가 나지 않도록 합니다.

배포 후에는 다음 주소를 Supabase와 GitHub OAuth 설정에 추가합니다.

- Vercel 배포 주소
- 실제 서비스 도메인

## 명령어

```bash
npm run dev      # 개발 서버
npm run build    # 타입 검사 및 프로덕션 빌드
npm run preview  # 빌드 결과 확인
```

## 프로젝트 구조

```text
src/
├── components/       # 공통 UI와 Markdown 렌더러
├── pages/            # 라우트별 페이지
├── utils/            # Supabase, 인증, 임시 저장, 포맷 유틸리티
├── App.tsx           # 인증·데이터 상태와 라우팅
├── styles.css        # 전체 디자인 시스템과 반응형 스타일
└── types.ts          # 서비스·프로필 타입
supabase/
└── schema.sql        # 테이블, RLS 정책, RPC 함수
public/
└── dropit-icon.svg   # 서비스 아이콘
```
