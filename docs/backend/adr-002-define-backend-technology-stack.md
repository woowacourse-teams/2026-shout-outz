# ADR 002: Drop-it 백엔드 기술 스택을 정의한다

## 상태 (Status)

수락됨 (Accepted)

## 컨텍스트 (Context)

Drop-it 백엔드는 서비스 등록, 사용자, 댓글, 좋아요, 북마크 등의 기능을 제공하기 위해 애플리케이션 개발, 데이터 접근, 인증·인가, 테스트, 문서화와 배포에 사용할 기술을 일관되게 정의해야 한다.

프로토타입 단계에서는 Supabase PostgreSQL을 데이터베이스 환경으로 사용하고, Supabase Auth의 GitHub OAuth를 통해 사용자를 인증한다. 운영환경으로 확장하면서 백엔드 애플리케이션 계층을 중심으로 기능을 관리하고, 데이터베이스 이전·스키마 관리·파일 저장소·검색 기능을 단계적으로 검토할 필요가 있다.

이 ADR은 Drop-it 백엔드와 서버 파트의 기준 기술 스택을 정리한다. 현재 사용하는 기술과 향후 조건이 충족될 때 도입을 검토하는 기술을 구분한다. 개별 기술의 구체적인 도입 절차나 데이터베이스 이전 방법은 별도의 ADR에서 결정한다.

## 결정 (Decision)

Drop-it 백엔드는 다음 기술 스택을 기준으로 개발하고 운영한다.

### 현재 사용 기술

| 구분 | 기술 | 적용 내용 |
| --- | --- | --- |
| 언어 | Java 21 LTS | 백엔드 애플리케이션 개발 |
| 프레임워크 | Spring Boot 4.1.0 | 백엔드 애플리케이션 구성 및 실행 |
| 웹 서버 | Spring MVC + Tomcat | 동기식 REST API 제공 |
| 빌드 도구 | Gradle Wrapper 9.6.0 | 의존성 관리 및 빌드 환경 고정 |
| 데이터 접근 | Spring Data JPA, Hibernate | 관계형 데이터의 객체 매핑 및 CRUD 처리 |
| 데이터베이스 | PostgreSQL | 서비스, 사용자, 댓글, 좋아요, 북마크 등의 데이터 저장 |
| 데이터베이스 환경 | Supabase PostgreSQL | 기존 데이터와 스키마를 유지하며 프로토타입 및 초기 백엔드에서 사용 |
| 커넥션 풀 | HikariCP | 데이터베이스 연결 관리 |
| 인증 | Supabase Auth GitHub OAuth | 사용자 로그인 및 Access Token 발급 |
| 인가·토큰 검증 | Spring Security OAuth2 Resource Server, JWT | Supabase Access Token 검증 및 인증 사용자 식별 |
| 요청 검증 | Spring Boot Validation | API 요청 값의 필수값 및 길이 검증 |
| API 문서화 | OpenAPI 3, springdoc-openapi 3.0.3, Swagger UI | API 명세 생성 및 API 테스트 화면 제공 |
| 애플리케이션 메트릭 | Spring Boot Actuator | 애플리케이션 상태, 헬스 체크 및 메트릭 수집 |
| 단위·애플리케이션 테스트 | JUnit, Mockito, AssertJ, Spring Boot Test | 서비스 정책 및 애플리케이션 테스트 |
| 통합 테스트 | Testcontainers PostgreSQL | 실제 PostgreSQL 기반 통합 테스트 |
| 로컬 테스트 실행 환경 | Docker | Docker Compose 기반 테스트 환경 실행 |

### 조건부 도입 예정 기술

다음 기술은 현재 기준으로 확정된 사용 기술이 아니다. 아래 조건이 충족되거나 해당 요구사항이 발생하면 별도의 검토와 의사결정을 거쳐 도입한다.

