/**
 * 공개 프로필 페이지.
 *
 * 상단 프로필과 탭(프로젝트·피드)으로 구성한다.
 *
 * handle(path param)과 탭(search param)은 `getRouteApi('/users/$handle')`로 이 안에서 직접 읽고,
 * 탭을 바꿀 때도 여기서 URL을 갱신한다. 라우트는 Suspense와 errorComponent 경계만 잡는다.
 *
 * 각 탭 목록은 첫 페이지만 조회한다. 다음 커서 처리는 나중에 더한다.
 */
export function UserProfilePage() {
  return null;
}
