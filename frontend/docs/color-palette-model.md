# 컬러 팔레트 생성 모델

## 확정 상태

현재 팔레트는 **V4 + 참조 팔레트에 Yellow 고정 오버라이드를 적용한 버전**이다.

- 참조 팔레트: 프로젝트에서 선택한 라이트·다크 팔레트
- 목표 스케일: Tailwind CSS `4.3.3`의 50~950
- 색 공간: OKLCH/OKLab
- 색상값: `src/styles/colors.css`
- Tailwind 연결: `src/styles/index.css`
- 컬러 블렌딩: 사용하지 않음

Blue·Red·Green은 V4 생성식을 그대로 사용한다. Yellow는 참조 팔레트의 라이트·다크 500을 직접 시드로 사용하고 나머지 단계를 고정값으로 관리한다. Gray와 Gray opacity는 유채색 생성 모델과 분리된 고정 중립 팔레트다.

## 기본 V4 생성 과정

Yellow를 포함한 참조 팔레트의 Blue·Red·Yellow·Green을 입력 집합으로 사용한다. Yellow도 통계 계산에는 포함해야 현재 Blue·Red·Green 결과가 재현된다.

```text
참조 팔레트를 OKLCH로 변환
→ 500 집합을 Tailwind 500 분포에 표준화
→ 50→500과 500→끝점의 L/C/H 경로 정규화
→ Tailwind의 누적 OKLab 거리 분포로 단계 재표본화
→ sRGB gamut 투영
→ Yellow 고정 오버라이드 적용
```

### 특성 좌표

```text
lightness = logit(OKLCH lightness)
chroma = log(OKLCH chroma + 0.0005)
hue = 500 기준 최단 원호 각도
```

명도와 채도는 극단 구간의 clipping과 절대량 편향을 줄이기 위해 각각 logit과 log 공간에서 계산한다. 색조는 500을 기준으로 한 상대 각도이며, 실제 거리 계산은 OKLab에서 수행한다.

### 500 시드 표준화

참조 팔레트의 각 500과 같은 색조 위치의 Tailwind 후보 500을 구한다. 참조 입력 집합의 명도와 로그 채도 표준점수를 Tailwind 후보 집합의 분포에 옮긴다.

```text
final500
= meanTailwind
+ zScore(reference500) × deviationTailwind
```

색조는 참조 팔레트 500의 값을 유지한다. Yellow 오버라이드를 적용하기 전 V4 산출 시드는 다음과 같다.

| 색상 | V4 산출 500 |
|---|---|
| Blue | `oklch(64.1% 0.1934 258.198)` |
| Red | `oklch(65.4% 0.2328 21.045)` |
| Yellow | `oklch(79.7% 0.1640 82.465)` |
| Green | `oklch(67.3% 0.1608 156.782)` |

### 경로와 단계 위치

각 색상의 50→500과 500→마지막 단계를 별도 분기로 다룬다. 참조 팔레트에서 500 대비 명도·채도·색조 변화 형태를 추출하고, 명도·채도의 절대 범위는 Tailwind 분포에 맞춘다. 색조 변화 범위는 참조 팔레트 특성을 유지한다.

생성된 연속 경로는 누적 OKLab 거리로 다시 샘플링한다.

```text
light 50→500:  0.000, 0.097, 0.255, 0.505, 0.778, 1.000
light 500→950: 0.000, 0.205, 0.383, 0.571, 0.729, 1.000
dark endpoint→500: 0.000, 0.193, 0.363, 0.538, 0.789, 1.000
```

다크 500에는 참조 팔레트의 라이트 500→다크 500 명도 이동, 채도 비율, 색조 이동을 적용한다. 나머지 다크 단계에는 참조 다크 팔레트의 정규화된 L/C/H 경로를 적용한다.

sRGB gamut을 벗어나면 명도와 색조는 고정하고 채도만 표시 가능한 최대값까지 줄인다.

## Yellow 고정 오버라이드

현재 Yellow는 V4의 순수 재생성 결과가 아니다. 코드에 남아 있던 `참조 500 시드를 사용한 노란색 팔레트` 규칙에 따라 다음 처리가 적용되어 있다.

