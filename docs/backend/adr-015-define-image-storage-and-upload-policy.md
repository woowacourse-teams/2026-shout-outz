# ADR-015: 이미지 저장 및 업로드 기준을 정의

### 결정일 및 결정자
- 결정일: 2026.08.31
- 결정자: 조상준(샤를)
- 작성자: 조상준(샤를)

## 상태 (Status)

수락됨(Accepted)

## 컨텍스트 (Context)

shout-outz에는 프로젝트, 포스트, 사용자 도메인에서 이미지 파일이 발생한다.

현재 데이터 모델은 이미지 파일 자체가 아니라 URL 또는 Markdown 본문을 저장하며, 미디어 메타데이터를 저장할 테이블과 프로젝트·게시글·사용자와의 연결 방식은 정의되지 않은 상태다.

이미지 파일을 애플리케이션 서버가 직접 전달하면 서버의 네트워크·메모리 부담이 커지고, S3를 공개하면 업로드 권한과 파일 접근 권한을 분리하기 어렵다. 또한 본문에 S3 Presigned URL을 직접 저장하면 URL이 만료된 뒤 이미지가 깨진다.

이번 결정은 우테코에서 제공한 S3 리소스를 사용하는 것을 전제로, 미디어 테이블과 업로드·조회 API가 따라야 할 공통 기준을 정의한다. 버킷 생성과 기본 AWS 보안 설정은 제공 인프라의 운영 기준을 따르며, 이 저장소에서 별도의 버킷 IaC로 재정의하지 않는다.

현재 공개 서비스에서 확인되는 규모는 서비스 등록 크루 44명, 등록 서비스 55개, 누적 방문자 486명이다(2026.08.31 확인). 이는 전체 가입자 수나 동시 업로드 수가 아니라 공개 화면에서 확인 가능한 현재 규모 지표이므로, 전용 이미지 처리 서버 도입 여부를 판단하는 초기 근거로만 사용한다.

## 결정 (Decision)

### 1. 저장소와 접근 방식

- 실제 이미지 파일은 Amazon S3에 저장한다.
- S3 버킷과 객체는 기본적으로 비공개로 유지한다.
- 클라이언트는 백엔드가 발급한 Presigned PUT URL로 업로드한다.
- 비공개 이미지 조회 시 백엔드가 권한을 확인한 뒤 Presigned GET URL을 발급한다.
- 공개 화면에서 사용하는 이미지도 S3 버킷을 직접 공개하지 않는다. 필요하면 이후 CDN을 private origin과 함께 도입한다.
- Presigned URL 자체를 `description_md`나 `posts.content`에 저장하지 않는다. 본문에는 `media://{mediaId}` 형식의 안정적인 내부 참조를 저장하고, 응답 시점에 접근 URL을 생성한다.
- 버킷 공개 차단, 객체 소유권, 기본 암호화, HTTPS 강제, CORS와 멀티파트 업로드 정리 설정은 우테코 제공 인프라의 설정을 따른다.
- 백엔드는 제공된 런타임 IAM 권한으로 필요한 미디어 객체 작업만 수행하며, 프론트엔드에는 AWS 자격 증명을 전달하지 않는다.
- 실제 연동 전에 제공 인프라의 버킷 리전, CORS 허용 origin, `media/*` 객체에 대한 백엔드 권한을 확인한다.

### 2. 업로드 흐름

```text
권한 확인
→ media 레코드 생성 (PENDING_UPLOAD)
→ Presigned PUT URL 발급
→ 프론트엔드가 S3에 업로드
→ 프론트엔드가 완료 API 호출
→ 백엔드가 S3 객체와 메타데이터 확인
→ 이미지 처리 작업 등록 (PROCESSING)
→ 변형본 생성 완료 (READY)
```

프론트엔드가 S3 업로드에 성공했더라도 완료 API 호출이 실패할 수 있다. 따라서 백엔드는 고정된 시간만 기다리지 않고 완료 API에서 `HeadObject`로 확인하며, 완료 요청이 오지 않은 `PENDING_UPLOAD` 레코드는 만료 정책으로 정리한다.

### 3. 미디어 상태

| 상태 | 의미 | 허용되는 다음 상태 |
| --- | --- | --- |
| `PENDING_UPLOAD` | Presigned URL을 발급했지만 완료 확인 전 | `PROCESSING`, `FAILED`, `EXPIRED` |
| `PROCESSING` | S3 객체 확인 후 파일 시그니처 검증·EXIF 제거·변형본 생성 중 | `READY`, `FAILED` |
| `READY` | 원본 검증과 필요한 변형본 처리가 완료됨 | 없음 |
| `FAILED` | 업로드 검증 또는 이미지 처리에 실패함 | 없음 |
| `EXPIRED` | 업로드 완료 요청 없이 유효 시간이 지남 | 없음 |

