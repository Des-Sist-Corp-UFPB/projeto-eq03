# Cristiane Moura – Beauty Salon SaaS

Full-featured SaaS for managing a beauty salon, covering admin operations, online booking, employee management, and financial reporting. All system-facing text output (UI labels, messages, notifications) must be in **pt-BR**.

## Stack

**Backend:** Java 21 · Spring Boot 3.4.6 · Spring Security · JWT · Spring Data JPA · PostgreSQL · Flyway · Lombok · Spring Validation · Springdoc OpenAPI · Maven

**Frontend:** React 18 · TypeScript · Vite · Bootstrap 5 · React-Bootstrap · Axios · React Router DOM · React Hook Form · Recharts · jsPDF

**Infra:** Docker · Docker Compose · GitHub Actions · Nginx · Linux VPS

## Monorepo Structure

```
salao-cristiane/
├── salon-back/      # Spring Boot API
├── salon-front/     # React SPA
├── docs/            # Project documentation
├── docker-compose.yml
└── README.md
```

## Prerequisites

- Java 21, Maven 3.9+
- Node.js 20+, npm 10+
- PostgreSQL 16+ (or Docker)
- Docker & Docker Compose

## Running Locally

```bash
# 1. Clone
git clone https://github.com/Elksandro2/sass-salon.git
cd sass-salon

# 2. Start database
docker compose up db -d

# 3. Backend
cd salon-back
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 4. Frontend
cd salon-front
npm install
npm run dev
```

| Service  | URL                              |
|----------|----------------------------------|
| Frontend | http://localhost:5173            |
| Backend  | http://localhost:8080            |
| Swagger  | http://localhost:8080/swagger-ui.html |

## Application Profiles

**dev** – local DB, verbose logs, Swagger enabled, CORS open to localhost  
**prod** – env vars, reduced logs, strict CORS, hardened security

## Features

**Public area:** view services & products, online booking, customer registration, login

**Customer area:** booking history, profile management, cancel appointments

**Admin area:** dashboard, user/employee/service/product management, cash flow, reports, permission control

## Code Conventions

- DTOs → Java `record` (named `UserRequest` / `UserResponse`, never `UserDTO`)
- Entities → Lombok (`@Getter @Setter @Entity @Table`)
- All endpoints versioned under `/v1`
- Security: JWT + Roles + granular Authorities per endpoint/HTTP method

## Roadmap

- WhatsApp notifications
- Image upload (S3/MinIO)
- Advanced reports & exports
- Multi-tenant support
- Inventory management
- Automatic commission calculation