1. 라이트 500은 참조 팔레트 원본 `#ffc342`를 OKLCH로 변환한 `oklch(85.0% 0.1539 82.465)`로 고정한다.
2. 다크 500은 참조 팔레트 원본 `#ffb134`를 OKLCH로 변환한 `oklch(81.6% 0.1580 73.705)`로 고정한다.
3. 나머지 단계는 두 시드를 중심으로 별도 확정된 고정 L/C/H 곡선을 사용한다. 이 값을 만든 추가 계산식은 저장소에 남아 있지 않으므로 값 자체를 명세로 취급한다.
4. static Yellow는 라이트 adaptive Yellow와 동일하다.

즉, V4 산출 Yellow 500인 `79.7%`를 계산식으로 `85.0%`까지 보정한 것이 아니다. 참조 팔레트 원본 시드로 교체한 뒤 전체 Yellow 스케일을 별도 고정한 예외 처리다. 따라서 아래 값 자체가 재현 기준이며, 추가 보간 공식을 추정해 적용하지 않는다.

| 단계 | Light | Dark |
|---:|---|---|
| 50 | `oklch(99% 0.012 93)` | `oklch(28% 0.060 70)` |
| 100 | `oklch(98% 0.025 92)` | `oklch(38% 0.090 70)` |
| 200 | `oklch(96% 0.050 91)` | `oklch(47% 0.120 71)` |
| 300 | `oklch(93% 0.085 89)` | `oklch(56% 0.150 72)` |
| 400 | `oklch(89% 0.125 86)` | `oklch(67% 0.170 73)` |
| 500 | `oklch(85.0% 0.1539 82.465)` | `oklch(81.6% 0.1580 73.705)` |
| 600 | `oklch(75% 0.150 80)` | `oklch(86% 0.140 76)` |
| 700 | `oklch(64% 0.140 78)` | `oklch(90% 0.110 79)` |
| 800 | `oklch(54% 0.125 77)` | `oklch(94% 0.075 81)` |
| 900 | `oklch(44% 0.105 76)` | `oklch(97% 0.040 83)` |
| 950 | `oklch(30% 0.075 74)` | `oklch(99% 0.015 85)` |

## 토큰 구성

- `colors.css`는 `--source-light-*`, `--source-dark-*` 원시 색상 변수만 가진다.
- `index.css`가 adaptive/static/primary 별칭과 Tailwind `@theme` 등록을 담당한다.
- Adaptive 토큰은 Tailwind `dark` variant에 따라 라이트·다크 값이 전환된다. 예시 컴포넌트의 수동 전환을 위해 Tailwind가 제공하는 class 기반 custom variant를 사용한다.
- Static 토큰은 테마와 무관하게 라이트 팔레트 값을 유지한다.
- `primary-*`와 `static-primary-*`는 각각 Blue와 Static Blue의 별칭이다.
- Tailwind 색상 namespace는 `@theme { --color-*: initial; }`로 초기화한 뒤 프로젝트 토큰만 등록한다.

## 재현 순서

1. 고정된 참조 팔레트와 Tailwind 입력으로 V4를 Blue·Red·Yellow·Green 전체에 실행한다.
2. 생성된 Blue·Red·Green을 사용한다.
3. Gray와 Gray opacity 고정값을 결합한다.
4. Yellow 라이트·다크 값을 위 표로 교체한다.
5. static 팔레트를 라이트 adaptive 팔레트와 동일하게 만든다.
6. `@theme inline`에서 Tailwind 유틸리티 이름과 토큰을 연결한다.

## 검증 조건

- 라이트 팔레트의 명도는 50→950 방향으로 단조 감소해야 한다.
- 다크 팔레트의 명도는 50→950 방향으로 단조 증가해야 한다.
- 반올림된 모든 유채색은 sRGB gamut 안에 있어야 한다.
- static 팔레트는 라이트 adaptive 팔레트와 정확히 같아야 한다.
- 다크 모드 분기는 `index.css`의 Tailwind `@variant dark`로 작성하고, selector나 media query를 직접 하드코딩하지 않는다.
