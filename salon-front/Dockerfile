# Estágio 1: Build da aplicação React
FROM node:22-alpine AS build
WORKDIR /app

# Copia arquivos de dependência e instala de forma limpa
COPY package*.json ./
RUN npm ci

# Copia todo o código fonte
COPY . .

# Argumentos e variáveis de ambiente de build para o Vite
ARG VITE_API_URL=/v1
ENV VITE_API_URL=$VITE_API_URL

# Executa o build de produção (gera a pasta /dist)
RUN npm run build

# Estágio 2: Servidor web de Produção (Nginx)
FROM nginx:1.27-alpine

# Copia os arquivos compilados do estágio 1 para a pasta do Nginx
COPY --from=build /app/dist /usr/share/nginx/html

# Copia a configuração customizada do Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
