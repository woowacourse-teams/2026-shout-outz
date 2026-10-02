-- 프로젝트 등록 시 검색해서 선택할 수 있는 기술 스택 초기 데이터
-- 관리자가 사전에 등록해 두고, 사용자는 검색으로만 선택한다.

INSERT INTO tech_tags (slug, display_name) VALUES
    -- 언어
    ('java', 'Java'),
    ('kotlin', 'Kotlin'),
    ('typescript', 'TypeScript'),
    ('javascript', 'JavaScript'),
    ('python', 'Python'),
    ('swift', 'Swift'),
    ('dart', 'Dart'),

    -- 백엔드 프레임워크
    ('spring-boot', 'Spring Boot'),
    ('spring-security', 'Spring Security'),
    ('spring-data-jpa', 'Spring Data JPA'),
    ('spring-batch', 'Spring Batch'),
    ('jpa-hibernate', 'JPA / Hibernate'),
    ('nodejs', 'Node.js'),
    ('express', 'Express'),
    ('nestjs', 'NestJS'),

    -- 프론트엔드
    ('react', 'React'),
    ('nextjs', 'Next.js'),
    ('vuejs', 'Vue.js'),
    ('svelte', 'Svelte'),
    ('redux', 'Redux'),
    ('recoil', 'Recoil'),
    ('zustand', 'Zustand'),
    ('react-query', 'React Query'),
    ('emotion', 'Emotion'),
    ('styled-components', 'styled-components'),
    ('tailwind-css', 'Tailwind CSS'),
    ('vite', 'Vite'),
    ('webpack', 'Webpack'),
    ('storybook', 'Storybook'),

    -- 모바일
    ('android', 'Android'),
    ('ios', 'iOS'),
    ('jetpack-compose', 'Jetpack Compose'),
    ('react-native', 'React Native'),
    ('flutter', 'Flutter'),

    -- 데이터베이스
    ('mysql', 'MySQL'),
    ('postgresql', 'PostgreSQL'),
    ('redis', 'Redis'),
    ('mongodb', 'MongoDB'),
    ('h2', 'H2'),
    ('elasticsearch', 'Elasticsearch'),

    -- 인프라 / DevOps
    ('aws', 'AWS'),
    ('docker', 'Docker'),
    ('kubernetes', 'Kubernetes'),
    ('nginx', 'Nginx'),
    ('github-actions', 'GitHub Actions'),
    ('jenkins', 'Jenkins'),
    ('terraform', 'Terraform'),
    ('vercel', 'Vercel'),

    -- 메시징 / 통신
    ('kafka', 'Kafka'),
    ('rabbitmq', 'RabbitMQ'),
    ('websocket', 'WebSocket'),
    ('graphql', 'GraphQL'),

    -- 모니터링
    ('grafana', 'Grafana'),
    ('prometheus', 'Prometheus'),
    ('sentry', 'Sentry'),
    ('loki', 'Loki'),

    -- 테스트
    ('junit', 'JUnit'),
    ('jest', 'Jest'),
    ('testing-library', 'Testing Library'),
    ('cypress', 'Cypress'),
    ('playwright', 'Playwright'),
    ('k6', 'k6'),

    -- 문서화 / 빌드
    ('swagger', 'Swagger'),
    ('spring-rest-docs', 'Spring REST Docs'),
    ('gradle', 'Gradle'),
    ('maven', 'Maven');
