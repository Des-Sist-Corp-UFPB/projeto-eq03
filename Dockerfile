# Estágio 1: Build do Frontend (React/Vite)
FROM node:22-alpine AS frontend-build
WORKDIR /app/frontend

COPY salon-front/package*.json ./
RUN npm ci

COPY salon-front/ .
# Como o Nginx foi removido e tudo rodará no mesmo domínio e porta (Spring Boot), a URL da API é apenas o caminho relativo
ARG VITE_API_URL=/v1
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# Estágio 2: Build do Backend (Spring Boot) integrando o Frontend
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-build
WORKDIR /app/backend

COPY salon-back/pom.xml .
RUN mvn dependency:go-offline -B

COPY salon-back/src ./src

# Copia os assets do React já embutindo-os nos recursos estáticos do Spring Boot
RUN mkdir -p src/main/resources/static
COPY --from=frontend-build /app/frontend/dist src/main/resources/static/

RUN mvn package -DskipTests

# Estágio 3: Ambiente Final de Execução (Single Image)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o artefato único
COPY --from=backend-build /app/backend/target/salon-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
