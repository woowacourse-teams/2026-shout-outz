# Dropit 백엔드 기술 의사결정 기록

작성일: 2026-08-04

이 문서는 Dropit의 프론트엔드와 기존 Supabase 구조를 기준으로, 1차 Spring 백엔드의 기술을 선택한 이유를 기록한다. 버전 선택은 “가장 최신인가”보다 지원 여부, 현재 환경, 데이터 안전성, 팀이 면접에서 설명할 수 있는가를 기준으로 했다.

## 1. 프로젝트 전제

| 항목 | 현재 결정 | 의미 |
|---|---|---|
| 프론트엔드 | React + TypeScript + Vite | 기존 화면과 기능은 유지하고 데이터 호출 경로만 Spring API로 전환 |
| 백엔드 배포 | AWS EC2 예정 | 실행 가능한 JAR와 환경 변수 기반 설정을 사용 |
| 현재 DB | Supabase PostgreSQL | 실제 데이터가 있으므로 1차에서는 스키마와 데이터를 건드리지 않음 |
| DB 이전 | 추후 AWS로 이전 | 별도 마이그레이션 계획을 만들고 검증 후 진행 |
| 로그인 | Supabase Auth의 GitHub OAuth 유지 | Spring이 OAuth를 중복 구현하지 않고 access token을 검증 |
| 파일 저장 | 현재 URL/Base64 흐름, 추후 S3 | 1차 API는 `thumbnailUrl`을 받고 S3 업로드는 후속 작업 |
| API 범위 | 현재 `api-spec/`의 앱 기능 전체 | 앱, 메이커, 북마크, 좋아요, 댓글, 크루 인증, 방문 통계 포함 |

## 2. 최종 1차 기술 선택

| 분야 | 선택 | 선택 기준 | 검토한 대안과 제외 이유 |
|---|---|---|---|
| JDK | Java 21 LTS | 현재 개발 환경에 이미 설치되어 있고, Spring Boot 4.1을 실행할 수 있으며, 가상 스레드가 정식 제공되는 LTS 버전 | Java 17은 프레임워크 최소선일 뿐 프로젝트 기준으로는 낮게 잡지 않음 |
| 프로젝트 Java 하한선 | Java 21 | 팀원·CI·EC2가 모두 Java 21 이상을 사용하도록 고정해 환경 차이를 줄임 | Java 17은 가상 스레드 사용이 불가능하고 최신 동시성 선택지를 제한함 |
| Spring Boot | 4.1.0 | 2026-08-04 기준 정식 안정 버전이며 새 프로젝트에 사용할 수 있는 최신 라인 | Boot 3.5는 기존 자료 호환성은 좋지만 3.5.x 마지막 OSS 릴리스이므로 신규 기준으로 채택하지 않음 |
| 빌드 | Gradle Wrapper 9.6.0 | Spring Boot 4.1이 지원하는 Gradle 9.x 범위 안에서 프로젝트별 버전을 고정 | 전역 Gradle에 의존하면 팀원별 버전 차이와 로컬 오류가 생김 |
| HTTP 서버 | Spring MVC + Tomcat | 현재 기능은 동기식 CRUD와 JDBC/JPA 기반 DB 요청 중심이며 코드 흐름이 단순함 | WebFlux는 비동기 전환 비용이 있고 현재 병목 근거가 없음 |
| 데이터 접근 | Spring Data JPA | 앱·메이커·댓글 등 관계형 CRUD가 중심이고 팀이 도메인 모델과 트랜잭션을 학습하기 좋음 | 처음부터 MyBatis/직접 SQL로 가면 CRUD 중복이 늘어남 |
| 복잡한 조회 | native SQL 또는 QueryDSL을 필요할 때 추가 | 인기순, 배열 필터, JSONB, 원자적 카운터처럼 JPA만으로 불편한 부분만 보완 | 모든 조회를 처음부터 QueryDSL로 만들면 초기 복잡도가 커짐 |
| 인증 | Spring Security OAuth2 Resource Server | Supabase가 발급한 JWT의 서명과 사용자 ID를 Spring에서 검증 | Spring OAuth Client를 추가하면 Supabase GitHub OAuth와 역할이 중복됨 |
| 권한 | Spring 서비스 계층에서 소유자·크루 검증 | JDBC 연결에서는 Supabase RLS의 `auth.uid()`가 자동 전달되지 않으므로 API가 명시적으로 검사 | 브라우저에 Supabase service key를 노출하거나 RLS에만 의존하지 않음 |
| 스키마 변경 | 1차 Flyway 비활성화, `ddl-auto=none` | 실제 데이터가 있는 Supabase DB에 앱 시작 시 자동 변경이 발생하지 않게 함 | 지금 바로 전체 스키마를 Flyway로 재작성하면 데이터 손실·드리프트 위험이 있음 |
| 가상 스레드 | 1차 비활성화 | 현재 저트래픽 서비스에서 예상 병목은 스레드보다 DB 연결 풀과 외부 DB 지연임 | 최신 기능이라는 이유만으로 켜지 않고 부하 테스트 결과로 재판단 |
| 테스트 | API 통합 테스트 우선 + 핵심 서비스 단위 테스트 | 실제 PostgreSQL 배열·JSONB·트랜잭션·권한을 검증해야 함 | 모든 단순 getter/위임 메서드에 기계적으로 단위 테스트를 만들지 않음 |
| 테스트 DB | Testcontainers PostgreSQL | 운영 DB와 같은 계열의 SQL, 배열, JSONB를 검증할 수 있음 | H2는 PostgreSQL 전용 문법과 타입을 충분히 재현하지 못함 |
| API 문서 | OpenAPI 3 생성 + Swagger UI 제공 | OpenAPI는 프론트가 사용할 기계용 계약이고 Swagger UI는 사람이 테스트할 화면임 | “OpenAPI와 Swagger 중 하나”가 아니라 역할이 달라 함께 사용 |

