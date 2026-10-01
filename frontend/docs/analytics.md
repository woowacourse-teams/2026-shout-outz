# 사용자 행동 수집

> 초안입니다. 수집 도구가 정해지기 전에 "무엇을 볼 것인가"를 먼저 정하기 위한 문서입니다.

## 원칙

1. **모든 버튼에 이벤트를 달지 않는다.** 답하고 싶은 질문이 있는 곳에만 단다.
2. **이벤트 이름은 타입으로 고정한다.** 문자열을 직접 넘기지 않는다.
3. **화면 코드는 수집 도구를 모른다.** 도구는 어댑터 뒤에 둔다.
4. **본문과 검색어는 보내지 않는다.** 사용자 식별자(`userId`, `handle`)는 보내되, 사용자가 쓴 내용은 파라미터에 넣지 않는다.

## 지금 단계

수집 도구(GA4 / PostHog / Clarity)를 아직 정하지 않았다. 그래서 다음 순서로 간다.

1. 이벤트 카탈로그와 포트(`AnalyticsClient`)를 정의하고, 개발용 콘솔 어댑터만 둔다.
2. 화면에 이벤트 호출을 심는다. 이 시점에는 외부 스크립트가 하나도 붙지 않는다.
3. 도구가 정해지면 어댑터 파일을 추가하고 측정 ID를 주입한다. 화면 코드는 바뀌지 않는다.

동의(consent) 배너는 받지 않기로 했다. 정책이 바뀌면 어댑터 앞단에서 차단하는 방식으로 붙인다.

## 구조

```
utils/analytics/
├─ types.ts           AnalyticsEvent, AnalyticsClient
├─ index.ts           어댑터를 묶어 내보내는 진입점
├─ console-adapter.ts 개발용(현재 기본값)
└─ (도구 확정 후) ga4-adapter.ts / posthog-adapter.ts / clarity-adapter.ts
```

```ts
export interface AnalyticsClient {
  pageView(path: string): void;
  track(event: AnalyticsEvent): void;
  identify(user: { userId: number; handle: string } | null): void; // 비로그인은 null
}
```

- **로그인 여부는 따로 알리지 않는다.** `identify`에 넘긴 값으로 판단해 모든 이벤트에 자동으로 붙는다.
- **`identify`는 값이 바뀔 때만 도구에 전달된다.** 같은 사용자로 여러 번 불러도 한 번만 나간다. 세션 조회가 화면마다 다시 일어나도 호출부가 신경 쓸 필요가 없다. 새로고침하면 상태가 초기화되므로 페이지 로드마다 한 번은 알려야 한다.
- **초기화는 클라이언트 진입점에서만** 한다(`ssg/client.tsx`). `ssg/render.tsx`는 Node에서 같은 트리를 그리므로, 렌더 중에 `window`를 만지면 SSG 빌드가 깨진다.
- **페이지 이동은 라우터 구독으로** 잡는다. SPA라서 최초 로딩 외의 이동은 도구가 자동으로 알지 못한다.
- **측정 ID는 `webpack.config.js`의 `EnvironmentPlugin`으로** 주입한다(`API_ORIGIN`과 같은 방식). 값이 없으면 콘솔 어댑터만 동작한다.

## 페이지뷰

경로에 값이 들어가는 화면은 **원본 대신 패턴을 보낸다.** 개인정보 때문이 아니라 집계 때문이다. 원본을 보내면 `/feeds/101`, `/feeds/102`가 서로 다른 페이지로 쌓여서 "피드 상세를 몇 번 봤나"를 셀 수 없다. 누가 봤는지는 `identify`로 따로 보낸다.

| 실제 경로       | 보낼 값                |
| --------------- | ---------------------- |
| `/users/woojin` | `/users/:handle`       |
| `/feeds/101`    | `/feeds/:feedId`       |
| `/projects/12`  | `/projects/:projectId` |
| `/news/3`       | `/news/:newsId`        |

쿼리 파라미터는 화면 상태를 나타내는 것(`tab`, `sort`, `type`)만 남기고 나머지는 제거한다.

**개별 콘텐츠 성과는 이벤트 파라미터로 본다.** 경로에서 사라진 식별자는 상세 진입 이벤트가 들고 있다.