S3 업로드 실패가 백엔드에 자동으로 전달되는 것은 아니다. 프론트엔드가 실패를 보고하지 않으면 미디어는 `PENDING_UPLOAD`로 남고, 만료 정리 대상이 된다. `FAILED`는 백엔드가 객체를 확인한 뒤 검증·처리 실패를 확정한 경우에 사용한다.

### 4. 초기 이미지 업로드 정책

- 초기 업로드 대상은 이미지로 한정한다.
- 허용 Content-Type은 `image/jpeg`, `image/png`, `image/webp`다.
- 최대 업로드 크기는 10MiB다.
- 파일 확장자나 클라이언트가 보낸 Content-Type만 신뢰하지 않고, 업로드 완료 후 파일 시그니처를 검증한다.
- SVG, GIF, PDF, 동영상, 압축파일은 초기 범위에서 제외한다.
- EXIF 제거와 원본·표시용·썸네일 변형본 생성은 이미지 처리 단계에서 수행한다.

### 5. 업로드 권한 기준

Presigned URL 발급 API는 인증된 백엔드 사용자만 호출할 수 있으며, 요청 본문의 `user_id`를 권한 판단 기준으로 사용하지 않는다. 백엔드는 인증된 principal과 대상 리소스를 기준으로 권한을 확인한다.

| 미디어 용도 | 업로드 권한 기준 |
| --- | --- |
| 사용자 프로필 이미지 | 인증된 본인 프로필만 수정 |
| 프로젝트 썸네일·설명·미디어 | 해당 프로젝트를 수정할 수 있는 사용자만 업로드 |
| 게시글 본문 이미지 | 해당 게시글을 수정할 수 있는 작성자만 업로드 |
| OAuth·아카이브 프로필 이미지 | 사용자 업로드 대상이 아니라 외부 동기화·import 데이터로 취급 |

구체적인 인증 방식(GitHub OAuth와 세션·토큰의 조합)은 별도 인증 ADR에서 결정한다. 인증 principal을 확인할 수 없는 동안에는 Presigned URL 발급 API를 공개하지 않는다.

### 6. 코드에 반영한 기준

다음 기준을 `com.shoutoutz.api.media.domain`에 고정했다.

- `MediaPurpose`: 사용자 아바타, 프로젝트 썸네일, 프로젝트 설명 속 인라인, 포스트 본문 용도
- `MediaStatus`: 업로드·처리 생명주기와 허용 상태 전이
- `MediaUploadPolicy`: 이미지 업로드 정책의 계약
- `DefaultMediaUploadPolicy`: 초기 허용 Content-Type과 최대 크기를 검증하는 기본 구현체

업로드 유스케이스는 `MediaUploadPolicy` 인터페이스에 의존한다. 현재는 `DefaultMediaUploadPolicy`를 사용하며, 정책 변경 시 환경 설정이나 정적 메서드를 직접 수정하는 대신 구현체를 교체한다. 정책값을 설정 파일이나 DB로 외부화하는 것은 현재 범위에 포함하지 않는다.

### 7. 초기 이미지 처리 실행 경계

- 초기에는 이미지 처리 전용 서버를 별도 서비스로 운영하지 않는다.
- 완료 API가 `PROCESSING`으로 저장한 뒤 트랜잭션 커밋 이벤트를 발행하고, 백엔드의 비동기 작업이 파일 시그니처 검증, EXIF 제거, 변형본 생성을 수행한다.
- 이미지 처리 워커는 S3에서 바이트를 읽어 `Content-Type`이 아니라 파일 시그니처를 기준으로 JPEG·PNG·WebP 여부를 검증한다. 업로드 완료 API의 `HeadObject` 검증은 객체 존재·크기·S3 메타데이터 확인만 담당하므로, 실제 파일 내용 검증을 대체하지 않는다.
- 처리 성공 시 업로드 원본 키에는 EXIF가 제거된 원본을 다시 저장하고, 같은 자산의 `{s3_key}/display`, `{s3_key}/thumbnail` 키에 표시용·썸네일을 저장한다. 두 변형본의 키는 `s3_key`로부터 결정적으로 계산하며 별도 변형본 테이블은 만들지 않는다.
- 표시용 이미지는 긴 변을 최대 1,920px, 썸네일은 긴 변을 최대 320px로 제한하고 원본보다 확대하지 않는다. 출력 포맷은 검증된 원본의 MIME 타입을 유지한다.
- 처리 실패 시 생성된 객체를 best-effort로 삭제하고 안전한 실패 사유를 `failure_reason`에 기록한 뒤 `FAILED`로 전환한다.
- 이미지 처리 작업의 동시성은 전용 `mediaProcessingExecutor`의 작업 스레드 2개와 대기열 100개로 제한한다. 대기열이 가득 차면 호출 스레드가 작업을 수행해 작업을 버리지 않고 백프레셔를 적용한다.
- 처리 대기열, CPU·메모리 사용량, 처리 시간과 실패율을 운영 지표로 수집한다.
- 처리 대기열이 지속적으로 쌓이거나 백엔드의 응답 시간·자원 사용량이 정한 기준을 넘으면, 이미지 처리 워커 또는 전용 서버 분리를 재검토한다.