## 3. Java 17~26 선택 근거

| Java | 판단 | 이유 |
|---|---|---|
| 17 | 프레임워크 최소선 | Spring Boot 4.1 실행은 가능하지만 프로젝트 하한선으로 선택하지 않음 |
| 21 | 프로젝트 하한선·현재 기준 | LTS이고 현재 팀 환경에 설치되어 있으며 가상 스레드가 정식 제공됨 |
| 25 | 향후 검토 대상 | 최신 LTS이고 장기 운영 기간이 길거나 가상 스레드를 적극 도입할 때 후보 |
| 26 | 사용하지 않음 | 비LTS 릴리스이므로 프로젝트 기준 버전으로 고정하지 않음 |

면접에서의 설명은 다음과 같이 정리할 수 있다.

> Spring Boot의 최소 요구사항인 Java 17이 아니라 Java 21을 프로젝트 하한선으로 정했습니다. 현재 팀 환경이 Java 21로 통일되어 있고 LTS이며, 향후 가상 스레드를 검토할 수 있는 기준선이기 때문입니다. 다만 현재 서비스는 트래픽이 크지 않고 DB 연결 풀이 먼저 병목이 될 가능성이 높아 가상 스레드는 바로 활성화하지 않았습니다. 부하 테스트에서 동시 I/O 대기가 병목으로 확인되면 Java 21 이상의 환경에서 활성화하고 효과를 측정할 계획입니다.

Spring Boot 공식 문서는 가상 스레드가 Java 21 이상에서 동작하지만 최상의 경험을 위해 Java 24 이상을 권장한다고 안내한다. 따라서 가상 스레드를 실제로 채택하는 시점에는 Java 25 LTS로 올릴지 다시 검토한다.

## 4. 테스트 전략

| 테스트 종류 | 적용 범위 | 기준 |
|---|---|---|
| API 통합 테스트 | 목록 조회, 검색·카테고리 필터, 방문자 중복 방지, 인증·권한이 필요한 주요 API | 실제 Spring Context + PostgreSQL Testcontainers |
| Repository 테스트 | 배열 필터, JSONB 매핑, 좋아요 원자적 토글, soft delete 조건, 댓글 부모 제약 | 실제 PostgreSQL 문법과 트랜잭션을 사용 |
| Service 단위 테스트 | 크루 인증, 소유권 검사, 댓글 답글 깊이 제한, 카테고리·필드 검증 | 외부 DB 없이 분기와 정책을 빠르게 검증 |
| Controller 테스트 | 요청 검증과 에러 응답 포맷 | 잘못된 입력이 `VALIDATION_ERROR`로 내려가는지 검증 |
| 외부 인증 테스트 | Supabase/GitHub 자체가 아닌 JWT 검증 경계 | 테스트용 서명 토큰 또는 Mock JWT 사용, 외부 네트워크 호출 금지 |

단위 테스트는 모든 코드 줄을 채우는 것이 목적이 아니다. 정책과 분기가 많은 서비스는 단위 테스트로 빠르게 검증하고, JPA·SQL·권한이 결합된 흐름은 통합 테스트로 검증한다.

