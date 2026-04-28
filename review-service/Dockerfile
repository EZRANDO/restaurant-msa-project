FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle gradle
COPY api-gateway/build.gradle api-gateway/
COPY auth-service/build.gradle auth-service/
COPY menu-service/build.gradle menu-service/
COPY order-service/build.gradle order-service/
COPY review-service/build.gradle review-service/
COPY review-service/src review-service/src
RUN chmod +x gradlew && ./gradlew :review-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
VOLUME /data
COPY --from=build /workspace/review-service/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