### 8. 제공 S3 인프라 연동

- AWS 계정, 버킷, 버킷 정책, CORS와 IAM 역할은 우테코가 제공하고 관리하는 인프라를 사용한다.
- 이 저장소는 버킷을 생성·수정·삭제하는 IaC를 포함하지 않는다.
- 백엔드는 제공받은 버킷 이름과 리전, 실행 역할 또는 자격 증명 체인을 환경별 설정으로 주입받는다.
- 장기 보관용 AWS 액세스 키나 S3 자격 증명을 저장소·프론트엔드에 저장하지 않는다.

### 9. 백엔드 AWS SDK 및 환경 설정

- AWS SDK for Java 2.x의 BOM과 S3 모듈을 사용한다.
- 백엔드는 `S3Client`와 `S3Presigner`를 Spring Bean으로 생성해 애플리케이션 생명주기 동안 재사용한다.
- `AWS_S3_BUCKET`으로 제공받은 버킷을 지정하고, `AWS_REGION`으로 S3 클라이언트가 사용할 리전을 지정한다.
- Presigned PUT, GET URL의 만료 시간은 `AWS_S3_PRESIGNED_URL_EXPIRATION_SECONDS`로 받는다. 기본 값은 별도 인수인계한다.
- 로컬과 운영은 동일한 `DefaultCredentialsProvider`를 사용한다. 로컬은 AWS CLI Profile 또는 환경변수, 운영은 실행 환경에 연결된 IAM Role과 같은 AWS SDK 기본 자격 증명 체인이 선택한 출처를 사용한다.
- AWS 액세스 키와 시크릿 키를 애플리케이션 설정 파일, 저장소, 프론트엔드에 기록하지 않는다.

### 10. 미디어 메타데이터 테이블

- 서비스 전체 미디어의 저장·처리 메타데이터를 저장하는 `media_metadata` 신규 테이블을 사용한다.
- 프로젝트 썸네일과 사용자 프로필 이미지는 각 도메인 테이블이 `media_metadata.id`를 직접 참조하는 필드로 관리하고, 프로젝트 본문 인라인 이미지는 Markdown 본문에 `media://{mediaId}` 형식의 내부 미디어 참조로 저장한다. 따라서 프로젝트·사용자 프로필용 별도 미디어 매핑 테이블은 만들지 않는다.
- `V20260831162619__init_schema.sql`에 프로젝트·포스트·사용자 프로필 스키마가 정의되어 있으므로, 미디어 관련 테이블은 그 다음 버전인 `V20260901000000__create_media_schema.sql` 하나에서 생성한다.
- `media_metadata`의 주요 컬럼은 다음과 같이 정의한다.

