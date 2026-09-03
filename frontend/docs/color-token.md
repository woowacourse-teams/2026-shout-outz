# 컬러 토큰

## 기본 원칙

- 컴포넌트에서는 임의의 색상값보다 이 문서에 정의된 컬러 토큰을 우선 사용한다.
- 일반 컬러 토큰은 라이트·다크 모드에 맞춰 자동으로 변경된다.
- 테마와 관계없이 같은 색상이 필요할 때만 `static-*` 토큰을 사용한다.
- 의미가 분명한 경우 컬러 토큰의 별칭을 사용한다.
- 팔레트는 Tailwind의 50~950 단계별 명도 흐름을 기반으로 프로젝트에 맞게 보정한다.
- 같은 단계는 동일한 명도 수치가 아니라 비슷한 시각적 강조도를 의미한다. 색상별 지각 밝기를 보정하므로 Yellow처럼 본래 밝게 느껴지는 색상은 다른 색상보다 높은 명도를 유지한다.

팔레트의 생성 및 검증 방식은 [컬러 팔레트 생성 모델](./color-palette-model.md)을 따른다.

## 단계

팔레트는 다음 단계를 제공한다.

```text
50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950
```

## 팔레트

| 토큰 |
|---|
| `blue-*` |
| `static-blue-*` |
| `red-*` |
| `static-red-*` |
| `yellow-*` |
| `static-yellow-*` |
| `green-*` |
| `static-green-*` |
| `gray-*` |
| `gray-opacity-*` |
| `primary-*` — `blue-*` 컬러 토큰의 별칭 |
| `static-primary-*` — `static-blue-*` 컬러 토큰의 별칭 |

단일 색상으로 `background`, `white`, `black`을 제공한다. `background`는 테마에 맞춰 변경되며 `white`와 `black`은 고정된다.

## 선택 기준

- 주요 액션과 브랜드 강조에는 `primary-*`를 사용한다.
- 위험, 실패, 삭제처럼 부정적인 결과에도 `red-*`를 사용한다.
- 주의가 필요하지만 실패는 아닌 상태에도 `yellow-*`를 사용한다.
- 색상 자체를 표현해야 하는 경우에만 컬러 이름 토큰을 사용한다.
- 오버레이, 구분선처럼 배경과 자연스럽게 혼합되어야 하는 색상에는 `gray-opacity-*`를 사용한다.
- 로고, 썸네일 등 테마에 따라 색상이 바뀌면 안 되는 콘텐츠에는 `static-*`을 사용한다.

## 사용 예시

```tsx
<main className="bg-background text-gray-900">
    <button className="bg-primary-500 text-white hover:bg-primary-600">
        저장
    </button>

    <button className="text-red-600 hover:bg-red-50">
        삭제
    </button>
</main>
```

일반 토큰은 Tailwind 기본 `dark` variant에 따라 자동으로 변경되므로 같은 의미를 유지하기 위한 `dark:` 색상 재지정은 하지 않는다. 모드에 따라 색상의 의미 자체가 달라지는 예외적인 경우에만 `dark:` variant를 사용한다.
