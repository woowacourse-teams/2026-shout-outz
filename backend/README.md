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

## 질문 AI 추천

질문(`feedType=QUESTION`)은 생성 후 Outbox를 거쳐 SQS 작업 큐로 전달하고, Worker가 Gemini를 먼저 호출해 연관 질문 5개를 저장한다. Gemini 호출이나 추천 응답 검증에 실패하면 같은 작업에서 OpenAI를 한 번 호출한다. 질문 제목이나 본문을 수정하면 새 추천 작업을 등록한다. 생성·수정 요청은 추천 생성을 기다리지 않으며 응답의 `recommendationStatus`는 `PENDING`, `PROCESSING`, `READY`, `FAILED` 중 하나다.

- `GET /api/v1/feeds/{feedId}`의 `relatedQuestionRecommendations`에는 추천 상태와 추천 질문 목록이 담긴다. 생성 완료 전에는 `items`가 빈 배열이고, 완료되면 최대 5개가 제공된다.
- `POST /api/v1/feeds`와 `PUT /api/v1/feeds/{feedId}`는 질문 추천 작업이 활성화된 환경에서 `recommendationStatus`를 반환한다. 질문 제목이나 본문이 바뀌지 않은 수정은 기존 상태를 유지한다.
- `GET /api/v1/feeds?type=QUESTION`은 실제 질문을 기본 20개 단위로 페이지네이션한다. `meta.recommendationSlots`에는 실제 질문 5개마다 하나씩 보여줄 추천 질문과 `afterFeedId`가 담긴다. 준비된 추천이 없으면 슬롯은 생략된다. 추천 슬롯은 피드 `data`, 커서, `totalCount`에 포함되지 않는다.
- 추천 목록 슬롯은 페이지 응답 시점에 READY 상태인 추천 질문 중 무작위로 채운다. 다음 페이지를 요청해도 앞 페이지 응답은 바뀌지 않는다. 새로고침이나 첫 페이지 재조회에서는 슬롯이 달라질 수 있다.

### 사용된 추천 질문 처리

- 추천 질문으로 질문을 등록할 때 `POST /api/v1/feeds` 요청에 `recommendationId`를 함께 보낸다. 해당 추천은 사용됨(`consumed_feed_id`, `consumed_at`)으로 표시되고 상세와 목록 슬롯에 더 이상 노출되지 않는다.
- 무효하거나 이미 사용된 `recommendationId`는 무시하며 질문 등록은 그대로 성공한다. 400을 반환하지 않는다.
- 사용으로 줄어든 미사용 추천은 원글당 5개를 맞추도록 비동기로 대체 생성한다. REPLENISH Outbox를 거쳐 SQS로 전달하고 Worker가 LLM을 호출한다. 대체 생성 중에도 추천 상태는 `READY`로 유지되어 남은 추천이 계속 노출된다.
- 질문 수정으로 추천을 재생성하면 진행 중인 대체 생성 결과는 폐기한다.
- 대체 생성이 최종 실패하면 `last_error`만 기록하고 남은 추천과 `READY` 상태는 유지한다. 시도 횟수는 초기화되어 다음에 추천이 사용되면 다시 대체 생성을 시도한다.
- 비용 가드로 generation당 대체 생성 총량을 `question-recommendation.max-replenish-per-generation`(기본 5)으로 제한한다. 이 값을 넘으면 더 생성하지 않는다.

추천 작업을 실행할 환경에는 Gemini 키, OpenAI 키와 SQS 큐 설정이 필요하다. SQS 기본값은 비활성화이며, 활성화하면 Gemini 키를 검사한다. 자동 대체는 기본 활성화이므로 OpenAI 키도 필요하고 누락 시 기동에 실패한다. 로컬에서 작업자까지 실행하려면 다음 환경 변수를 설정한다.

```bash
QUESTION_RECOMMENDATION_SQS_ENABLED=true
QUESTION_RECOMMENDATION_QUEUE_URL=<work-queue-url>
QUESTION_RECOMMENDATION_DLQ_URL=<dead-letter-queue-url>
GEMINI_API_KEY=<gemini-api-key>
OPENAI_API_KEY=<openai-api-key>
QUESTION_RECOMMENDATION_FALLBACK_ENABLED=true
```