| 컬럼 | 타입 | 의미 | 제약 및 사용 기준 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 미디어 자산 식별자 | PK, PostgreSQL identity가 생성하며 Java에서는 `Long`으로 매핑 |
| `uploaded_by` | `BIGINT` | 업로드를 시작한 사용자 | `users.id` 참조, 새 업로드에서는 필수이며 완료 요청자의 소유권 확인에 사용 |
| `purpose` | `VARCHAR(32)` | 업로드 용도 | `MediaPurpose` 값만 허용 |
| `s3_key` | `VARCHAR(1024)` | S3 원본 객체 키 | 필수, unique, 클라이언트가 임의로 결정하지 않음, DB ID와 별도로 랜덤하게 생성하며 처리 성공 후에도 정제된 원본이 같은 키를 사용 |
| `original_filename` | `VARCHAR(255)` | 사용자가 업로드한 원본 파일명 | 선택값, 표시·운영 목적에만 사용 |
| `mime_type` | `VARCHAR(100)` | 업로드를 요청한 MIME 타입 | 필수, 업로드 완료 후 S3 메타데이터와 실제 파일 시그니처를 검증 |
| `size_bytes` | `BIGINT` | 현재 S3 원본 객체 크기(바이트) | 0보다 커야 함, 완료 API에서는 업로드 객체 크기를 기록하고 `READY` 전환 시 EXIF 제거 후 원본 크기로 갱신, 최대 업로드 크기는 `MediaUploadPolicy`가 검증 |
| `status` | `VARCHAR(20)` | 업로드·처리 상태 | `PENDING_UPLOAD`, `PROCESSING`, `READY`, `FAILED`, `EXPIRED` |
| `expires_at` | `TIMESTAMPTZ` | 업로드 완료 확인 기한 | `PENDING_UPLOAD` 정리 기준 |
| `failure_reason` | `TEXT` | 실패 사유 | `FAILED`일 때만 비어 있지 않은 값 허용 |
| `uploaded_at` | `TIMESTAMPTZ` | S3 객체 존재를 확인한 시각 | 완료 API의 `HeadObject` 확인 시 기록 |
| `created_at` | `TIMESTAMPTZ` | 레코드 생성 시각 | UTC 기준 |
| `updated_at` | `TIMESTAMPTZ` | 마지막 변경 시각 | 상태 변경 시 갱신 |

- S3 객체 키의 중복을 방지하기 위해 `s3_key`에 unique 제약을 두고, 객체가 `media/` prefix 밖에 생성되지 않도록 제한한다.
- 업로드 직후에는 `s3_key`에 클라이언트가 올린 객체가 있고, 처리 성공 후에는 같은 키에 EXIF가 제거된 정제 원본이 있다. 재인코딩에 따라 파일 크기가 달라질 수 있으므로 `size_bytes`는 `READY` 시점의 정제 원본 크기로 갱신한다.
- 미디어 DB 식별자는 기존 도메인과 일관되게 `BIGINT identity`와 Java `Long`을 사용한다. ID가 API에 노출되더라도 접근 권한 검증을 우회할 수 없으며, S3 key는 별도의 랜덤 값으로 생성해 DB ID와 저장 객체의 추측 가능성을 분리한다.
- 만료 파일 정리를 위해 `PENDING_UPLOAD` 상태의 `expires_at` 부분 인덱스를 둔다.
- MIME 타입별 허용 목록과 최대 파일 크기는 정책 변경 시 구현체를 교체할 수 있도록 DB CHECK 제약으로 고정하지 않는다. DB에는 양수 크기와 상태·용도 값의 유효성만 둔다.
- `failure_reason`에는 원본 예외나 자격 증명 등 민감한 정보를 그대로 저장하지 않고, 운영에 필요한 안전한 메시지만 저장한다.
- Java 도메인 모델은 `MediaMetadata`로 정의하며, 시간 표현은 서버·DB의 타임존 혼동을 줄이기 위해 `Instant`를 사용한다.
- private S3 접근은 백엔드가 발급하는 Presigned GET URL로 제어하므로 `access_token`을 별도로 저장하지 않는다.
- `associated_id` 같은 다형성 외래키와 `display_order`는 참조 무결성을 보장할 수 없거나 사용처마다 값이 달라질 수 있으므로 미디어 메타데이터에 두지 않는다. 여러 미디어를 연결해야 하는 포스트에서는 `post_media`가 실제 외래키와 정렬 순서를 관리한다.
- 삭제 정책은 업로드·처리 상태와 별개의 정책이므로 이번 마이그레이션에서는 `is_deleted`를 추가하지 않는다. 사용자 삭제 요구가 확정되면 이력 보존이 가능한 `deleted_at` 방식으로 별도 결정한다.
- 업로더 식별자는 `uploaded_by`로 저장하고 `users.id`를 참조한다. 기존 `media_metadata` 데이터와의 호환을 위해 마이그레이션에서는 nullable로 추가하지만, 새 업로드 생성 시 애플리케이션과 도메인에서 필수로 검증한다.
- 미디어 메타데이터, 업로더 정보, 프로젝트·프로필 직접 참조, 포스트 매핑과 관련 인덱스·제약은 `V20260901000000__create_media_schema.sql` 하나에서 최종 형태로 생성한다. 사용하지 않는 `PROJECT_MEDIA` 용도는 처음부터 포함하지 않는다.

### 11. 포스트 미디어 매핑

