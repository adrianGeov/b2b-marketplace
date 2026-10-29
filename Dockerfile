# =====================================================================
# Etapa 1: compilación (JDK + Maven Wrapper)
# =====================================================================
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

# 1) Solo el pom y el wrapper: las dependencias se cachean en una capa propia
#    y no se vuelven a descargar si solo cambia el código fuente
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
# Por si el archivo se editó en Windows (saltos de línea CRLF)
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# 2) Código fuente y empaquetado (las pruebas unitarias se ejecutan aquí)
COPY src src
ARG SKIP_TESTS=false
RUN ./mvnw -B package -DskipTests=${SKIP_TESTS}

# =====================================================================
# Etapa 2: imagen final (solo JRE + jar)
# =====================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario sin privilegios (no se ejecuta como root)
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /workspace/target/*.jar app.jar
USER app

EXPOSE 8080

# La JVM usa como máximo el 75% de la memoria asignada al contenedor
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]