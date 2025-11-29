#
# Dockerfile for StingrayTV Alice
#
# Features:
# - Multi-stage сборка (build + runtime)
# - Пропуск тестов при сборке (по умолчанию)
# - Кэширование зависимостей Gradle
#
# Требования:
# - Docker 20.10+
#
# Использование:
# docker build -t stingraytv-alice .
#
ARG BUILD_HOME=/build

#
# Gradle image for the build stage.
#
FROM eclipse-temurin:21-jdk-alpine AS build-image

#
# Set the working directory.
#
ARG BUILD_HOME
ENV APP_HOME=$BUILD_HOME
WORKDIR $APP_HOME

#
# Copy only build files first to cache dependencies
#
COPY gradle $APP_HOME/gradle/
COPY gradlew $APP_HOME/
RUN ./gradlew --no-daemon --version
COPY settings.gradle build.gradle $APP_HOME/

# Download dependencies first (cached unless build.gradle changes)
RUN ./gradlew dependencies --no-daemon

# Copy source code after dependencies are cached
COPY src/ $APP_HOME/src/

#
# Build the specified service
#
RUN ./gradlew :build --no-daemon -x test;

#
# Java image for the application to run in.
#
FROM eclipse-temurin:21-jre-alpine

#
# Build arguments
#
ARG BUILD_HOME
ARG SERVICE_NAME
ENV APP_HOME=$BUILD_HOME

#
# Copy the jar file and name it app.jar
#
COPY --from=build-image $APP_HOME/build/libs/stingraytv-alice-1.0.jar app.jar

#
# The command to run when the container starts.
#
CMD ["java", "-jar", "app.jar"]