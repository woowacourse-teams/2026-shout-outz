# 디자인 규칙

## 디자인 토큰

UI를 구현할 때 색상, 간격, 크기, 타이포그래피 등은 프로젝트에 정의된 Tailwind 디자인 토큰을 우선 사용한다.

임의의 arbitrary value 사용은 지양하며, 근삿값인 Tailwind 디자인 토큰이 있다면 우선 사용한다.

### arbitrary value 예외

아래는 사유 없이 허용한다.

- 서드파티/자식 DOM 대응 — [&_svg]:size-4, [&:not(:first-child)]:mt-2
- 계산식 — h-[calc(100dvh-3.5rem)]
- 토큰화 대상이 아닌 1회성 그래픽 값 — clip-path, 특정 translate 보정
