# API 타입

## 단일 출처

`src/api/generated/schema.ts`가 서버 응답·요청 타입의 유일한 출처다.

```
npm run generate:api   # 서버의 openapi3.yaml → src/api/generated/schema.ts
```

이 파일은 **손으로 고치지 않는다.** 재생성하면 통째로 덮어쓴다.

원본은 서버가 내려주는 `openapi3.yaml`이고, `generate:api`가 그 주소에서 직접 받아온다
(현재 `http://api.shout-ou.tz:8080`). 계약을 볼 때는 그 문서를 보고, 타입은 `schema.ts`만 쓴다.

한동안 루트에 있던 `openapi.yaml`은 피드를 `/api/v1/posts`로 적은 옛 문서였다. 지금은 없고,
경로는 `/api/v1/feeds`가 맞다.

## 흐름

```
src/api/generated/schema.ts   생성물. XxxSuccessResponse = { status, data, meta } 봉투
        ↓
src/types/api.ts              봉투를 벗겨 알맹이에 이름을 붙인다 (Data / Meta / Item)
        ↓
src/types/{feed,user,project,news}.ts   화면이 부르는 이름으로 다시 내보낸다
        ↓
src/api/*.ts, src/apis/*.ts   httpClient<XxxSuccessResponse>로 응답을 받는다
```

`src/types/api.ts`의 헬퍼 셋이 전부다.

```ts
type Data<T> = NonNullable<T['data']>; // 봉투에서 data
type Meta<T> = NonNullable<T['meta']>; // 봉투에서 meta
type Item<T> = T extends readonly (infer E)[] ? E : never; // 배열 원소
```

새 화면을 붙일 때는 `src/types/api.ts`에 한 줄 더하고 도메인 타입 파일에서 다시 내보낸다.
인터페이스를 손으로 적지 않는다.

**도메인 이름은 `src/types/*`에만 둔다.** `src/api/*.ts`와 `src/apis/*.ts`는 필요한 이름을
거기서 가져다 쓰고, 같은 타입에 자기만의 별칭을 다시 붙이지 않는다. 요청/응답 봉투는 생성된
`XxxSuccessResponse`를 `httpClient`의 제네릭에 바로 넣으면 되므로 중간 이름이 필요 없다.

```ts
// 이렇게 — 봉투는 생성 타입 그대로, 도메인 이름은 @/types에서
const response = await httpClient<CategoryFindAllSuccessResponse>('/api/v1/categories', …);

// 이러지 말 것 — 아무도 쓰지 않는 통과용 별칭이 쌓인다
export type Category = CategoryItem;
```

## 스키마에 없어 손으로 둔 것

한 군데다. 엔드포인트 자체가 `openapi3.yaml`에 없어서, 타입만 없는 게 아니라 계약이 아직
문서화되지 않은 상태다. 문서에 올라오면 파생 타입으로 바꾼다.

| 대상                                              | 위치               | 쓰는 곳                                     |
| ------------------------------------------------- | ------------------ | ------------------------------------------- |
| 미디어 업로드 3단계 (`/api/v1/media/*` presigned) | `src/api/media.ts` | `ThumbnailField` (프로젝트 썸네일, 홈 배너) |

관리자 프로젝트 심사(`/api/v1/admin/projects*`)는 명세에 올라와서 파생 타입으로 바꿨다
(`AdminProjectFindAllSuccessResponse`, `AdminProjectRejectRequest`). 가정했던 주소와 요청 본문은
실제와 같았고, 응답 봉투만 `{ items, nextCursor }`에서 `data` 배열 + `meta`로 달랐다.

다만 항목의 `members`와 `techTags`는 서버 문서가 원소 타입을 내보내지 않아
`(object | boolean | string | number)[]`로 생성된다. 화면이 읽는 필드만 `src/types/admin.ts`의
`AdminProject`에서 손으로 좁혀 두었다. 명세에 원소 타입이 붙으면 그 교차 타입을 지운다.

`GET /api/v1/home/statistics`는 명세에 올라와서 파생 타입으로 바꿨다(`HomeStatisticsSuccessResponse`).
손으로 적었던 네 필드와 명세가 정확히 같아 화면은 고칠 게 없었다.

