# Cristiane Moura – Gestão de Salão de Beleza

SaaS completo para gerenciamento de salão de beleza, com área administrativa (proprietária), área pública para agendamentos e perfis de funcionárias e clientes.

---

## Objetivo do Projeto

O sistema tem como objetivo centralizar toda a operação do salão de beleza Cristiane Moura em uma única plataforma, permitindo:

- Controle de clientes
- Gestão de funcionárias
- Gerenciamento de serviços
- Controle de produtos
- Agendamentos online
- Fluxo de caixa
- Relatórios financeiros
- Controle de permissões
- Área administrativa completa
- Área pública para clientes

O sistema será desenvolvido utilizando arquitetura moderna baseada em SPA no frontend e API REST no backend.

---

## Tecnologias Utilizadas

### Backend

- Java 21
- Spring Boot 3.4.6
- Spring Security
- JWT Authentication
- Spring Data JPA
- PostgreSQL
- Flyway
- Lombok
- Spring Validation
- Springdoc OpenAPI (Swagger)
- Maven

### Frontend

- React 18
- TypeScript
- Vite
- Bootstrap 5
- React-Bootstrap
- Axios
- React Router DOM
- React Hook Form
- Recharts
- jsPDF

### Infraestrutura

- Docker
- Docker Compose
- GitHub Actions
- Nginx
- VPS Linux

---

## Estrutura do Monorepo

```text
cristiane-moura/
├── backend/
├── frontend/
├── docs/
├── docker-compose.yml
└── README.md
```

---

## Pré-requisitos

Antes de iniciar o projeto, instale:

### Backend

- Java 21
- Maven 3.9+

### Frontend

- Node.js 20+
- npm 10+

### Banco de Dados

- PostgreSQL 16+

### Infraestrutura

- Docker
- Docker Compose

---

## Como Rodar o Projeto

### 1. Clonar o repositório

```bash
git clone https://github.com/seu-usuario/cristiane-moura.git
```

---

### 2. Entrar na pasta do projeto

```bash
cd cristiane-moura
```

---

### 3. Subir o banco de dados

```bash
docker compose up db -d
```

---

### 4. Rodar o backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

### 5. Rodar o frontend

```bash
cd frontend
npm install
npm run dev
```

---

## URLs do Sistema

### Frontend

```text
http://localhost:5173
```

### Backend

```text
http://localhost:8080
```

### Swagger

```text
http://localhost:8080/swagger-ui.html
```

---

## Perfis de Aplicação

### dev

Características:

- Banco local
- Logs detalhados
- Swagger habilitado
- CORS liberado para localhost
- Debug ativado

---

### prod

Características:

- Variáveis de ambiente
- Logs reduzidos
- Segurança reforçada
- CORS restrito
- Configuração otimizada

## Funcionalidades Principais

### Área Pública

- Visualizar serviços
- Visualizar produtos
- Realizar agendamentos
- Cadastro de clientes
- Login

### Área do Cliente

- Histórico de agendamentos
- Atualizar perfil
- Visualizar horários
- Cancelar agendamento

### Área Administrativa

- Dashboard
- Controle de usuários
- Controle de funcionárias
- Controle de serviços
- Controle de produtos
- Controle financeiro
- Relatórios
- Controle de permissões

---

## Padrões do Projeto

### DTOs

Todos os DTOs serão records.

Exemplo:

```java
public record UserRequest(
    String name,
    String email
) {}
```

---

### Entidades

Entidades utilizarão Lombok.

Exemplo:

```java
@Getter
@Setter
@Entity
@Table(name = "tb_user")
public class User {
}
```

---

### Controllers

Todos os endpoints serão versionados:

```text
/v1
```

### Segurança

O projeto utilizará:

- JWT
- Roles
- Permissões granulares
- Verificação por endpoint
- Verificação por método HTTP

---

## Roadmap

- Sistema de notificações
- Integração com WhatsApp
- Upload de imagens
- Relatórios avançados
- Multiempresa
- Controle de estoque avançado
- Comissões automáticas

---