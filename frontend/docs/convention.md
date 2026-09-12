# 프론트엔드 컨벤션

현재 프로젝트는 **React SPA + TanStack Router + TanStack Query**를 사용한다.

일반적인 React SPA 구조를 유지하면서 SSG는 가능한 한 얇은 별도 레이어로 구성하고, 향후 Next.js App Router로 이전할 때 변경 범위를 최소화할 수 있는 구조를 지향한다.

---

## 폴더 구조

```
src/
├─ routes/
├─ pages/
├─ components/
├─ hooks/
├─ apis/
└─ types/
```

### `routes`

라우팅 설정과 라우팅 컴포넌트를 관리한다.

TanStack Router에 종속된 처리는 가능한 한 이 경계에 둔다.

- path params 처리
- search params 처리
- Page 컴포넌트에 라우팅 값을 전달
- SSG 대상 여부와 정적 경로 생성 정보 정의

Page와 그 하위 컴포넌트에서는 가능한 한 TanStack Router를 직접 사용하지 않는다.

```tsx
export const Route = createFileRoute('/posts/$postId')({
  staticData: {
    prerender: true,
    generateStaticParams: async () => {
      const posts = await getPosts();

      return posts.map((post) => ({
        postId: post.id,
      }));
    },
  },
  component: PostRoute,
});

function PostRoute() {
  const { postId } = Route.useParams();
  const search = Route.useSearch();

  return (
    <ErrorBoundary>
      <Suspense fallback={<PostPageSkeleton />}>
        <PostPage postId={postId} page={search.page} />
      </Suspense>
    </ErrorBoundary>
  );
}
```

TanStack Router의 `loader`는 기본 데이터 로딩 방식으로 사용하지 않는다.

```tsx
// 사용하지 않는 기본 패턴
loader: ({ context, params }) => context.queryClient.ensureQueryData(getPostQuery(params.postId));
```

SSG 관련 정보는 Route의 `staticData`에 정의한다.

- `prerender`: 해당 Route를 SSG 대상으로 포함할지 여부
- `generateStaticParams`: 동적 Route를 prerender하기 위한 params 목록 생성

`prerender`와 `generateStaticParams`는 프로젝트의 SSG 빌드 레이어에서 사용하는 `staticData` 컨벤션이다.

SSG 빌드 레이어는 각 Route의 `staticData`를 수집해 prerender할 경로를 결정한다.

### `pages`

페이지 단위 UI와 페이지에 필요한 데이터 조합을 담당한다.

Route에서 전달받은 params, search params 등의 값을 사용하고 필요한 서버 데이터는 TanStack Query를 통해 가져온다.

```tsx
interface PostPageProps {
  postId: string;
  page: number;
}

export function PostPage({ postId, page }: PostPageProps) {
  const { data: post } = useSuspenseQuery(getPostQuery(postId));

  return (
    <main>
      <Post post={post} />
      <Comments postId={postId} page={page} />
    </main>
  );
}
```

### `components`

페이지를 구성하는 UI 컴포넌트를 관리한다.

### `hooks`

React hook을 관리한다.

### `apis`

HTTP 요청과 TanStack Query의 Query/Mutation Options를 관리한다.

```
apis/
├─ crew.ts
├─ post.ts
└─ comment.ts
```

API 파일은 다음 요소로 구성한다.

- TanStack Query와 독립적인 ky 기반 HTTP 요청 함수
- 조회를 위한 `queryOptions` 또는 변경을 위한 `mutationOptions`

각 요소는 필요한 곳에서 재사용할 수 있도록 export한다.

```tsx
export const getCrew = async (crewId: string) => {
  return api.get(`crews/${crewId}`).json<Crew>();
};

export const getCrewQuery = (crewId: string) =>
  queryOptions({
    queryKey: ['crews', crewId],
    queryFn: () => getCrew(crewId),
  });
```

```tsx
export const updateCrew = async (input: UpdateCrewInput) => {
  return api
    .patch(`crews/${input.crewId}`, {
      json: input.body,
    })
    .json<Crew>();
};

export const updateCrewMutation = mutationOptions({
  mutationFn: updateCrew,
});
```

컴포넌트에서는 정의된 Query/Mutation Options를 사용한다.

```tsx
const { data: crew } = useSuspenseQuery(getCrewQuery(crewId));

const mutation = useMutation(updateCrewMutation);
```

HTTP 요청 함수는 SSG 빌드나 Query/Mutation 외부에서 HTTP 요청 자체가 필요한 경우 직접 재사용할 수 있다.

### `types`

여러 영역에서 공통으로 사용하는 타입을 관리한다.

API 요청/응답 타입은 Swagger 등을 통해 생성된 타입을 우선 사용한다.

API 응답 타입과 도메인 타입의 분리 여부 및 수동 타입 정의 기준은 필요에 따라 결정한다.

---

## 공통 컴포넌트

공통 UI 컴포넌트는 shadcn/ui와 Radix UI를 직접 설치하거나 사용하지 않고 프로젝트에서 구현한다.

구현할 때는 shadcn/ui의 공식 문서와 공식 소스 코드를 레퍼런스로 사용한다.

- 공식 문서: https://ui.shadcn.com/docs/components
- 공식 저장소: https://github.com/shadcn-ui/ui
- 참고 소스: `apps/v4/registry/bases/radix/`

특히 다음 구현을 참고한다.

- variant, size 등의 컴포넌트 API와 스타일 구성 방식
- keyboard interaction, focus 관리, ARIA 속성 등의 접근성 처리
- 컴포넌트 API와 composition 구조

shadcn/ui와 Radix UI의 구현을 참고하되 프로젝트에 필요한 기능과 접근성만 최소한으로 직접 구현한다.

사용하지 않는 variant, 상태, interaction, API까지 미리 구현하지 않는다.

---

## 비동기 처리

데이터 조회로 인해 발생하는 로딩과 오류 처리는 가능한 한 `Suspense`와 `ErrorBoundary`로 처리한다.

페이지 단위의 비동기 처리는 라우팅 컴포넌트에서 경계를 구성한다.

```tsx
function PostRoute() {
  const { postId } = Route.useParams();

  return (
    <ErrorBoundary>
      <Suspense fallback={<PostPageSkeleton />}>
        <PostPage postId={postId} />
      </Suspense>
    </ErrorBoundary>
  );
}
```

데이터 조회는 Suspense 기반 TanStack Query API를 사용한다.

```tsx
function PostPage({ postId }: PostPageProps) {
  const { data: post } = useSuspenseQuery(getPostQuery(postId));

  return <Post post={post} />;
}
```

페이지 내부에서 다른 영역과 독립적으로 로딩할 수 있는 데이터가 있다면 해당 영역 가까이에 별도의 `Suspense`와 `ErrorBoundary`를 둘 수 있다.

```tsx
function PostPage({ postId }: PostPageProps) {
  const { data: post } = useSuspenseQuery(getPostQuery(postId));

  return (
    <main>
      <Post post={post} />

      <ErrorBoundary>
        <Suspense fallback={<CommentsSkeleton />}>
          <Comments postId={postId} />
        </Suspense>
      </ErrorBoundary>
    </main>
  );
}
```

개인화된 데이터나 자주 변경되는 데이터처럼 빌드 시점에 렌더링하기 적절하지 않은 영역은 SSG에서 제외하고 클라이언트에서 렌더링할 수 있다.
