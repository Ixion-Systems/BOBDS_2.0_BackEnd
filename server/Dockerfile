# Etapa 1: Construcción
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
# Compila el proyecto saltando los tests para mayor velocidad
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copia el .jar compilado desde la etapa anterior
COPY --from=builder /app/target/*.jar app.jar
# Directorio de datos
RUN mkdir data
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
