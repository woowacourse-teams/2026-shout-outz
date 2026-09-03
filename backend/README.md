# Shout-outz Backend

백엔드는 Spring Boot 기반의 애플리케이션/API 서버로 동작하며, 현재는 서버 초기 구성과 로컬 개발 환경을 제공한다.

## 프로젝트 정보

- 애플리케이션 이름: `shout-outz`
- 기본 패키지: `com.shoutoutz.api`
- 실행 모듈: `backend`
- 기본 실행 프로필: `local`
- 운영 프로필: `prod`
- 로컬 데이터베이스: Docker Compose로 실행하는 PostgreSQL 17

## 기술 스택

### 현재 적용 기술

| 구분 | 기술 | 용도 |
| --- | --- | --- |
| 언어 | Java 21 LTS | 백엔드 애플리케이션 개발 |
| 프레임워크 | Spring Boot 4.1.1 | 애플리케이션 구성 및 실행 |
| 웹 계층 | Spring MVC, Tomcat | 동기식 REST API 제공 |
| 빌드 | Gradle Wrapper 9.6.0 | 의존성 관리, 빌드, 테스트 실행 |
| 데이터 접근 | Spring Data JPA, Hibernate | 엔티티 매핑, CRUD, 트랜잭션 처리 |
| 데이터베이스 | PostgreSQL 17 | 로컬 개발 데이터 저장 |
| 커넥션 풀 | HikariCP | 데이터베이스 연결 관리 |
| 요청 검증 | Spring Boot Validation | 요청 값 검증 |
| 운영 상태 확인 | Spring Boot Actuator | 헬스 체크 및 메트릭 제공 |
| 테스트 | JUnit, Spring Boot Test | 애플리케이션 테스트 |
| 로컬 인프라 | Docker Compose | PostgreSQL 컨테이너 실행 |
| 보조 도구 | Lombok | 반복적인 Java 코드 축소 |

## 실행 환경 준비

다음 도구를 설치한다.

- Git
- JDK 21
- IntelliJ IDEA
- Docker Desktop

## 애플리케이션 실행

### 1. 프로젝트 내려받기

```bash
git clone https://github.com/woowacourse-teams/2026-shout-outz.git
cd 2026-shout-outz/backend
```

IntelliJ IDEA에서는 `backend` 디렉터리의 `build.gradle`을 기준으로 프로젝트를 연다.

### 2. IntelliJ 설정

- Project SDK를 JDK 21로 설정한다.
- Gradle JVM을 JDK 21로 설정한다.
- Gradle Wrapper를 사용한다.
- Build and run using과 Run tests using을 Gradle로 설정한다.
- Java Compiler를 21로 설정한다.
- Lombok 플러그인을 설치하고 annotation processing을 활성화한다.

### 3. 로컬 데이터베이스 실행

Docker Desktop을 실행한 뒤 `backend` 디렉터리에서 PostgreSQL 컨테이너를 시작한다.

```bash
docker compose up -d
```

현재 로컬 데이터베이스 설정은 다음과 같다.

| 항목 | 값 |
| --- | --- |
| 컨테이너 이름 | `shoutoutz-db` |
| 데이터베이스 | `shoutoutz` |
| 사용자 | `shoutoutz` |
| 비밀번호 | `localdev` |
| 포트 | `5432` |

컨테이너가 실행 중인지 확인한다.

```bash
docker ps
```

최초 실행 후 데이터베이스 연결 정보를 확인할 수 있다.

```bash
docker compose exec db psql -U shoutoutz -d shoutoutz -c "\\conninfo"
```

### 4. 애플리케이션 실행

`application.yml`의 기본 프로필은 `local`이다. 로컬 실행 시 `application-local.yml`의 PostgreSQL 설정을 사용한다.

```bash
./gradlew bootRun
```

또는 IntelliJ IDEA에서 `ShoutOutzApplication`을 실행한다.

애플리케이션이 실행되면 로그에서 다음 내용을 확인한다.

- `local` 프로필로 실행됨
- 데이터베이스 연결 성공

## API 문서 생성 및 확인

API 문서는 MockMvc 기반 테스트 코드와 `restdocs-api-spec`을 이용해 생성한다.
API를 추가하거나 문서 내용을 수정할 때는 해당 컨트롤러 테스트에 문서화 코드를 작성한 뒤 다음 명령을 실행한다.

```bash
./gradlew copyOasToSwagger
```

`copyOasToSwagger`는 다음 작업을 순서대로 수행한다.

1. 테스트 실행
2. 테스트 결과로 OpenAPI YAML 생성
3. `src/main/resources/static/docs/openapi3.yaml`에 YAML 복사

따라서 문서 갱신을 위해 `./gradlew test`나 `./gradlew openapi3`를 별도로 실행할 필요가 없다.
문서 생성에 필요한 테스트가 실패하면 YAML 복사도 완료되지 않는다.

생성된 문서를 확인하려면 애플리케이션을 재시작한 뒤 다음 주소로 접속한다.

```text
http://localhost:8080/docs
```

OpenAPI 원본 YAML은 다음 주소에서 확인할 수 있다.

```text
http://localhost:8080/docs/openapi3.yaml
```

### API 문서 작성 규칙

- 문서 테스트는 각 컨트롤러 테스트 클래스에 작성한다.
- 테스트 코드의 `document(...)` 내용을 OpenAPI 문서에 반영할 API 계약으로 취급한다.
- `openapi3.yaml`은 직접 수정하지 않고 테스트 코드 수정 후 명령어로 재생성한다.
- API 테스트 코드와 생성된 `openapi3.yaml` 변경 사항은 함께 커밋한다.
- API의 요청, 응답 필드가 변경되면 문서 테스트와 YAML 변경 여부를 함께 확인한다.

### 5. 애플리케이션 종료

애플리케이션을 종료한 뒤 PostgreSQL 컨테이너를 중지한다.

```bash
docker compose down
```

`docker compose down`은 볼륨을 유지한다. 로컬 데이터까지 삭제하려면 다음 명령을 사용한다.

```bash
docker compose down -v
```

`-v` 옵션을 사용하면 저장된 로컬 데이터가 삭제되므로 주의한다.

## 환경 변수

로컬 프로필은 `application-local.yml`의 로컬 데이터베이스 설정을 사용한다.

운영 프로필은 다음 환경 변수를 필요로 한다.

```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<database>
SPRING_DATASOURCE_USERNAME=<username>
SPRING_DATASOURCE_PASSWORD=<password>
```

IntelliJ IDEA에서 환경 변수를 설정하려면 `Run/Debug Configurations`의 `Environment variables`에 입력한다. 운영용 비밀 값은 저장소에 커밋하지 않는다.
