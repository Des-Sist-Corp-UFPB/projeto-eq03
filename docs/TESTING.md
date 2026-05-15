# Testing Strategy

## Backend

**Unit tests** — JUnit 5 + Mockito. Test service layer in isolation (mock repositories).

**Integration tests** — `@SpringBootTest` + Testcontainers (PostgreSQL container). Test full request/response cycle via `MockMvc`.

Coverage target: **≥ 80%** on service and controller layers.

Key scenarios:
- Login with valid/invalid credentials
- JWT validation and expiry
- Permission denied (403) for unauthorized roles
- CRUD success and failure paths for all entities
- Appointment slot conflict detection
- Cash flow entry creation and listing

## Frontend

**Unit tests** — Vitest + React Testing Library. Test components in isolation (mock API calls).

**E2E tests** — Cypress. Runs against the dev environment.

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

# Frontend unit
cd salon-front
npm run test

# Frontend E2E (dev server must be running)
npm run cypress:open
```