```ts
analytics.pageView('/feeds/101'); // → /feeds/:feedId  (화면 단위 집계)
analytics.track({ name: 'feed_detail_opened', feedId: 101, from: 'home' }); // 어떤 피드인지
```

## 이벤트 카탈로그 (초안)

### 인증·가입

| 이벤트                   | 발생 시점               | 파라미터                      | 답하려는 질문                  |
| ------------------------ | ----------------------- | ----------------------------- | ------------------------------ |
| `login_started`          | GitHub 로그인 버튼 클릭 | `from` (경로 패턴)            | 어느 화면에서 로그인이 막히나  |
| `signup_submitted`       | 가입 폼 제출            | —                             | 로그인 후 가입까지 몇 %가 오나 |
| `signup_failed`          | 가입 실패 응답          | `reason` (서버 에러 코드)     | 어디서 가입이 깨지나           |
| `verification_requested` | 크루 인증 요청 제출     | `userType`, `track`, `cohort` | 인증 요청이 실제로 쓰이나      |

### 피드

| 이벤트                  | 발생 시점          | 파라미터                                    | 답하려는 질문                               |
| ----------------------- | ------------------ | ------------------------------------------- | ------------------------------------------- |
| `feed_create_started`   | 작성 화면 진입     | `from` (경로 패턴)                          | 작성 시작 대비 완료율                       |
| `feed_create_submitted` | 작성 성공          | `categoryCount`, `mediaCount`               | 이미지·카테고리를 실제로 쓰나               |
| `feed_create_failed`    | 작성 실패          | `reason`                                    | 작성이 어디서 깨지나                        |
| `feed_detail_opened`    | 피드 상세 진입     | `feedId`, `from` (`home`/`feeds`/`profile`) | 어느 경로로 피드를 읽나, 어떤 피드가 읽히나 |
| `feed_sort_changed`     | 최신순·인기순 전환 | `sort`, `surface` (`home`/`feeds`)          | 인기순이 필요한가                           |
| `comment_submitted`     | 댓글 작성 성공     | —                                           | 댓글이 실제로 달리나                        |

### 프로젝트

| 이벤트                     | 발생 시점      | 파라미터                                                                    | 답하려는 질문               |
| -------------------------- | -------------- | --------------------------------------------------------------------------- | --------------------------- |
| `project_create_started`   | 등록 화면 진입 | `from`                                                                      | 등록 시작 대비 완료율       |
| `project_create_submitted` | 등록 성공      | `cohort`, `techTagCount`, `memberCount`, `hasThumbnail`, `hasDeploymentUrl` | 어떤 항목이 실제로 채워지나 |
| `project_create_failed`    | 등록 실패      | `reason`, `invalidFields` (필드명 목록)                                     | 어느 입력에서 막히나        |
| `project_detail_opened`    | 상세 진입      | `projectId`, `from` (`projects`/`profile`/`home`)                           | 프로젝트를 어디서 발견하나  |
| `project_search_performed` | 검색 실행      | `keywordLength`, `resultCount`                                              | 검색이 결과를 주고 있나     |

`keywordLength`만 보내고 검색어 자체는 보내지 않는다. 검색어에 이름이 들어갈 수 있다.

### 소식·프로필·탐색

| 이벤트                | 발생 시점             | 파라미터                 | 답하려는 질문                            |
| --------------------- | --------------------- | ------------------------ | ---------------------------------------- |
| `news_filter_changed` | 전체·공지·이벤트 전환 | `type`                   | 공지와 이벤트 중 무엇을 보나             |
| `news_detail_opened`  | 소식 상세 진입        | `newsId`, `type`, `from` | 이벤트 공지가 읽히나, 어떤 소식이 읽히나 |
| `profile_tab_changed` | 프로필 탭 전환        | `tab`                    | 프로필에서 무엇을 보나                   |
| `hero_banner_clicked` | 홈 배너 클릭          | —                        | 배너가 클릭되나                          |

### 이동 경로

같은 화면에 들어가는 길이 여러 개다. 어느 길로 들어오는지 보려고 **입구마다 이벤트를 나눈다.**

