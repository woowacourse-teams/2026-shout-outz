# ============ 빌드 스테이지 ============
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# 의존성 정보만 먼저 복사 => 소스만 바뀌면 아래 RUN은 캐시 재사용
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 복사 후, 빌드
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# ============ 런타임 스테이지 ============
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --no-create-home --shell /usr/sbin/nologin --uid 1001 appuser

COPY --from=builder --chown=appuser:appuser /workspace/build/libs/shout-outz-*.jar app.jar
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
