# ADR 005: Spring Data JPA를 기본 데이터 접근 방식으로 사용한다

## 상태 (Status)

수락됨 (Accepted)

## 컨텍스트 (Context)

Drop-it은 사용자, 프로젝트, 카테고리, 좋아요, 북마크, 댓글, 등록 권한처럼 서로 연관된 관계형 데이터를 다룬다. 특히 프로젝트를 탐색하는 카탈로그 서비스이므로 등록보다 목록·상세·검색·정렬과 같은 조회 기능이 반복적으로 사용되는 구조다. 따라서 서비스의 기본 데이터 접근 방식은 조회 흐름에서 함께 사용되는 연관 데이터를 일관되게 객체로 매핑하고, 일반적인 CRUD를 안정적으로 처리할 수 있어야 한다.

이를 기준으로 애플리케이션에서 데이터베이스로 접근하는 방식으로 JdbcTemplate, JPA, jOOQ를 검토했다. 조회가 많은 서비스에서 JPA를 사용하면 영속성 컨텍스트와 연관관계 매핑을 활용해 도메인 데이터를 일관되게 다룰 수 있지만, 연관관계 조회로 인한 N+1 문제나 불필요한 쿼리 실행이 발생할 수 있다. 또한 카탈로그 기능이 확장되면 카테고리, 태그, 키워드, 인기순·최신순과 같은 검색·정렬 조건이 함께 조합될 수 있다. 따라서 일반적인 관계형 데이터의 객체 매핑과 CRUD는 JPA로 처리하되, 조회 조건이 복잡해지거나 쿼리 실행 방식을 세밀하게 제어해야 하는 경우를 위한 별도의 접근 방식도 함께 정할 필요가 있었다.

### Spring Data JPA와 Hibernate를 선택한 이유

Drop-it은 엔티티 간 관계, 외래 키, 복합 유일성, 트랜잭션을 코드와 데이터베이스에서 함께 관리해야 한다. JPA는 객체와 관계형 데이터베이스를 매핑하는 표준 API이고, Hibernate는 JPA 구현체로 영속성 컨텍스트와 SQL 변환을 담당한다. Spring Data JPA는 Repository 추상화로 기본 CRUD와 반복적인 데이터 접근 코드를 줄인다([Spring Data JPA 공식 문서](https://docs.spring.io/spring-data/jpa/reference/jpa.html), [Hibernate ORM 공식 문서](https://hibernate.org/orm/quickly/)).

관계형 도메인을 운영하는 우아한형제들의 사례에서도 Spring Data JPA와 QueryDSL을 함께 사용한다([BROS 2.0 기술블로그](https://techblog.woowahan.com/2516/)). 이를 참고해 Drop-it은 기본 CRUD와 엔티티 매핑에는 JPA를 사용하고, 복잡한 조회나 PostgreSQL 특화 기능은 QueryDSL 또는 native SQL로 보완한다.

## 결정 (Decision)

Drop-it 백엔드는 Spring Data JPA와 Hibernate를 기본 데이터 접근 방식으로 사용한다. JPA는 엔티티 관계와 트랜잭션을 관리하는 기본 수단으로 사용하고, 데이터베이스의 외래 키와 복합 유일성은 PostgreSQL 제약 조건으로 함께 보장한다.

일반적인 관계형 데이터의 객체 매핑과 CRUD는 JPA로 처리한다. JPA만으로 표현하기 어려운 동적 검색·정렬 조건이 증가하면 QueryDSL을 조건부로 도입해 쿼리를 구성한다.

복잡한 집계, 배열·JSONB 조회, 원자적 카운터 갱신처럼 PostgreSQL 특화 기능이 필요한 경우에는 native SQL을 보완적으로 사용한다. N+1 문제나 쿼리 성능 문제가 발생하면 생성된 SQL과 실행 횟수를 확인한 뒤 조회 전략을 개선한다. JPA와 QueryDSL 또는 native SQL만으로 요구사항을 충족하기 어려운 경우에는 JdbcTemplate, jOOQ 등 다른 접근 방식을 별도로 검토한다.

실제 PostgreSQL과 동일한 환경을 사용하는 Testcontainers 통합 테스트로 쿼리 동작과 트랜잭션 정합성을 검증한다. JPA, QueryDSL, native SQL은 모든 기능에 일괄 적용하지 않고 기능의 조회 복잡도와 성능 요구사항에 따라 선택한다.

## 결과 (Consequences)

**긍정적 영향**: 반복적인 CRUD 코드를 줄이고 도메인 객체와 관계형 데이터를 일관된 방식으로 매핑할 수 있다. Spring Data 기반의 저장소 추상화를 사용할 수 있으며, 복잡한 조회에는 QueryDSL과 native SQL을 선택적으로 적용할 수 있다. Testcontainers로 실제 PostgreSQL과의 동작 차이를 줄인 통합 테스트를 작성할 수 있다.

**부정적 영향**: JPA의 영속성 컨텍스트, 지연 로딩, 연관관계 매핑을 이해해야 한다. 잘못된 조회 전략은 N+1이나 불필요한 데이터 조회로 이어질 수 있다. JPA와 QueryDSL 또는 다른 접근 방식을 혼용하면 데이터 접근 규칙과 테스트 범위가 복잡해질 수 있다.

**중립적 영향**: QueryDSL, native SQL, JdbcTemplate, jOOQ는 현재 모든 기능에 일괄 도입하지 않는다. 실제 검색·정렬 요구사항, PostgreSQL 특화 기능, 성능 문제가 확인될 때 도입 범위와 기준을 별도로 결정한다.

## 검토한 대안 (Options Considered)

**대안 1**: JdbcTemplate을 기본으로 사용한다. 실행되는 SQL을 직접 제어하기 쉽지만, 반복적인 매핑과 CRUD 코드를 직접 관리해야 하므로 기본 데이터 접근 방식으로 선택하지 않았다.

**대안 2**: jOOQ를 기본으로 사용한다. SQL 표현력과 타입 안전한 쿼리 작성에 장점이 있지만, 현재 요구사항의 기본 CRUD와 객체 매핑에는 JPA 기반 접근이 더 적합하다고 판단해 선택하지 않았다.

**대안 3**: JPA만 사용하고 복잡한 조회도 모두 JPA API로 처리한다. 기술 구성을 단순하게 유지할 수 있지만, 동적 검색·정렬 조건이 증가할 때 쿼리 표현력이 제한될 수 있어 QueryDSL을 조건부 대안으로 남겼다.
