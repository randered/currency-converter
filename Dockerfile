# ---- Stage 1: build with Maven + JDK 21 ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the wrapper and pom first to leverage Docker layer caching for deps.
COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml .
RUN ./mvnw -q dependency:go-offline -B

# Copy sources and package.
COPY src src
RUN ./mvnw -q package -DskipTests -B

# ---- Stage 2: minimal JRE runtime as a non-root user ----
FROM eclipse-temurin:21-jre
RUN useradd --create-home --shell /usr/sbin/nologin appuser
USER appuser
WORKDIR /app

COPY --from=build /build/target/currency-converter-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
