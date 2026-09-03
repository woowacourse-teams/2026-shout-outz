# 디자인 규칙

## 디자인 토큰

UI를 구현할 때는 디자인 토큰을 사용한다.

컬러는 프로젝트에서 정의한 컬러 토큰을 사용하며, 토큰의 종류와 선택 기준은 [컬러 토큰](./color-token.md)을 따른다. 간격, 크기, 타이포그래피 등 나머지 디자인 토큰은 [Tailwind CSS 문서](https://tailwindcss.com/docs)를 참고한다.

임의의 arbitrary value 사용은 지양하며, 사용할 수 있는 디자인 토큰이 있다면 우선 사용한다.

### arbitrary value 예외

아래와 같은 경우에는 arbitrary value를 허용한다. 자세한 문법은 [Tailwind CSS arbitrary values 문서](https://tailwindcss.com/docs/adding-custom-styles#using-arbitrary-values)를 참고한다.

- 서드파티/자식 DOM 대응

  ```tsx
  <div className="[&_svg]:size-4 [&:not(:first-child)]:mt-2" />
  ```

- 계산식

  ```tsx
  <main className="h-[calc(100dvh-3.5rem)]" />
  ```

- 토큰화 대상이 아닌 1회성 그래픽 값

  ```tsx
  <div className="[clip-path:polygon(0_0,100%_0,100%_100%)]" />
  ```