| 이벤트                 | 발생 시점                                | 파라미터                                         | 답하려는 질문                    |
| ---------------------- | ---------------------------------------- | ------------------------------------------------ | -------------------------------- |
| `nav_tab_clicked`      | GNB 탭 클릭                              | `tab` (`home`/`feeds`/`projects`/`news`), `from` | 어떤 탭을 가장 많이 쓰나         |
| `section_more_clicked` | 홈의 "피드 전체보기", "소식 더보기" 클릭 | `target` (`feeds`/`news`)                        | 전체보기로 가나, GNB 탭으로 가나 |
| `card_clicked`         | 목록에서 카드 클릭                       | `target` (`feed`/`project`/`news`), `surface`    | 목록에서 상세로 얼마나 넘어가나  |

`nav_tab_clicked`의 `tab=feeds`와 `section_more_clicked`의 `target=feeds`를 비교하면 피드 목록에 들어오는 두 경로의 비중을 알 수 있다. `feed_detail_opened`의 `from`도 같은 목적이라, 상세로 가는 길(홈 카드 / 피드 목록 / 프로필)을 나눠 볼 수 있다.

## 공통 파라미터

모든 이벤트에 자동으로 붙인다. 호출부에서 넘기지 않는다.

| 이름         | 값                   |
| ------------ | -------------------- |
| `path`       | 현재 경로 패턴       |
| `isLoggedIn` | 세션 상태            |
| `viewport`   | `mobile` / `desktop` |

## 이름 규칙

- `{대상}_{동작}` 형태의 snake_case. 동작은 과거형(`submitted`, `opened`, `changed`).
- 성공한 행동만 `submitted`로 남기고, 실패는 `_failed`로 분리해 `reason`을 붙인다.
- 화면 이름이 아니라 **행동**을 이름에 쓴다. 화면은 `from`·`surface` 파라미터로 구분한다. 같은 행동이 여러 화면에 생겨도 이벤트가 늘지 않는다.

## 지금은 넣지 않는 것

| 항목                    | 이유                                                     |
| ----------------------- | -------------------------------------------------------- |
| 좋아요·북마크           | 버튼이 `disabled` 상태다. 기능이 붙으면 추가한다.        |
| 공유 버튼               | 클립보드 복사만 한다. 실제 공유로 이어졌는지 알 수 없다. |
| 스크롤 깊이, 체류 시간  | 세션 리플레이 도구로 보는 편이 낫다.                     |
| 피드·프로젝트 수정·삭제 | 서버 DB에 남아 거기서 확인할 수 있다.                    |

## 테스트

콘솔 어댑터 대신 **기록용 가짜 어댑터**를 넣어 검증한다. 도구를 목킹하지 않으므로 도구가 바뀌어도 테스트는 그대로다.

```ts
const events: AnalyticsEvent[] = [];
const fake: AnalyticsClient = { pageView: () => {}, identify: () => {}, track: (e) => events.push(e) };

// 제출 후
expect(events).toContainEqual({ name: 'project_create_submitted', cohort: 6, ... });
```

이벤트 수집은 사용자에게 보이는 동작이 아니므로, **핵심 전환 흐름(가입, 피드 작성, 프로젝트 등록)에만** 테스트를 둔다.

## 미정 사항

- **도구 선택**: PostHog는 이벤트 분석과 세션 리플레이를 모두 제공해서 GA4 + Clarity 조합을 대체할 수 있다. 조합을 고른다면 리플레이 도구가 중복되지 않게 정리가 필요하다.
- **autocapture 사용 여부**: PostHog를 쓰면 클릭·입력이 자동 수집돼 위 카탈로그 중 일부(`*_opened`, `*_changed`)는 수동으로 심지 않아도 된다.
- **feature flag**: 분석이 아니라 분기 로직이다. `AnalyticsClient`에 넣지 않고 별도 인터페이스로 둔다.
- **식별자 범위**: 지금은 `userId`와 `handle`을 함께 보낸다. 분석 도구에 사람을 특정할 수 있는 값이 쌓이므로, 외부 도구를 붙이는 시점에 한 번 다시 판단한다.