## 5. API 문서화 결정

| 용어 | 역할 |
|---|---|
| OpenAPI | 엔드포인트, 요청, 응답, 인증을 표현하는 표준 기계용 명세 |
| Swagger UI | OpenAPI 명세를 브라우저에서 보고 직접 호출하는 화면 |
| springdoc-openapi | Spring Controller와 어노테이션에서 OpenAPI 문서와 Swagger UI를 생성하는 라이브러리 |

1차 서버에서 제공하는 주소는 다음과 같다.

| 주소 | 용도 |
|---|---|
| `/v1/openapi.json` | 프론트엔드·도구가 읽을 OpenAPI 문서 |
| `/v1/swagger-ui.html` | 개발자가 확인하고 호출할 화면 |

초기에는 기존 `api-spec/*.md`를 사람이 읽는 요구사항 문서로 유지하고, 실행 중인 Controller에서 생성된 OpenAPI JSON을 실제 계약의 기준으로 사용한다. 프론트엔드와 협의가 끝나면 JSON을 기준으로 API client 생성 또는 계약 검증을 추가한다.

## 6. 1차 구현 범위와 제외 범위

| 상태 | 내용 |
|---|---|
| 구현 | `/v1/apps`, `/v1/makers`, `/v1/users/me/bookmarks`, `/v1/users/me/likes`, 댓글·답글, 크루 인증, 방문 통계, `/v1/auth/session` |
| 의도적 제외 | `/auth/github/authorize`, callback, refresh, logout의 Spring 중계. Supabase Auth가 계속 담당하므로 중복 구현하지 않음 |
| 후속 | S3 presigned URL 업로드 |
| 후속 | Supabase PostgreSQL → AWS DB 데이터 마이그레이션 |
| 후속 | 실행 수 rate limit·어뷰징 방지 |
| 후속 | 부하 테스트 후 가상 스레드 활성화 여부 결정 |

## 7. 변경 조건

| 조건 | 재검토할 결정 |
|---|---|
| AWS DB 이전 일정이 확정됨 | Flyway baseline, 데이터 이관 순서, dual-write 여부 |
| 썸네일 파일이 서버를 거치기 시작함 | S3 presigned URL, 파일 크기·확장자·권한 정책 |
| 동시 요청 증가와 DB 대기 시간이 병목으로 확인됨 | Hikari pool, EC2 크기, 가상 스레드, 캐시 |
| 검색 조건과 정렬이 복잡해짐 | QueryDSL 또는 검색 인덱스 도입 |
| 팀 전원이 Java 25 환경을 사용할 수 있고 장기 운영이 필요함 | Java 25 LTS 전환 |
| 프론트엔드가 API 계약을 자동 생성해야 함 | OpenAPI 기반 client 생성과 CI 계약 검증 |

## 8. 1차 구현 및 검증 결과

| 검증 항목 | 결과 | 해석 |
|---|---|---|
| `./gradlew clean test` | 성공 | Java 컴파일과 단위 테스트 실행 완료 |
| 서비스 정책 단위 테스트 | 3개 통과 | 크루 전용 등록, 중복 카테고리 거부, 메이커 스냅샷 정책 확인 |
| 인증 경계 단위 테스트 | 2개 통과 | JWT의 UUID 추출과 인증 정보 누락 처리를 확인 |
| PostgreSQL API 통합 테스트 | 2개 테스트 정의, 현재 스킵 | Testcontainers 기반 테스트는 작성했지만 검증 환경에서 Docker Desktop 데몬이 실행되지 않아 컨테이너를 띄우지 못함 |
| `./gradlew bootJar` | 성공 | EC2 배포에 사용할 실행 가능한 JAR 생성 |
| DB 스키마 변경 방지 | 적용 | `ddl-auto=none`, Flyway 비활성화로 기존 Supabase 데이터에 자동 변경을 가하지 않음 |

현재 통합 테스트는 코드에서 제거하거나 H2로 대체하지 않고, Docker가 실행되는 CI 또는 개발 환경에서 실제 PostgreSQL로 재검증하는 것을 다음 검증 단계로 남겼다.

## 참고한 공식 문서

- [Spring Boot 시스템 요구사항](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Boot 4.1.0 출시](https://spring.io/blog/2026/06/10/spring-boot-4/)
- [Spring Boot 가상 스레드](https://docs.spring.io/spring-boot/reference/features/spring-application.html)
- [OpenJDK JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Oracle Java SE 지원 로드맵](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- [springdoc-openapi 호환성 FAQ](https://springdoc.org/faq.html)
