FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle gradle
COPY api-gateway/build.gradle api-gateway/
COPY auth-service/build.gradle auth-service/
COPY menu-service/build.gradle menu-service/
COPY order-service/build.gradle order-service/
COPY review-service/build.gradle review-service/
COPY api-gateway/src api-gateway/src
RUN chmod +x gradlew && ./gradlew :api-gateway:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/api-gateway/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
