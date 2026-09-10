# 프로젝트의 Java 21 도구 체인으로 Spring Boot 실행 JAR을 빌드한다.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# 의존성 관련 파일을 먼저 복사해 Docker 빌드 캐시를 활용한다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 변경 후 실행 가능한 JAR 파일을 생성한다.
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# 실행 이미지는 JRE만 포함해 크기를 줄이고, non-root 사용자로 구동한다.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S quickpass && adduser -S quickpass -G quickpass
COPY --from=build /workspace/build/libs/*.jar app.jar

USER quickpass
# Spring Boot 기본 HTTP 포트
EXPOSE 8080

# 컨테이너 시작 시 Spring Boot 애플리케이션을 실행한다.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