작업 큐에는 DLQ redrive 정책을 연결하고 `maxReceiveCount`를 `QUESTION_RECOMMENDATION_MAX_GENERATION_ATTEMPTS` 기본값인 5와 맞춘다. 
큐와 DLQ는 같은 AWS 리전에 두고, 실행 역할에 작업 큐 `SendMessage`, `ReceiveMessage`, `DeleteMessage`, `ChangeMessageVisibility`와 DLQ `ReceiveMessage`, `DeleteMessage` 권한을 부여한다. 
API 키는 환경 변수나 비밀 저장소로 주입하고 저장소에 기록하지 않는다. 기존 `GEMINI_*` 환경 변수는 그대로 사용한다. YAML 설정 경로는 `question-recommendation.gemini`에서 `llm.gemini`로 이동했고, OpenAI 설정은 `llm.openai`에 있다. 추천 전용 프롬프트·응답 검증은 추천 생성기에, HTTP 요청과 제공자 응답 해석은 `common.llm`에 둔다.

SQS poller는 한 번에 가져온 메시지를 순서대로 처리한다. 기본 `QUESTION_RECOMMENDATION_SQS_MAX_MESSAGES_PER_POLL=1`을 유지한다. 배치를 크게 잡으면 뒤 메시지의 가시성 시간이 앞 작업 처리에 쓰일 수 있다.

| 환경 변수 | 기본값 | 용도 |
| --- | --- | --- |
| `QUESTION_RECOMMENDATION_FALLBACK_ENABLED` | `true` | Gemini 실패 시 OpenAI 호출. `false`이면 Gemini만 사용하며 OpenAI 키 없이 기동 가능 |
| `GEMINI_MODEL` | `gemini-3.8-flash` | 기본 제공자 모델 |
| `GEMINI_CONNECT_TIMEOUT` / `GEMINI_READ_TIMEOUT` | `5s` / `45s` | Gemini 연결·응답 제한 시간 |
| `OPENAI_MODEL` | `gpt-6-luna` | 대체 제공자 모델 |
| `OPENAI_REASONING_EFFORT` | `none` | OpenAI 추론 예산. 모델 변경 시 해당 값과 Structured Outputs 지원 여부 확인 |
| `OPENAI_CONNECT_TIMEOUT` / `OPENAI_READ_TIMEOUT` | `5s` / `45s` | OpenAI 연결·응답 제한 시간 |
| `QUESTION_RECOMMENDATION_SQS_MAX_MESSAGES_PER_POLL` | `1` | poller가 한 번에 가져올 메시지 수. 순차 처리하므로 기본값 유지 권장 |
| `QUESTION_RECOMMENDATION_SQS_VISIBILITY_TIMEOUT` | `3m` | SQS 메시지 기본 가시성 제한 시간 |

### 장애와 재시도

- 한 번의 생성 시도에서 Gemini 최대 1회, OpenAI 최대 1회를 호출한다. 추천 5개는 한 요청에서 생성한다.
- 두 제공자가 모두 실패하면 재시도 가능한 오류가 있는지 판별한다. HTTP 408·429·5xx, 통신 장애, 불완전하거나 잘못된 추천 응답은 재시도 대상으로 취급한다. 단, OpenAI의 `insufficient_quota`와 `billing_hard_limit_reached`는 HTTP 429여도 영구 오류다. 인증·권한 등 영구 오류만 남으면 DB에 `FAILED`를 기록하고 작업 큐 메시지를 삭제한다.
- 재시도는 SQS 메시지의 visibility timeout으로 예약한다. 워커 스레드에서 잠들거나 HTTP 호출 안에서 재시도를 중첩하지 않는다. 지연은 5·10·20·40초 순으로 증가하고 최대 20%의 무작위 지연을 추가하며, 기본 상한은 300초다. 제공자의 `Retry-After` 또는 Gemini `RetryInfo`가 더 길면 첫 수신 시점부터 12시간인 SQS 상한의 남은 시간까지만 기다린다.
- 최대 생성 시도 후에는 SQS redrive 정책에 따라 DLQ로 이동하고, DLQ poller가 DB에 `FAILED`와 `MAX_RETRIES_EXCEEDED`를 기록한 뒤 메시지를 삭제한다. 따라서 DLQ가 비어 있어도 실패한 작업은 DB에 남을 수 있다.
- SQS가 활성화되면 애플리케이션은 Gemini와 fallback OpenAI의 제한 시간 합이 작업 선점 시간 180초와 설정한 SQS visibility timeout보다 짧은지 확인한다. 둘 중 하나 이상이면 애플리케이션 기동에 실패한다.