쿼리 파라미터 enum(`FeedSort`, `NewsFilter` 등)도 스키마가 내보내지 않아 도메인 타입 파일에 남아 있다.

문서에 없어서 아직 화면을 붙일 수 없는 것도 적어 둔다.

- **좋아요·북마크 쓰기**: 리액션 엔드포인트가 명세에 없다. 읽기 쪽만 있다
  (`ProjectFindDetailSuccessResponse`의 `likeCount`·`likedByMe`·`bookmarkCount`·`bookmarkedByMe`).
  그래서 피드·프로젝트의 좋아요 버튼은 아직 `disabled`다.
- **프로필 활동 탭**: `/users/{handle}/activities`는 없다. 대신 본인 것만 보는
  `GET /api/v1/users/me/comments`가 있다(`UserCommentFindAllSuccessResponse`). 남의 프로필에서
  활동을 보여주는 디자인(📱9-C)은 이 API로는 만들 수 없다.

## 옮기면서 바뀐 계약

손으로 적었던 타입과 실제 스키마가 달랐던 곳이다. 화면은 스키마 쪽에 맞췄다.

| 대상                          | 전                                  | 후 (schema.ts)                      |
| ----------------------------- | ----------------------------------- | ----------------------------------- |
| 피드 `media[]`                | `{ mediaId?, displayOrder, url? }`  | `{ displayOrder, mediaId, url }`    |
| 댓글 `author`                 | `avatarImageId`                     | `avatarUrl`                         |
| 프로젝트 상세 썸네일          | `thumbnailUrl`                      | `imageUrl`                          |
| 프로젝트 등록 응답            | `{ id, approvalStatus, createdAt }` | `{ projectId, slug }`               |
| 크루 검색 결과                | `avatarImageId`                     | `avatarUrl`                         |
| 소식 목록 항목                | —                                   | `isPinned` 필수                     |
| 소식 상세 `cta/previous/next` | `T \| null`                         | `T \| undefined` (optional)         |
| 세션 `userId`, `role`         | `T \| null`                         | `T \| null \| undefined` (optional) |
| `GET /users/me` 응답          | 요약 + 기수·트랙만                  | 공개 프로필과 동일 (`UserProfile`)  |
| 프로젝트 상세 `members[]`     | `userId` 있음                       | `userId` 없음 (목록 응답에만 있다)  |

선택 필드가 `null`에서 `?: T | null`로 넓어진 곳이 많아, 표시 유틸과 컴포넌트 props도 `undefined`를
받도록 넓혔다(`formatCrewRole`, `formatCrewName`, `ProfileHeaderProps`, `ProjectCardProps`).
`=== null` 비교는 `== null`로 바꿨다.

## 홈 배너

`GET /api/v1/home/banners`는 이미지 URL과 이동 정보만 주고, 이동 방식이 두 갈래다.

| `destinationType` | 쓰는 필드                | 결과                                           |
| ----------------- | ------------------------ | ---------------------------------------------- |
| `TARGET`          | `targetType`, `targetId` | 내부 상세 경로 (`/news/7`, `/projects/7`)      |
| `URL`             | `linkType`, `linkUrl`    | `INTERNAL_PATH`면 내부, `EXTERNAL_URL`면 새 탭 |

이 판단은 `src/utils/home-banner.ts`의 `resolveHomeBannerLink` 한 곳에 모아 두고 단위 테스트로 덮었다.
`linkType`이 위 둘 중 하나가 아니면 링크를 만들지 않는다. 주소 모양으로 짐작하지 않는다.
`linkType`이 맞더라도 내부는 `/`로 시작해야 하고 외부는 `http(s)`여야 통과시킨다
(`javascript:`나 `//evil.example.com` 같은 주소가 그대로 렌더되지 않게).

두 가지는 명세가 주지 않아 화면에서 정했다.

- **대체 텍스트가 없다.** 배너 문구가 이미지 안에 그려져 있어 읽을 방법이 없다. 지금은 `"홈 배너"`라는
  공통 문구를 쓴다. 서버가 alt를 주면 그 값으로 바꾼다.