- 프로젝트 썸네일은 `projects.thumbnail_media_id`, 사용자 프로필 이미지는 `user_profiles.avatar_media_id`가 `media_metadata.id`를 직접 참조하도록 관리한다. 프로젝트 본문 인라인 이미지는 `projects.description_md`에 `media://{mediaId}` 내부 참조를 포함한 Markdown으로 저장한다.
- `projects.thumbnail_media_id`와 `user_profiles.avatar_media_id`는 미디어 메타데이터 삭제 시 참조를 `NULL`로 정리하도록 `ON DELETE SET NULL`을 적용한다.
- 포스트 본문은 여러 이미지를 포함할 수 있고 이미지별 연결 대상과 표시 순서를 관리해야 하므로 `post_media` 매핑 테이블만 추가한다.
- `post_media`는 `post_id`, `media_metadata_id`, `display_order`를 저장한다. 포스트 본문 이미지 여부는 `media_metadata.purpose = 'POST_CONTENT'`로 구분하며, 이 값은 `post_media`에 중복 저장하지 않는다.
- `post_media`에는 `posts.id`와 `media_metadata.id`에 대한 외래키를 두고, 포스트 삭제 시 매핑 행은 `ON DELETE CASCADE`로 삭제한다. 미디어 메타데이터와 S3 객체는 즉시 삭제하지 않고 별도 고아 미디어 정리 정책으로 처리한다.
- `post_media`에 연결하는 미디어는 `READY` 상태인지와 `POST_CONTENT` 용도인지 애플리케이션 서비스에서 검증한다. 이 조건은 두 테이블의 값을 함께 확인해야 하므로 DB CHECK만으로 처리하지 않는다.
- `associated_id`와 `associated_type`을 하나의 테이블에 저장하는 다형성 매핑은 사용하지 않는다. 포스트 연결은 명시적인 외래키로 보장한다.
- 초기 모델에서 기존 프로젝트 썸네일·사용자 프로필 URL 컬럼은 `V20260831162619__init_schema.sql`에 남겨 두되, `V20260901000000__create_media_schema.sql`에서 제거한다. 최종 스키마에서는 `thumbnail_media_id`, `avatar_media_id`로 `media_metadata.id`를 참조한다. Markdown 본문은 `media://{mediaId}` 참조를 사용하며, 본문 저장·렌더링 통합은 별도 단계로 진행한다.
- `post_media`와 조회용 인덱스는 `V20260901000000__create_media_schema.sql`에서 `media_metadata`와 함께 생성한다.

### 12. S3 연동 모듈

- `AwsS3Configuration`에서 `S3Client`와 `S3Presigner`를 각각 하나의 Spring Bean으로 생성하고, AWS SDK의 기본 자격 증명 체인을 사용한다.
- `MediaObjectKeyGenerator`는 `media/{purpose-kebab-case}/{UUID}` 형식의 업로드 원본 키를 생성한다. 원본 파일명과 DB ID를 키에 포함하지 않으며, 확장자는 키가 아니라 MIME 타입과 메타데이터로 관리한다. `generateVariant`는 이 키에 `/display`, `/thumbnail`을 붙여 파생 키를 결정적으로 생성한다.
- `S3MediaStorage.createPresignedUpload`는 `S3Presigner`로 Presigned PUT URL을 발급한다. `PutObjectRequest`에 MIME 타입을 서명하므로 프론트엔드는 응답으로 받은 `Content-Type`을 동일한 요청 헤더에 넣어야 한다. 파일 바이트는 백엔드가 직접 받지 않는다.
- `S3MediaStorage.createPresignedDownload`는 비공개 객체 조회용 Presigned GET URL을 발급한다. 객체 접근 권한과 `READY` 상태 검증은 호출하는 애플리케이션 서비스의 책임이며, S3 URL을 본문에 저장하지 않는다.
- `S3MediaStorage.headObject`는 `S3Client`로 객체 존재 여부와 크기·MIME 타입·ETag·수정 시각을 확인한다. `verifyUploadedObject`는 `media_metadata`의 예상 크기와 MIME 타입이 S3 HeadObject 결과와 일치하는지 검증한 뒤 처리 단계로 넘긴다.
- `S3MediaStorage.downloadObject`와 `putObject`는 백엔드 이미지 처리 워커가 S3 객체를 읽고 정제 원본·변형본을 저장할 때 사용한다. 클라이언트 업로드는 계속 Presigned PUT으로 수행하며, 처리 결과 저장만 백엔드가 직접 수행한다.
- `S3MediaStorage.deleteObject`는 `S3Client`의 DeleteObject를 사용하고, `media/` prefix 밖의 키는 거부한다. 이미 없는 객체를 삭제하는 경우는 S3의 멱등적 삭제 동작에 따라 성공으로 처리한다.
- S3 객체가 없으면 `S3ObjectNotFoundException`, 객체 메타데이터가 업로드 요청과 다르면 `S3ObjectValidationException`, SDK 호출 자체가 실패하면 `S3StorageException`으로 구분한다. `ImageSignatureValidator`와 `S3MediaImageProcessor`는 S3에서 바이트를 읽어 파일 시그니처 검증, EXIF 제거, 원본·표시용·썸네일 생성을 수행한다.