제공자 호출 실패 로그에는 `provider`, `model`, `httpStatus`, `errorCode`, `retryAfterSeconds`, `fallback`이 남는다. `traceId=recommendation-{feedId}-{generation}-{attempts}`로 워커의 실패·성공 로그를 연결할 수 있다. API 키, 질문, 응답 본문과 제공자가 반환한 원문 오류 메시지는 로그에 기록하지 않는다. 이 로그는 `prod` 프로필의 `/var/log/shout-outz/app.log`에 저장되며 CloudWatch Agent를 적용한 환경에서는 기존 앱 로그 그룹으로 수집된다.

생성 시도 횟수는 `question.recommendation.generation.count` 지표로, 생성 소요 시간은 `question.recommendation.generation` 타이머로 확인할 수 있다. 두 지표 모두 `outcome` 태그를 제공한다. 현재 CloudWatch Prometheus 수집 설정은 HTTP·DB 커넥션 지표만 허용하므로 추천 지표를 장기 보관하려면 수집 필터와 metric declaration도 별도로 추가해야 한다.

### OpenAI 결제와 서버 적용

1. [OpenAI Platform](https://platform.openai.com/)에서 프로젝트를 만들고 API 결제 수단·크레딧을 설정한 뒤 해당 프로젝트의 API 키를 발급한다. 결제 활성화와 계정별 모델 접근 권한은 운영자가 확인한다.
2. EC2의 `/opt/shout-outz/shout-outz.env`에 `OPENAI_API_KEY`를 추가한다. 모델·제한 시간은 위 기본값을 사용하거나 환경 변수로 지정한다. Gemini 키도 유지한다.
3. 새 버전을 배포하고 서비스를 재시작한다. 재시작만으로 이전 버전에 OpenAI 기능이 추가되지는 않는다. 키가 아직 준비되지 않았으면 배포 전에 `QUESTION_RECOMMENDATION_FALLBACK_ENABLED=false`로 설정해 Gemini만 사용한다.
4. 배포 후 질문을 등록해 `PENDING → PROCESSING → READY` 전환을 확인한다. 실제 Gemini 장애 중에는 같은 traceId에서 Gemini 실패와 `provider=OPENAI, fallback=true` 완료 로그를 확인한다. 실 API 검증은 실제 사용료가 발생하므로 자동 테스트는 외부 HTTP 호출을 모의한다.

기본 모델은 [GPT-6 Luna](https://developers.openai.com/api/docs/models/gpt-6-luna), 요청 방식은 Responses API의 Structured Outputs다. `store=false`, `reasoning.effort=none`, 출력 토큰 상한 4,096으로 요청한다. 2026-10-15에 확인한 [공식 Standard 요금](https://developers.openai.com/api/docs/pricing)은 짧은 컨텍스트 기준 입력 100만 토큰당 $0.10, 출력 100만 토큰당 $0.50이다. 예를 들어 입력 1,000토큰·출력 3,000토큰을 가정하면 OpenAI 호출 1회는 약 $0.0016, 1,000회는 약 $1.60이다. 실제 토큰 수를 측정한 견적은 아니며 캐시 비용·세금·환율·Gemini 비용은 별도다. Gemini 성공 시 OpenAI는 호출하지 않고, 두 제공자 실패 시 재시도마다 호출 비용이 추가될 수 있다.

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
CORS_ALLOWED_ORIGINS=https://shout-ou.tz,https://*.shout-ou.tz
AWS_S3_BUCKET=<bucket-name>
AWS_REGION=<region>
AWS_S3_KEY_PREFIX=<key-prefix>
AWS_S3_PRESIGNED_URL_EXPIRATION_SECONDS=<seconds>
AWS_CLOUDFRONT_PUBLIC_BASE_URL=https://<cloudfront-domain>/
VISITOR_HASH_SECRET=<32자 이상의 임의 문자열>
```

IntelliJ IDEA에서 환경 변수를 설정하려면 `Run/Debug Configurations`의 `Environment variables`에 입력한다. 운영용 비밀 값은 저장소에 커밋하지 않는다.

`CORS_ALLOWED_ORIGINS`에는 쉼표로 구분한 Origin 또는 Origin 패턴을 입력한다. `https://*.shout-ou.tz`는 서브도메인만 허용하므로 루트 도메인도 사용한다면 `https://shout-ou.tz`를 별도로 함께 지정해야 한다. 자격 증명을 포함한 CORS 요청을 허용하므로 전체 Origin을 여는 `*` 대신 팀이 관리하는 도메인만 명시한다.

## AWS S3 연결 설정

S3 버킷과 기본 보안 설정은 기본 우테코 제공 인프라의 설정을 따라간다. 백엔드는 `AWS_S3_BUCKET`, `AWS_REGION`, `AWS_S3_KEY_PREFIX`, `AWS_S3_PRESIGNED_URL_EXPIRATION_SECONDS`, `AWS_CLOUDFRONT_PUBLIC_BASE_URL`을 환경별로 주입받는다.

S3 버킷은 계속 private으로 유지하고, 공개 이미지 조회는 CloudFront를 통해 제공한다. CloudFront는 S3를 origin으로 사용하며, origin access control(OAC)로 CloudFront만 S3 객체를 읽을 수 있도록 구성해야 한다. `AWS_CLOUDFRONT_PUBLIC_BASE_URL`에는 배포된 CloudFront 도메인의 scheme과 host를 입력한다.

`develop`의 DEV 배포도 현재 `prod` 프로필로 실행되므로 DEV 서버에는 `<dev-key-prefix>`를 주입한다. 운영 환경으로 전환할 때는 애플리케이션 설정 파일을 수정하지 않고 `AWS_S3_KEY_PREFIX`만 `<prod-key-prefix>`로 변경한다.


### 로컬
로컬에서는 AWS CLI Profile 또는 환경변수에서 자격 증명을 조회한다. (자격 증명 값은 `application-*.yml`이나 소스 코드에 기록하면 안된다.)

아래는 로컬에 AWS_PROFILE 생성하는 명령어 
```bash
aws configure --profile shoutoutz-dev
export AWS_PROFILE=shoutoutz-dev
export AWS_S3_BUCKET=<bucket-name>
export AWS_REGION=<region>
export AWS_S3_KEY_PREFIX=<dev-key-prefix>
export AWS_S3_PRESIGNED_URL_EXPIRATION_SECONDS=<seconds>
export AWS_CLOUDFRONT_PUBLIC_BASE_URL=https://<cloudfront-domain>/
```

### 실제 AWS S3 통합 테스트

기본 테스트는 S3 클라이언트를 mock으로 대체한다. 실제 AWS S3 연동을 확인하려면 DEV용 AWS 자격 증명과 위 환경 변수를 설정한 뒤 다음 테스트를 명시적으로 실행한다.

```bash
RUN_AWS_INTEGRATION_TEST=true ./gradlew test --tests '*S3MediaStorageAwsIntegrationTest'
```

테스트는 `AWS_S3_KEY_PREFIX` 아래에 UUID가 포함된 임시 객체를 만들고 Presigned PUT, HeadObject, GetObject, PutObject, DeleteObject를 확인한 뒤 생성한 객체를 삭제한다. 조회용 공개 URL은 CloudFront 배포 상태에 의존하므로 S3 통합 테스트에서는 Presigned GET을 검증하지 않는다. 운영용 자격 증명이나 운영 prefix로 실행하지 않는다.

### 운영
운영에서는 애플리케이션이 실행되는 AWS 런타임에 S3 접근 IAM Role을 연결한다. 장기 액세스 키를 환경변수로 등록하지 않고 AWS SDK의 기본 자격 증명 체인이 제공하는 임시 자격 증명을 사용한다.

현재 구성은 `S3Client`와 `S3Presigner`를 Spring Bean으로 한 번 생성하며, 둘 다 `AWS_REGION`으로 지정한 리전을 사용한다. 로컬 Profile, 운영 IAM Role 등 자격 증명 출처가 달라도 애플리케이션 코드는 같은 기본 자격 증명 체인을 사용한다.

### 미디어 객체 연동 모듈

S3 객체 연동은 `com.shoutoutz.api.media.infrastructure.s3`에서 담당한다.

| 기능 | 구현 | 설명                                                     |
| --- | --- |--------------------------------------------------------|
| 업로드 | `S3Presigner` | 백엔드가 Presigned PUT URL을 발급하고, 프론트엔드가 파일을 S3에 직접 업로드한다. |
| 조회 | `MediaPublicUrlResolver` | private S3 객체의 CloudFront 공개 URL을 계산한다. |
| 업로드 검증 | `S3Client.headObject` | 객체 존재 여부와 요청 당시의 파일 크기, MIME 타입을 비교한다.                 |
| 삭제 | `S3Client.deleteObject` | 환경별 `AWS_S3_KEY_PREFIX` 아래의 객체를 삭제한다.                             |

DB에 저장하는 논리 객체 키는 `media/{purpose-kebab-case}/{UUID}` 형식이며 원본 파일명이나 DB ID를 포함하지 않는다.
AWS SDK 요청에는 논리 키의 `media/`를 제거한 뒤 환경별 `AWS_S3_KEY_PREFIX`를 붙인 실제 객체 키를 사용한다.
Presigned PUT URL로 업로드할 때는 URL 발급 응답의 `Content-Type`을 업로드 요청 헤더에 동일하게 지정해야 한다. S3 객체의 파일 시그니처 검증과 이미지 변형본 생성은 별도의 이미지 처리 단계에서 수행한다.

### 업로드 시작 API

`POST /api/v1/media/uploads`는 인증된 사용자가 미디어 업로드를 시작할 때 호출한다. 현재 인증 어댑터 계약은 `Principal.getName()`에 `users.id`를 문자열로 제공하는 것이며, 인증 주체가 없으면 요청을 거부한다.

요청 예시:

```json
{
  "purpose": "FEED_CONTENT",
  "originalFileName": "feed-image.webp",
  "contentType": "image/webp",
  "sizeBytes": 1048576
}
```

서버는 인증된 활성 사용자와 이미지 업로드 정책을 확인한 뒤 `media_metadata`에 `PENDING_UPLOAD` 레코드를 만들고 Presigned PUT URL을 반환한다. 프론트엔드는 응답의 `uploadUrl`로 S3에 직접 PUT하고, `contentType`을 요청 헤더에 동일하게 지정해야 한다. S3 업로드가 끝나면 완료 API를 호출한다. 프로젝트·피드·프로필에 미디어를 연결할 때는 각 도메인 API에서 소유자·용도·처리 상태를 다시 검증한다.

### 업로드 완료 API

`POST /api/v1/media/{mediaId}/complete`는 S3 업로드가 끝난 뒤 호출한다. 요청 본문은 없으며, 서버는 인증된 업로더인지 확인한 뒤 S3 `HeadObject`로 객체 존재 여부·크기·Content-Type을 검증한다.

- 검증 성공: `PROCESSING` 상태로 변경하고 미디어 정보를 반환한다.
- S3 객체 없음: `409 Conflict`, `PENDING_UPLOAD` 유지
- 크기·Content-Type 불일치: `FAILED` 저장 후 `422 Unprocessable Entity`
- 이미 `PENDING_UPLOAD`가 아닌 상태: `409 Conflict`

현재 완료 요청의 중복 멱등 처리와 동시 요청 직렬화는 구현하지 않았다.

### 도메인 미디어 참조

단일 이미지만 필요한 프로젝트 썸네일과 사용자 프로필 이미지는 별도 매핑 테이블을 만들지 않고 각 도메인 테이블이 미디어를 직접 참조한다.

```text
projects.thumbnail_media_id  -> media_metadata.id
user_profiles.avatar_image_id -> media_metadata.id
```

프로젝트 썸네일과 사용자 프로필 이미지는 URL을 저장하지 않고 각각 `thumbnail_media_id`, `avatar_image_id`로 `media_metadata.id`를 참조한다. 조회 시 `media_metadata.s3_key`와 변형 규칙을 기준으로 CloudFront 공개 URL을 계산한다.

### 조회 응답의 미디어 URL

별도의 미디어 ID 조회 API는 제공하지 않는다. 프로젝트 목록·상세, 피드, 사용자 프로필 등 조회 응답은 `MediaUrlResolver`를 통해 READY 미디어의 변형 용도에 맞는 CloudFront URL을 직접 반환한다. 프로젝트 목록은 `thumbnailUrl`, 프로젝트 상세는 `imageUrl`, 그 외 조회 응답은 `avatarUrl`, `media[].url`을 사용하며 조회 응답에는 `media_id`를 노출하지 않는다.

업로드 시작·완료 응답의 `mediaId`는 업로드 후 프로젝트·피드·프로필에 미디어를 연결하는 명령 요청에 사용한다.

본문에는 만료되는 S3 URL을 저장하지 않고 `media://{mediaId}`를 저장한다.

```markdown
![프로젝트 화면](media://123)
```

본문을 응답할 때 이 참조를 READY 미디어의 CloudFront URL로 치환한다. URL은 만료되지 않으므로 클라이언트가 별도의 미디어 조회 API를 호출할 필요가 없다.

프로젝트 수정 요청에서 상세 응답의 `descriptionMd`를 그대로 다시 보내는 경우에도, 기존 프로젝트 본문에 연결된 CloudFront URL은 URI의 scheme·host·port·path를 기준으로 저장 전에 `media://{mediaId}` 참조로 복원한다. 매칭되지 않는 외부 이미지 URL은 저장하지 않는다.
