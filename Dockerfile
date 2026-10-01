#
# GraalVM native image build for StingrayTV Alice.
#

ARG BUILD_HOME=/build

#
# Stage 1: Alpine-based GraalVM JDK with native-image tool
#
FROM ghcr.io/graalvm/native-image-community:25 AS build-image

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
# Build the native image
#
RUN ./gradlew :nativeCompile --no-daemon;

#
# Stage 2: distroless for the native binary to run in.
#
FROM gcr.io/distroless/base-debian13:nonroot

ARG BUILD_HOME
ARG SERVICE_NAME
ENV APP_HOME=$BUILD_HOME

#
# Copy the native executable
#
COPY --from=build-image $APP_HOME/build/native/nativeCompile/stingraytv-alice app

#
# The command to run when the container starts.
#
CMD ["app"]