- **응답은 목록인데 디자인의 배너 자리는 하나다.** 표시 순서대로 온 목록에서 맨 앞 배너만 그린다.
  캐러셀은 디자인이 나오면 `HomeBannerSection`에서 붙인다.

## 돌아가게만 해 둔 것

지금은 동작하지만 제대로 된 처리가 아니다. **다음에 손볼 곳.**

1. **작성·수정 응답의 `media[]`에만 `mediaId`가 없다.**
   조회 응답 셋(`FeedFind`, `FeedFindAll`, `UserFeedFindAll`)에는 들어왔는데
   `FeedSaveSuccessResponse`·`FeedUpdateSuccessResponse`에는 아직 없다. 그 응답을 상세 캐시에
   그대로 넣으면 바로 이어서 수정할 때 첨부 ID를 잃으므로, `FeedForm`은 `setQueryData` 대신
   캐시를 무효화해 다시 읽게 한다. 서버가 채워 주면 `setQueryData`로 되돌려 요청 한 번을 아낄 수 있다.

2. **`FeedSaveRequest.categoryIds` / `mediaIds`를 손으로 좁혔다.** (`src/apis/feed.ts`의 `SaveFeedInput`)
   생성기가 배열 원소 타입을 못 읽어 `(object | boolean | string | number)[]`로 뽑는다. `number[]`로
   좁혀 쓴다. 서버 문서의 배열 스키마가 고쳐지면 `Omit`을 지운다.

3. **`members[]`의 `userId`가 응답마다 다르다.**
   프로젝트 목록에만 있고, 프로필 프로젝트 탭과 프로젝트 상세에는 없다. 그래서 React key를
   `handle`이나 인덱스로 만든다. `ProjectCard`는 세 응답의 교집합(`displayName`, `avatarUrl`)만 받는다.

4. **삭제한 것**: `src/components/feeds/FeedMedia.tsx`, `src/apis/media.ts`.
   `mediaId`로 이미지를 따로 조회하던 경로인데, 응답이 공개 URL을 바로 주면서 쓸 곳이 없어졌다.

## 이미지: mediaId와 URL을 함께 준다

수정은 전체 교체라 "기존 이미지를 그대로 둔다"를 표현하려면 ID가 필요한데, 조회 응답이 URL만 주면
그걸 만들 수 없었다. 서버가 조회 응답에 ID를 같이 넣어 주면서 풀렸다.

| 조회 응답                 | 이미지 ID                    | 공개 URL                 |
| ------------------------- | ---------------------------- | ------------------------ |
| 피드 `media[]`            | `mediaId`                    | `url`                    |
| 프로젝트 상세 썸네일      | `thumbnailImageId`           | `imageUrl`               |
| 프로젝트 상세 본문 이미지 | `descriptionMedia[].mediaId` | `descriptionMedia[].url` |
| 공개 프로필 아바타        | `avatarImageId`              | `avatarUrl`              |
| 홈 배너                   | `mediaId`                    | `imageUrl`               |

**화면은 URL로 그리고, 수정 요청에는 ID를 보낸다.** 피드 수정이 그 첫 사례다
(`FeedForm`이 `initialFeed.media`를 `displayOrder` 순으로 정렬해 `mediaId`만 뽑아 보낸다).

프로젝트 수정·프로필 수정 화면을 만들 때도 같은 규칙을 쓴다. 폼은 조회 응답이 준 ID를 그대로 들고
있다가 제출 때 보내고, 새로 올린 이미지는 업로드 1단계(`POST /api/v1/media/uploads`)가 준 ID로 바꾼다.
등록·수정의 프로젝트 썸네일 필드는 `thumbnailImageId`로 이름이 통일됐다.

## 덤으로 붙은 것

스키마에 `avatarUrl`과 `thumbnailUrl`이 있어 비어 있던 자리를 채웠다. 프로필 아바타, 댓글 작성자
아바타, 프로젝트 카드의 참여자 아바타와 썸네일이 이제 실제 이미지를 그린다.
