# ===== 1단계: 빌드 =====
# JDK(자바 개발도구)가 들어있는 이미지에서 우리 코드
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# 빌드에 필요한 파일들을 복사
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY src src

# gradlew 실행 권한 부여 (sed 는 윈도우 줄바꿈(CRLF) 대비)
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# 테스트는 빼고 JAR 파일을 만든다.
RUN ./gradlew bootJar -x test --no-daemon

# ===== 2단계: 실행 =====
# JRE(자바 실행기)만 있는 더 가벼운 이미지로 갈아탐
FROM eclipse-temurin:17-jre
WORKDIR /app

# 1단계에서 만든 JAR 파일만 가져옴
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]