/**
 * 경로 파라미터에서 인코딩하지 않을 문자.
 *
 * 프로젝트 상세 주소가 `/projects/@shout-outz`라, 기본 설정대로면 `@`가 `%40`으로 인코딩돼
 * `/projects/%40shout-outz`가 된다. RFC 3986에서 `@`는 경로 세그먼트에 그대로 쓸 수 있다.
 *
 * 라우터를 만드는 모든 곳(앱, 프리렌더, 테스트)이 같은 값을 써야 링크와 매칭이 어긋나지 않는다.
 */
export const PATH_PARAMS_ALLOWED_CHARACTERS = ['@'] as const;
