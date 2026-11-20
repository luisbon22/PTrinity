# --- Etapa 1: compilar y probar (la imagen no se crea si la cobertura baja del 85 %) ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src/ src/
ARG OMITIR_PRUEBAS=false
RUN ./mvnw -q verify -DskipTests=${OMITIR_PRUEBAS} -Djacoco.skip=${OMITIR_PRUEBAS}

# --- Etapa 2: solo el JRE y el jar, con usuario sin privilegios ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 ptrinity
COPY --from=build /app/target/ptrinity.jar app.jar
USER ptrinity
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health HTTP/1.0\r\n\r\n" >&3 && grep -q "UP" <&3'
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