### 13. 업로드 시작 API

- 업로드 시작 엔드포인트는 `POST /api/v1/media/uploads`다.
- 요청은 `purpose`, `targetId`, `originalFileName`, `contentType`, `sizeBytes`를 받는다. `targetId`는 사용자 아바타에서는 `user_id`, 프로젝트 용도에서는 `project_id`, 포스트 본문에서는 `post_id`를 의미한다.
- 컨트롤러는 `Principal`이 없거나 principal 이름을 사용자 ID로 해석할 수 없으면 `401 Unauthorized`를 반환한다. 현재 인증 모듈이 아직 구현되지 않았으므로, 인증 어댑터는 `Principal.getName()`에 활성 `users.id`의 문자열을 제공해야 한다. 임시 사용자 ID 헤더나 요청 본문의 사용자 ID는 인증 수단으로 사용하지 않는다.
- `DatabaseMediaUploadAuthorizer`는 DB 관계를 기준으로 권한을 확인한다. 사용자 아바타는 본인 활성 계정만 허용하고, 프로젝트 썸네일·설명은 활성 프로젝트의 등록자 또는 프로젝트 멤버만 허용하며, 포스트 본문은 삭제되지 않은 포스트의 작성자만 허용한다.
- 권한 확인 후 `MediaUploadPolicy`로 Content-Type과 파일 크기를 검증한다. 현재 `DefaultMediaUploadPolicy`는 JPEG·PNG·WebP와 10MiB 이하만 허용한다.
- 검증을 통과하면 서버가 객체 키와 만료 시각을 생성하고 `media_metadata`에 `PENDING_UPLOAD` 레코드를 저장한다. 이후 `S3MediaStorage`가 Presigned PUT URL을 발급하고, 응답에 `mediaId`, 상태, URL, 만료 시각, 업로드에 사용할 Content-Type을 포함한다.
- 메타데이터 저장과 Presigned URL 발급은 하나의 트랜잭션 흐름으로 실행한다. URL 발급이 실패하면 예외를 전파해 미디어 레코드도 커밋하지 않는다. Presigned URL은 파일 업로드 자체가 아니므로, 프론트엔드는 URL 발급 성공 후 S3에 파일을 PUT해야 한다.
- 이 API는 파일 바이트를 받지 않으며, 프론트엔드는 S3 PUT 성공 후 별도 완료 API를 호출한다.

### 14. 업로드 완료 API

- 업로드 완료 엔드포인트는 `POST /api/v1/media/{mediaId}/complete`이며 요청 본문은 없다. 인증된 `Principal`의 사용자 ID와 `media_metadata.uploaded_by`가 일치해야 한다.
- 미디어가 존재하지 않으면 `404 Not Found`, 업로더가 다르면 `403 Forbidden`을 반환한다.
- 현재는 `PENDING_UPLOAD` 상태만 완료할 수 있다. `PROCESSING`, `READY`, `FAILED`, `EXPIRED` 상태에 대한 재요청은 상태를 다시 처리하지 않고 `409 Conflict`로 반환한다. 중복 요청 멱등 처리와 동시 요청 직렬화는 이번 범위에서 제외하며 후속 트러블슈팅 항목으로 남긴다.
- 완료 요청이 들어오면 `S3MediaStorage.verifyUploadedObject`가 `HeadObject`로 객체 존재 여부, 실제 크기, Content-Type을 확인한다. 객체가 아직 없으면 `409 Conflict`를 반환하고 `PENDING_UPLOAD`를 유지한다.
- 객체 크기 또는 Content-Type이 요청 메타데이터와 다르면 미디어를 `FAILED`로 저장하고 `422 Unprocessable Entity`를 반환한다. 실패 사유는 외부에 노출하지 않는 안전한 고정 메시지를 저장한다.
- 검증에 성공하면 `uploaded_at`을 기록하고 `MediaMetadata.confirmUpload`를 통해 `PROCESSING`으로 전환한 뒤 `MediaProcessingRequested` 이벤트를 발행한다. 응답에는 `mediaId`, `PROCESSING` 상태, 실제 업로드 크기, Content-Type, 확인 시각을 포함한다. 이벤트 리스너는 완료 트랜잭션 커밋 후 비동기로 처리하며, 처리 결과에 따라 `READY` 또는 `FAILED`를 저장한다.