| 기술 | 도입 검토 조건 | 적용 목적 |
| --- | --- | --- |
| AWS 환경의 PostgreSQL | Supabase PostgreSQL에서 AWS 환경으로 데이터베이스를 이전할 때 | 운영 데이터베이스 환경으로 이전 |
| Flyway | 프로토타입 데이터베이스의 데이터를 이전하고 데이터베이스 이전 계획과 스키마 관리 전략을 확정한 이후 | 데이터베이스 스키마 변경 이력 및 마이그레이션 관리 |
| S3 Presigned URL | 서비스 등록 시점의 이미지와 상세 설명의 Markdown 이미지를 저장해야 할 때 | 클라이언트가 객체 저장소에 이미지를 직접 업로드하도록 처리 |
| QueryDSL | JPA만으로 처리하기 어려운 검색·정렬 조건이 증가할 때 | 동적 검색 및 정렬 쿼리 구성 |
| 추가 OAuth 제공자 | GitHub Login 외의 로그인을 지원할 때 | OAuth 기반 로그인 제공자 확장 |
| AWS EC2 | 운영환경을 AWS에 배포할 때 | 실행 가능한 JAR와 환경 변수를 이용한 서버 배포 |

조건부 도입 예정 기술은 도입 시점에 현재 기술 스택과의 호환성, 운영 비용, 마이그레이션 범위, 보안 요구사항을 다시 검토한다.

## 결과 (Consequences)

**긍정적 영향**: 백엔드 개발과 운영에 필요한 기술의 기준을 공유할 수 있다. Java와 Spring 기반의 애플리케이션 계층, PostgreSQL 기반의 데이터 저장, Supabase Auth 기반의 현재 인증 환경을 일관되게 유지할 수 있다. JUnit·Mockito·AssertJ·Spring Boot Test와 Testcontainers PostgreSQL을 사용해 단위 테스트와 실제 데이터베이스 기반 통합 테스트를 구분할 수 있다. OpenAPI와 Swagger UI를 통해 API 계약을 문서화하고 확인할 수 있다.

**부정적 영향**: Spring, JPA, Spring Security, Supabase Auth 등 여러 구성 요소를 함께 운영해야 하므로 기술 학습과 설정 관리 비용이 발생한다. 현재 Supabase PostgreSQL에 의존하므로 운영환경의 데이터베이스를 변경할 때 데이터 이전과 연결 설정 변경이 필요하다. Testcontainers와 Docker를 사용하는 통합 테스트는 단위 테스트보다 실행 환경과 실행 시간이 증가할 수 있다.

**중립적 영향**: 이 ADR은 기술 스택의 기준을 정의할 뿐, 각 기술의 상세 설정과 운영 절차까지 결정하지 않는다. AWS PostgreSQL의 구체적인 서비스와 데이터베이스 이전 계획, Flyway의 마이그레이션 전략, S3 버킷 정책과 업로드 흐름, QueryDSL 도입 범위, 추가 OAuth 제공자 연동 방식은 요구사항이 확정될 때 별도의 ADR로 결정한다. 백엔드 애플리케이션 계층 도입의 배경과 범위는 [ADR 001](./adr-001-introduce-spring-server.md)에서 다룬다.

## 검토한 대안 (Options Considered)

**대안 1**: 기능이 필요해질 때마다 기술을 개별적으로 선택한다. 단기적으로는 검토 문서 작성 비용을 줄일 수 있지만, 프로젝트 내 기술 선택 기준과 현재 사용 여부가 분산되어 팀의 개발·운영 환경을 일관되게 관리하기 어렵다. 공통 기준을 먼저 정의하기 위해 선택하지 않았다.

**대안 2**: 프로토타입에서 사용한 Supabase 기능을 백엔드의 유일한 기술 스택으로 유지한다. 초기 기능 검증에는 적합하지만, Spring 애플리케이션 계층의 테스트·문서화·운영 기술과 향후 데이터베이스 및 배포 환경의 변경 계획을 표현하기 어렵다. 현재 사용 기술과 조건부 도입 기술을 함께 관리하기 위해 선택하지 않았다.

**대안 3**: 현재 사용 기술과 도입 예정 기술을 하나의 확정된 기술 목록으로 기록한다. 향후 기술을 이미 도입한 것처럼 오해할 수 있고, 마이그레이션이나 스키마 관리 전략이 확정되기 전에 구현이 진행될 수 있다. 기술의 현재 상태와 도입 조건을 분리하기 위해 선택하지 않았다.
