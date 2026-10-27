# ADR-016: 동일 세션의 OAuth 로그인 중복 요청 제어

## 상태

이슈 #524에서 확정한 단일 로그인 시도 정책을 반영한다. 작성일은 2026-10-27이다.

## 배경

기존 로그인 시작은 세션의 `oauthLoginAttempt`를 덮어쓰고, Callback은 외부 통신 전에 이 속성을 제거했다. 따라서 Callback A가 GitHub와 통신하는 동안 시작 B가 같은 세션에 새 시도를 저장할 수 있었다. A의 인증 또는 가입 대기 전환에서 Session ID가 회전하면 B의 후속 요청이 이전 ID로 도착해 세션을 찾지 못할 수 있다. 프론트의 중복 클릭 차단만으로는 여러 탭과 직접 요청까지 제어하지 못한다.

운영 첨부 로그에서는 같은 JVM에서 Callback 처리 중 두 번째 시작과 후속 400을 관측했다. 원본에 Cookie 헤더는 없어 당시 ID 전달은 확인하지 못했다. 별도 로컬 브라우저와 Tomcat, 가짜 Provider 실험에서는 회전 후 이전 ID를 전송하는 경로를 재현했다. 이 결과는 실제 GitHub 연동이나 운영 개선율 검증과 구분한다.

## 결정

같은 세션에는 유효한 OAuth 시도를 하나만 허용한다. `WAITING`은 Callback을 기다리는 상태, `PROCESSING`은 유효한 Callback 하나가 처리 권한을 획득한 상태다. 두 상태 모두 새로운 시작을 `409 OAUTH_LOGIN_IN_PROGRESS`로 거절하며 기존 state와 PKCE verifier를 유지한다. PROCESSING 상태의 같은 Callback 재전송도 409로 거절한다.

시도 확인과 상태 변경은 같은 세션 모니터에서 수행한다. GitHub HTTP 통신과 DB 처리는 잠금 밖에 둔다. 세션 완료 직전에는 현재 시도가 해당 Callback의 시도인지, PROCESSING 상태인지, 만료되지 않았는지 확인하고 같은 잠금 안에서 ID 회전, 인증 또는 가입 대기 정보 저장, 시도 정리를 수행한다.

성공과 동의 거절, 처리 실패는 해당 시도만 정리한다. state 불일치는 `400 OAUTH_STATE_MISMATCH`로 거절하고 기존 시도를 보존한다. 생성 후 5분을 초과하면 `400 OAUTH_LOGIN_ATTEMPT_EXPIRED`로 구분하며 새 시작으로 교체할 수 있다. 만료된 속성이 남아 있어도 활성 시도로 보지 않는다. 늦은 이전 Callback의 완료와 정리는 새 시도에 영향을 주지 않는다.

Session ID 회전과 state, PKCE 검증은 유지한다. 중복 시작 409 응답에는 GitHub 리다이렉트를 주지 않는다. 프론트는 기존의 즉시 중복 내비게이션 차단을 유지한다.

## 검토한 대안

- 프론트 차단만 유지: 변경이 작지만 서버의 덮어쓰기와 중복 Callback 정책이 남는다.
- 세션별 단일 시도 상태 관리: 기존 저장 구조를 유지하면서 서버의 중복 처리와 늦은 완료를 제어한다. 이번 결정이다.
- state별 병렬 OAuth 저장소: 동시 성공 결과 선택과 브라우저 바인딩, 만료, 일회성 소비까지 별도 정책이 필요하다. Map이나 Redis 도입만으로 ID 회전 경쟁이 해결되지는 않는다.

## 결과와 한계

단일 세션의 진행 중인 시도를 덮어쓰지 않으며 외부 통신 동안 세션 잠금을 점유하지 않는다. Clock 기반 단위 테스트와 latch로 외부 통신 시점을 고정한 실제 HTTP 테스트로 중복, 만료, 재시작과 늦은 정리를 검증한다.

이 동기화는 현재 동일 JVM의 동일 세션에 한정된다. 공유 세션이나 다중 노드를 도입하면 상태 전이의 분산 원자성을 다시 설계한다. 이미 브라우저가 취소한 응답의 새 쿠키를 복구하지 않으며, 이전 ID를 인증 세션으로 다시 연결하지 않는다. 늦은 Callback의 세션 수립은 차단하지만 그 전에 완료된 DB 로그인 시각 갱신을 되돌리지는 않는다.

기존 쿠키 없이 동시에 들어온 요청이 서로 다른 세션을 생성한 경우에는 동일 세션 제어가 적용되지 않는다. 프론트 차단을 서버 제어로 대체했다고 해석하지 않는다.

## 관련 근거

- [이슈 #524](https://github.com/woowacourse-teams/2026-shout-outz/issues/524)
- `OAuthLoginHttpApi`, `AuthSessionAccessor`, `AuthSessionManager`, `OAuthLoginAttempt`
- `AuthSessionAccessorTest`, `OAuthLoginHttpApiTest`, `OAuthLoginAcceptanceTest`, `AuthApiDocumentationTest`
