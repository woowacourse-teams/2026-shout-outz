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
| 객체 스토리지 | Amazon S3 | 미디어 버킷 저장 및 조회 |
| AWS SDK | AWS SDK for Java 2.x | S3 객체 접근 및 Presigned URL 생성 |
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
AWS_S3_BUCKET=<bucket-name>
AWS_REGION=<region>
AWS_S3_PRESIGNED_URL_EXPIRATION_SECONDS=<seconds>
```

IntelliJ IDEA에서 환경 변수를 설정하려면 `Run/Debug Configurations`의 `Environment variables`에 입력한다. 운영용 비밀 값은 저장소에 커밋하지 않는다.

## AWS S3 연결 설정

S3 버킷과 기본 보안 설정은 기본 우테코 제공 인프라의 설정을 따라간다. 백엔드는 `AWS_S3_BUCKET`, `AWS_REGION`, `AWS_S3_PRESIGNED_URL_EXPIRATION_SECONDS`만 환경별로 주입받는다.


### 로컬
로컬에서는 AWS CLI Profile 또는 환경변수에서 자격 증명을 조회한다. (자격 증명 값은 `application-*.yml`, 소스 코드에 기록하면 안된다.)

아래는 로컬에 AWS_PROFILE 생성하는 명령어 
```bash
aws configure --profile shoutoutz-dev
export AWS_PROFILE=shoutoutz-dev
export AWS_S3_BUCKET=<우테코에서 제공받은 개발용 버킷 이름>
export AWS_REGION=<region>
export AWS_S3_PRESIGNED_URL_EXPIRATION_SECONDS=<seconds>
```

### 운영
운영에서는 애플리케이션이 실행되는 AWS 런타임에 S3 접근 IAM Role을 연결한다. 장기 액세스 키를 환경변수로 등록하지 않고 AWS SDK의 기본 자격 증명 체인이 제공하는 임시 자격 증명을 사용한다.

현재 구성은 `S3Client`와 `S3Presigner`를 Spring Bean으로 한 번 생성하며, 둘 다 `AWS_REGION`으로 지정한 리전을 사용한다. 로컬 Profile, 운영 IAM Role 등 자격 증명 출처가 달라도 애플리케이션 코드는 같은 기본 자격 증명 체인을 사용한다.

### 미디어 객체 연동 모듈

S3 객체 연동은 `com.shoutoutz.api.media.infrastructure.s3`에서 담당한다.

| 기능 | 구현 | 설명                                                     |
| --- | --- |--------------------------------------------------------|
| 업로드 | `S3Presigner` | 백엔드가 Presigned PUT URL을 발급하고, 프론트엔드가 파일을 S3에 직접 업로드한다. |
| 조회 | `S3Presigner` | 비공개 객체용 Presigned GET URL을 발급한다.                       |
| 업로드 검증 | `S3Client.headObject` | 객체 존재 여부와 요청 당시의 파일 크기, MIME 타입을 비교한다.                 |
| 삭제 | `S3Client.deleteObject` | `media/` prefix의 객체를 삭제한다.                             |

객체 키는 `media/{purpose-kebab-case}/{UUID}` 형식이며 원본 파일명이나 DB ID를 포함하지 않는다. Presigned PUT URL로 업로드할 때는 URL 발급 응답의 `Content-Type`을 업로드 요청 헤더에 동일하게 지정해야 한다. S3 객체의 파일 시그니처 검증과 이미지 변형본 생성은 별도의 이미지 처리 단계에서 수행한다.

### 업로드 시작 API

`POST /api/v1/media/uploads`는 인증된 사용자가 미디어 업로드를 시작할 때 호출한다. 현재 인증 어댑터 계약은 `Principal.getName()`에 `users.id`를 문자열로 제공하는 것이며, 인증 주체가 없으면 요청을 거부한다.

요청 예시:

```json
{
  "purpose": "POST_CONTENT",
  "targetId": 42,
  "originalFileName": "post-image.webp",
  "contentType": "image/webp",
  "sizeBytes": 1048576
}
```

서버는 대상 수정 권한과 이미지 업로드 정책을 확인한 뒤 `media_metadata`에 `PENDING_UPLOAD` 레코드를 만들고 Presigned PUT URL을 반환한다. 프론트엔드는 응답의 `uploadUrl`로 S3에 직접 PUT하고, `contentType`을 요청 헤더에 동일하게 지정해야 한다. S3 업로드가 끝난 뒤의 완료 확인과 `PROCESSING` 전이는 후속 API에서 처리한다.
