# Estágio 1: Compilação do Spring Boot JAR
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia pom.xml e baixa dependências offline para otimizar cache do Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia código-fonte e compila pulando os testes unitários (executados em CI)
COPY src ./src
RUN mvn package -DskipTests

# Estágio 2: Ambiente de Execução (JRE Leve)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o artefato compilado do estágio 1
COPY --from=build /app/target/salon-0.0.1-SNAPSHOT.jar app.jar

# Define portas e perfil de execução de produção por padrão
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
