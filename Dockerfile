# Multi-stage build for Render: CSS (Node) -> jar (JDK) -> small runtime image (JRE).

# 1) Compile Tailwind CSS. It scans templates and JS for class names, so copy those too.
FROM node:24-alpine AS css
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-fund --no-audit
COPY src/main/frontend src/main/frontend
COPY src/main/resources/templates src/main/resources/templates
COPY src/main/resources/static/js src/main/resources/static/js
RUN npm run build:css

# 2) Build the Spring Boot jar. Dependencies are downloaded in their own layer,
#    so they are cached until pom.xml changes. Tests run locally, not here.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q -B dependency:go-offline
COPY src src
COPY --from=css /app/src/main/resources/static/css/app.css src/main/resources/static/css/app.css
RUN ./mvnw -q -B package -DskipTests

# 3) Runtime: JRE only, running as a non-root user.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 repup
COPY --from=build /app/target/repup-0.0.1-SNAPSHOT.jar app.jar
USER repup
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
# Render's free instance has 512 MB; let the JVM use most of it for the heap.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
