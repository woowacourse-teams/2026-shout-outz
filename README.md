<p align="center">
  <img src="public/dropit-icon.svg" alt="Dropit logo" width="72" height="72" />
</p>

<h1 align="center">Dropit</h1>

<p align="center">
  우테코 크루들이 직접 만든 서비스를 발견하고 공유하는 아카이빙 서비스
</p>

<p align="center">
  <a href="https://drop-it-project.vercel.app/"><strong>서비스 바로가기</strong></a>
  ·
  <a href="https://github.com/sangjun121/dropit/issues">버그 제보</a>
  ·
  <a href="https://github.com/sangjun121/dropit/issues">기능 제안</a>
  ·
  <a href="https://github.com/sangjun121/dropit">GitHub</a>
</p>

<p align="center">
  <img alt="React" src="https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=20232a" />
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=fff" />
  <img alt="Supabase" src="https://img.shields.io/badge/Supabase-3ECF8E?style=flat-square&logo=supabase&logoColor=fff" />
  <img alt="Vercel" src="https://img.shields.io/badge/Vercel-000?style=flat-square&logo=vercel&logoColor=fff" />
</p>

## 서비스 소개

**Dropit은 우테코 크루들이 만든 작은 웹앱을 한곳에서 둘러보고, 바로 실행하고, 서로 공유할 수 있는 서비스입니다.**

- 서비스 카드를 눌러 상세 설명과 제작자 정보를 확인할 수 있습니다.
- GitHub 로그인 후 좋아요와 저장 기능을 사용할 수 있습니다.
- 크루 인증을 완료한 사용자는 직접 만든 서비스를 등록할 수 있습니다.
- 서비스 설명은 Markdown으로 작성하고 미리볼 수 있습니다.
- 제작자 프로필과 서비스 정보를 직접 수정할 수 있습니다.

## 주요 기능

### 서비스 탐색

- 추천 서비스와 최근 등록 서비스 확인
- 좋아요 기준 실시간 순위
- 인기순·최신순 정렬
- 카테고리 필터와 검색
- 서비스 실행, 공유, GitHub 바로가기

### 서비스 등록과 관리

- GitHub OAuth 로그인
- 크루 인증 코드 기반 등록 권한 확인
- 서비스 이름, 한 줄 소개, 상세 설명, 주소, 카테고리, 기술 태그 입력
- PNG, JPG, WEBP, GIF 썸네일 URL·파일 업로드
- 작성 중인 등록 폼 브라우저 임시 저장
- 등록한 서비스 수정
- 소프트 삭제와 내 프로필에서 서비스 복구

### 카테고리

게임, 생산성, 학습, 여행, AI, 하네스, 자기개발, 개발, 디자인, 생활, 건강, 생성기, 소셜, 실험

## 사용 흐름

```text
서비스 탐색 → 상세 페이지 확인 → 로그인 → 좋아요·저장
                                      ↓
                         크루 인증 → 서비스 공유 → 서비스 관리
```

## 기술 스택

- React 19
- TypeScript
- Vite
- React Router
- Supabase Auth, PostgreSQL, Row Level Security, RPC
- Vercel
