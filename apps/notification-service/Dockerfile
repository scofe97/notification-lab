# notification-service 컨테이너 이미지 — kind 클러스터 배치용
#
# 정본: ~/runners-high/write/06_observability/book/mastering_prometheus/
#       05-01.Alertmanager — 라우팅·그룹핑·억제·HA.md
#
# compose 로 띄운 앱은 클러스터 밖이라 ServiceMonitor 대상이 못 된다 — 그래서 파드로 띄운다.
# ⚠️ 독립 Gradle 프로젝트다. 루트 gradlew 가 아니라 이 디렉토리가 build context.
# ⚠️ kind 는 레지스트리가 없어 kind load 로 넣는다 → imagePullPolicy: IfNotPresent
#    필요. :latest 는 정책 기본이 Always 라 태그를 :lab 으로 고정했다.
#
# 빌드: docker build -t notification-service:lab apps/notification-service

# ── 빌드 스테이지 (toolchain 이 Java 21) ──────────────────────
FROM eclipse-temurin:21-jdk AS build

WORKDIR /src

# 빌드 스크립트를 먼저 복사해 의존성 해석을 캐시 레이어로 굳힌다
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./

RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 소스는 마지막에 — 자주 바뀌는 레이어
COPY src ./src

# -x test: 실습 배치용 이미지라 테스트는 호스트에서 따로 돌린다
RUN ./gradlew --no-daemon clean bootJar -x test

# ── 런타임 스테이지 (JRE 만) ──────────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

# non-root 로 돌린다. UC-5 아카이브가 ./archive 에 쓰므로 소유권을 준다
RUN useradd --system --uid 10001 --create-home appuser \
    && mkdir -p /app/archive \
    && chown -R appuser:appuser /app

COPY --from=build --chown=appuser:appuser /src/build/libs/*.jar /app/app.jar

USER appuser

EXPOSE 8092   # application.yml 의 server.port

# exec 형식 — PID 1 이 java 라야 SIGTERM 이 전달돼 graceful shutdown 이 된다
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