### 15. 이미지 처리 파이프라인

처리 워커는 다음 순서로 동작한다.

```text
S3 원본 바이트 다운로드
→ 파일 시그니처와 선언 MIME 타입 일치 여부 확인
→ 이미지 디코딩 및 해상도 한도 확인
→ EXIF가 제거된 원본 재인코딩
→ 표시용(최대 1,920px)·썸네일(최대 320px) 리사이즈 및 재인코딩
→ 원본·표시용·썸네일을 S3에 저장
→ READY 및 정제 원본 크기 저장
```

- JPEG·PNG·WebP의 매직 넘버를 확인한 뒤 선언 MIME 타입과 다르면 처리하지 않는다. 따라서 완료 API에서 `Content-Type`이 일치했더라도 실제 바이트가 위조된 경우 `FAILED`로 전환할 수 있다.
- 이미지를 ImageIO로 디코딩한 뒤 기본 메타데이터 없이 다시 인코딩해 EXIF를 제거한다. WebP 읽기·쓰기를 위해 Apple Silicon과 x86 환경을 지원하는 `com.github.usefulness:webp-imageio` ImageIO 플러그인을 사용한다.
- 이미지의 한 변은 10,000px, 전체 픽셀 수는 4,000만 픽셀 이하만 처리한다. 파일 용량이 작아도 과도한 해상도로 디코딩 메모리를 소진하는 입력을 제한하기 위한 기준이다.
- 처리 중 어느 단계에서든 실패하면 원본·파생 객체 정리를 시도하고 `MediaMetadata.markFailed`로 `FAILED`를 저장한다. 처리 예외의 상세 내용이나 S3 자격 증명 정보는 `failure_reason`에 저장하지 않는다.

### 16. 이미지 조회 API

- 이미지 조회 엔드포인트는 `GET /api/v1/media/{mediaId}`다. `variant` 쿼리 파라미터로 `ORIGINAL`, `DISPLAY`, `THUMBNAIL` 중 하나를 선택할 수 있으며, 생략하면 `DISPLAY`를 사용한다.
- 조회 서비스는 미디어 메타데이터를 조회한 뒤 접근 권한을 먼저 확인하고, `READY` 상태인 경우에만 변형본의 S3 key를 계산해 Presigned GET URL을 발급한다. 권한이 없는 사용자가 미디어의 처리 상태를 확인하지 못하도록 권한 검증을 상태 검증보다 먼저 수행한다.
- 삭제되지 않은 포스트에 `post_media`로 연결된 `POST_CONTENT` 미디어, 승인되고 삭제되지 않은 프로젝트의 `thumbnail_media_id`로 연결된 썸네일, 삭제되지 않은 사용자의 `avatar_media_id`로 연결된 프로필 이미지는 비로그인 조회를 허용한다. 프로젝트 본문 인라인 이미지는 본문 안의 `media://{mediaId}`만으로 리소스 연결을 DB에서 검증할 수 없으므로, 별도 본문 조회 통합 전까지 업로더 본인만 조회할 수 있다.
- 응답에는 `mediaId`, `variant`, `downloadUrl`, `expiresAt`, `contentType`을 포함한다. `downloadUrl`은 만료되는 임시 URL이므로 DB나 본문에 저장하지 않는다.

예시:

```http
GET /api/v1/media/123?variant=THUMBNAIL
```

```json
{
  "mediaId": 123,
  "variant": "THUMBNAIL",
  "downloadUrl": "https://s3.example.com/...",
  "expiresAt": "2026-09-01T10:05:00Z",
  "contentType": "image/webp"
}
```

본문에는 다음처럼 미디어 ID만 저장한다. 본문을 응답할 때 클라이언트 또는 본문 변환 계층이 `media://123`을 미디어 조회 API로 해석해 임시 URL을 사용한다.

```markdown
![프로젝트 화면](media://123)
```

- 현재 코드베이스에는 게시글·프로젝트 본문 저장 서비스가 아직 없으므로, 본문에 `media://{mediaId}`를 기록하고 렌더링하는 통합은 해당 도메인 API 구현 시 적용한다.

## 결과 (Consequences)

### 긍정적 영향

- AWS 자격 증명과 S3 업로드 권한을 클라이언트에 노출하지 않는다.
- 애플리케이션 서버가 이미지 바이너리를 직접 중계하지 않아 서버 부담을 줄인다.
- S3 파일 접근과 서비스 리소스 권한을 분리할 수 있다.
- 만료·실패·처리 중 상태를 구분해 고아 파일과 장애를 추적할 수 있다.
- 이후 프로필 이미지, 프로젝트 이미지, 게시글 이미지가 같은 업로드 기준을 공유한다.

