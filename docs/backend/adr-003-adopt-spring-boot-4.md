# ADR 003: Spring Boot 4를 백엔드 프레임워크로 채택한다

## 상태 (Status)

수락됨 (Accepted)

## 컨텍스트 (Context)

Drop-it은 운영환경에서 Spring 서버를 애플리케이션 및 API 계층으로 사용하기로 결정했다([ADR 001](./adr-001-introduce-spring-server.md)). 이에 따라 서버 애플리케이션의 구성과 실행을 담당할 Spring Boot 버전을 선택해야 한다.

신규 프로젝트이므로 기존 Spring Boot 버전과의 호환성을 유지해야 하는 레거시 제약이 없다. Spring Boot 3 계열을 유지할지, 지원 정책과 최신 생태계를 고려해 Spring Boot 4 계열을 사용할지 비교했다.

## 결정 (Decision)

Drop-it 백엔드는 Spring Boot 4 계열을 사용한다. 현재 기술 스택의 기준 버전은 Spring Boot 4.1.0이다.

Spring Boot 4의 세부 설정과 의존성 호환성은 프로젝트의 빌드 구성에서 관리한다. Java 21과 Java 25 중 최종 실행 버전을 선택하는 결정은 이 ADR의 범위에 포함하지 않는다.

## 결과 (Consequences)

**긍정적 영향**: 신규 프로젝트에 최신 Spring Boot 계열을 기준으로 애플리케이션을 구성할 수 있다. 향후 Spring 생태계의 기능과 지원 정책을 기준으로 개발 환경을 유지하기 쉽다. 레거시 호환성을 위해 이전 Spring Boot 버전을 유지해야 하는 부담을 줄일 수 있다.

**부정적 영향**: Spring Boot 3 계열과의 API·설정·의존성 차이를 확인해야 한다. 사용하는 라이브러리가 Spring Boot 4를 지원하는지 검증해야 하며, 팀이 새로운 버전의 변경 사항을 학습해야 한다.

**중립적 영향**: Java 실행 버전, 세부 Spring 모듈, 배포 환경은 별도의 결정 또는 프로젝트 설정으로 확정한다.

## 검토한 대안 (Options Considered)

**대안 1**: Spring Boot 3 계열을 사용한다. 기존 자료와 생태계 사례가 많다는 장점이 있지만, 신규 프로젝트에서 이전 메이저 버전을 선택하면 향후 지원 정책과 업그레이드 비용을 다시 고려해야 한다. Spring Boot 4를 기준으로 삼기 위해 선택하지 않았다.
