# Testing Strategy

## Backend

**Unit tests** — JUnit 5 + Mockito. Test service layer in isolation (mock repositories).

**Integration tests** — `@SpringBootTest` + Testcontainers (PostgreSQL container). Test full request/response cycle via `MockMvc` with Spring Security Mocking (`@WithMockUser`).

Coverage target: Minimum **85%** on service and controller layers (enforced by `jacoco-maven-plugin`).

Key scenarios:
- Login with valid/invalid credentials
- JWT validation and expiry
- Permission denied (403) for unauthorized roles
- CRUD success and failure paths for all entities
- Appointment slot conflict detection (overlap rules)
- Cash flow entry creation and listing

## Frontend

**Unit & Component tests** — Vitest + React Testing Library (RTL). Test pages, components, hooks (e.g., `usePermission`), and contexts (e.g., `AuthContext`) in isolation.

Key scenarios:
- Login flow (success, wrong password, expired token)
- Admin CRUD for users, services, products, employees
- Customer books and cancels appointment
- PermissionGate hides/shows elements correctly
- Report page renders chart data
- Form validation errors display in pt-BR

## Running Tests

```bash
# Backend
cd salon-back
./mvnw test

# Frontend Unit/Component tests
cd salon-front
npm run test

# Frontend Coverage report
npm run test:coverage
```