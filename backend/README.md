# Dropit Backend

Dropit의 Spring REST API 서버입니다. 현재 1차 운영 구조는 다음과 같습니다.

```text
React/Vite 프론트엔드
        │ Authorization: Bearer <Supabase access token>
        ▼
Spring Boot API (AWS EC2 예정)
        │ JDBC + JPA
        ▼
Supabase PostgreSQL (AWS DB로 이전하기 전까지 유지)
```

## 기술 기준

| 항목 | 선택 |
|---|---|
| Java | 21 LTS |
| 프로젝트 하한선 | Java 21 |
| Spring Boot | 4.1.0 |
| 빌드 | Gradle Wrapper 9.6.0 |
| 웹 | Spring MVC REST |
| 데이터 접근 | Spring Data JPA, 복잡한 쿼리는 native SQL/QueryDSL 후보 |
| 인증 | Supabase Auth GitHub OAuth + Supabase JWT 검증 |
| 테스트 | JUnit, Spring 통합 테스트, Testcontainers PostgreSQL |
| API 문서 | OpenAPI 3 + Swagger UI (`springdoc-openapi`) |
| 가상 스레드 | 기본 비활성화, 부하 측정 후 선택 |

## 실행

Java 21과 Docker Desktop을 준비한 뒤 실행합니다.

```bash
cd backend
./gradlew test
./gradlew bootRun
```

Docker가 실행 중이면 Testcontainers PostgreSQL 통합 테스트가 실행됩니다. Docker가 없는 환경에서는 해당 통합 테스트가 자동으로 스킵되며, 테스트 코드 자체를 제거한 것은 아닙니다.

## 환경 변수

```env
SUPABASE_DB_URL=jdbc:postgresql://<host>:5432/postgres
SUPABASE_DB_USERNAME=<backend-db-user>
SUPABASE_DB_PASSWORD=<backend-db-password>

SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_JWT_ISSUER=https://<project-ref>.supabase.co/auth/v1
SUPABASE_JWT_JWK_SET_URI=https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json
# 레거시 HS256 프로젝트라면 JWK 대신 SUPABASE_JWT_SECRET을 사용합니다.

CORS_ALLOWED_ORIGINS=http://localhost:5173
PORT=8080
VIRTUAL_THREADS_ENABLED=false
```

실제 Supabase DB의 데이터가 있으므로 애플리케이션 시작 시 JPA가 테이블을 생성하거나 수정하지 않습니다(`ddl-auto=none`, Flyway 비활성화). DB 마이그레이션은 AWS 이전 계획과 함께 별도 결정합니다.

## API 문서

서버 실행 후 아래 주소에서 확인합니다.

- OpenAPI JSON: `http://localhost:8080/v1/openapi.json`
- Swagger UI: `http://localhost:8080/v1/swagger-ui.html`

OpenAPI는 프론트엔드가 읽을 수 있는 기계용 명세이고, Swagger UI는 그 명세를 사람이 확인하고 호출하는 화면입니다. 둘 중 하나를 고르는 관계가 아니므로 1차 구현에서는 두 가지를 함께 제공합니다.

## 인증 경계

GitHub OAuth 로그인·토큰 갱신·로그아웃은 기존 Supabase Auth가 담당합니다. 프론트엔드는 Supabase에서 발급받은 access token을 Spring API 호출 시 Bearer 토큰으로 전달하고, Spring은 토큰 서명과 `sub` 사용자 ID를 검증합니다.

따라서 1차 서버에는 GitHub OAuth callback을 중복 구현하지 않았습니다. 서버에는 토큰으로 현재 사용자를 확인하는 `GET /v1/auth/session`만 제공합니다.

Spring의 JDBC 연결은 Supabase JWT의 `auth.uid()` 세션 컨텍스트를 자동으로 전달하지 않습니다. 그래서 API의 소유권·크루 권한 검사는 Spring 서비스 계층에서 수행하고, 백엔드 DB 계정은 브라우저에 노출하지 않는 별도 계정으로 운영해야 합니다.

## 아직 구현하지 않은 확장 지점

- AWS PostgreSQL로 실제 데이터 이전
- S3 presigned URL 기반 썸네일 업로드
- 실행 수 API의 rate limit 및 어뷰징 방지
- GitHub OAuth를 Spring이 직접 중계하는 방식
- 부하 테스트 결과에 따른 가상 스레드 활성화