### 부정적 영향

- 프론트엔드가 S3 업로드 완료 후 백엔드 완료 API를 별도로 호출해야 한다.
- 백엔드가 S3 객체 검증과 Presigned GET URL 발급을 구현해야 한다.
- Presigned URL 만료 시간과 고아 파일 정리 정책을 운영해야 한다.
- private S3를 사용하면 공개 이미지도 조회 URL을 생성하거나 CDN을 구성해야 한다.

### 중립적 영향

- AWS 버킷·IAM·CORS의 기본 설정은 우테코 제공 인프라에 의존한다. 버킷 이름, 리전, 운영 origin, 백엔드 런타임 역할 연결은 제공받은 환경에서 확인한다.
- 백엔드는 AWS SDK와 환경 변수, 기본 자격 증명 체인으로 제공 S3에 연결한다. S3 객체 저장소 어댑터, 업로드 시작·완료 API, 이미지 처리 워커와 이미지 조회 HTTP API의 기본 흐름을 구현했으며, 완료 요청의 중복 멱등 처리와 본문 저장·렌더링 통합은 후속 단계에서 추가한다.
- `media_metadata`, 프로젝트·프로필 직접 참조와 `post_media`의 구조는 확정했지만, 게시글·프로젝트 본문 저장과 `media://{mediaId}` 렌더링 통합은 별도 단계로 진행한다.
- 문서·동영상·SVG 등의 지원이 필요해지면 허용 포맷과 처리 방식을 다시 검토한다.

## 검토한 대안 (Options Considered)

**대안 1: 백엔드가 파일을 직접 받아 S3에 저장**

권한과 검증을 한 요청에서 처리하기 쉽다. 그러나 이미지 파일이 커질수록 백엔드의 네트워크와 메모리 부담이 커지고, 파일 전송이 애플리케이션 처리량을 직접 제한한다. 기본 업로드 방식으로 선택하지 않았다.

**대안 2: S3 버킷 또는 객체를 공개하고 URL을 직접 저장**

조회 구현은 단순하지만 누구나 URL을 통해 파일에 접근할 수 있고, 업로드·삭제 권한을 안전하게 분리하기 어렵다. 비공개 버킷 기준을 유지하기 위해 선택하지 않았다.

**대안 3: Presigned URL을 본문에 직접 저장**

구현은 간단하지만 URL이 만료되면 본문 이미지가 깨진다. 본문에는 미디어 ID 또는 안정적인 내부 참조를 저장하기 위해 선택하지 않았다.

**대안 4: 백엔드가 일정 시간 후 S3를 확인**

업로드 완료 시점을 정확히 알 수 없고, 사용자가 페이지를 닫은 경우에도 불필요한 대기가 발생한다. 완료 API를 기본 신호로 사용하고 만료 정리를 보완책으로 두기 위해 선택하지 않았다.

**대안 5: 이미지 처리 전용 서버를 별도 구축**

S3는 이미지 파일을 저장하고 전송하는 저장소이며, 이미지 처리 전용 서버는 검증·리사이즈·변형본 생성을 실행하는 처리 계층이다. 따라서 둘은 서로 대체하는 선택지가 아니다. 이번 결정에서는 S3를 저장소로 사용하되, 처리 계층을 별도 서비스로 분리할지 여부를 함께 검토했다.

현재 프로토타입에서 확인되는 규모는 서비스 등록 크루 44명, 등록 서비스 55개, 누적 방문자 486명이다. 
현재 이미지 업로드 상한인 10MiB를 등록 크루 44명이 각 한 장씩 동시에 업로드한다고 단순 가정하면 원본 입력량은 약 440MiB다. 이 값은 메모리 사용량이 아니며, S3 직접 업로드와 제한된 처리 동시성으로 전체 파일을 백엔드 메모리에 적재하지 않도록 설계한다. 또한 전체 가입자 수와 실제 동시 업로드 수는 현재 수집하고 있지 않으므로, 별도 서버를 정당화할 정도의 처리량이 발생했다고 볼 근거도 없다.

전용 서버를 추가하면 배포 단위, 모니터링, 장애 복구, 인증·권한 경계와 운영 비용이 함께 늘어난다. 
현재와 같은 제한된 등록 규모에서는 백엔드의 비동기 처리 작업으로 시작하는 편이 운영 복잡도와 비용에 비해 충분하다. 따라서 초기에는 전용 이미지 처리 서버를 구축하지 않고, 처리 지표가 임계치를 넘을 때 워커 또는 별도 서버로 분리하기로 했다.
