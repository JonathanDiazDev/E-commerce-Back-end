# =============================================================
# Stage 1: Build
# =============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiamos el wrapper y el pom primero para aprovechar el cache de Docker.
# Si solo cambia código fuente (no dependencias), esta capa no se re-descarga.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Ahora copiamos el código fuente y compilamos
COPY src src
RUN ./mvnw package -DskipTests -B

# =============================================================
# Stage 2: Run
# =============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copiamos solo el JAR generado en el stage anterior
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]