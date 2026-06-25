# Relatório de Avaliação — EQ03 (DSC)

| | |
|---|---|
| **Data** | 2026-06-25 |
| **Repositório** | https://github.com/des-sist-corp-ufpb/projeto-eq03 |
| **Aplicação** | https://eq03.dsc.rodrigor.com |
| **Período de atividade** | 2026-06-25 → 2026-06-25 |
| **Total de commits** (sem merges, branch main) | 11 |
| **Integrantes** | Anna Gabriela De Moura Souza (@AnnaGabrielaMS), Jose Elksandro Do Nascimento Silva (@Elksandro2) |

---

## 1. Tecnologias

- Thymeleaf
- Flyway (23 migrations)
- Spring Security
- JWT
- Lombok
- OpenAPI/Swagger
- Testcontainers

---

## 2. Análise Funcional

### Endpoints REST (62 mapeados)

| Método | Path | Arquivo |
|--------|------|---------|
| `GET` | `/v1/appointments` | `AppointmentController.java` |
| `GET` | `/v1/appointments/my` | `AppointmentController.java` |
| `GET` | `/v1/appointments/{id}` | `AppointmentController.java` |
| `PATCH` | `/v1/appointments/{id}/cancel` | `AppointmentController.java` |
| `PATCH` | `/v1/appointments/{id}/confirm` | `AppointmentController.java` |
| `PATCH` | `/v1/appointments/{id}/decline` | `AppointmentController.java` |
| `PATCH` | `/v1/appointments/{id}/payment-status` | `AppointmentController.java` |
| `PATCH` | `/v1/appointments/{id}/status` | `AppointmentController.java` |
| `POST` | `/v1/appointments` | `AppointmentController.java` |
| `POST` | `/v1/appointments/{id}/pix` | `AppointmentController.java` |
| `GET` | `/v1/audit` | `AuditController.java` |
| `GET` | `/v1/audit/action/{action}` | `AuditController.java` |
| `GET` | `/v1/audit/entity/{entityType}` | `AuditController.java` |
| `GET` | `/v1/audit/range` | `AuditController.java` |
| `GET` | `/v1/audit/user/{userId}` | `AuditController.java` |
| `GET` | `/v1/auth/me` | `AuthController.java` |
| `POST` | `/v1/auth/login` | `AuthController.java` |
| `POST` | `/v1/auth/refresh` | `AuthController.java` |
| `POST` | `/v1/auth/register` | `AuthController.java` |
| `DELETE` | `/v1/cashflow/{id}` | `CashFlowController.java` |
| `GET` | `/v1/cashflow` | `CashFlowController.java` |
| `POST` | `/v1/cashflow` | `CashFlowController.java` |
| `GET` | `/v1/clients` | `ClientController.java` |
| `GET` | `/v1/clients/{id}` | `ClientController.java` |
| `DELETE` | `/v1/employees/{id}` | `EmployeeController.java` |
| `GET` | `/v1/employees` | `EmployeeController.java` |
| `GET` | `/v1/employees/booking` | `EmployeeController.java` |
| `GET` | `/v1/employees/{id}` | `EmployeeController.java` |
| `POST` | `/v1/employees` | `EmployeeController.java` |
| `PUT` | `/v1/employees/{id}` | `EmployeeController.java` |
| `GET` | `/v1/feature-flags` | `FeatureFlagController.java` |
| `GET` | `/v1/sysadmin/feature-flags` | `FeatureFlagController.java` |
| `PATCH` | `/v1/sysadmin/feature-flags/{name}/toggle` | `FeatureFlagController.java` |
| `POST` | `/v1/webhooks/mercadopago` | `MercadoPagoWebhookController.java` |
| `GET` | `/ping` | `PingController.java` |
| `DELETE` | `/v1/products/{id}` | `ProductController.java` |
| `GET` | `/v1/products` | `ProductController.java` |
| `GET` | `/v1/products/{id}` | `ProductController.java` |
| `PATCH` | `/v1/products/{id}/reactivate` | `ProductController.java` |
| `POST` | `/v1/products` | `ProductController.java` |
| `PUT` | `/v1/products/{id}` | `ProductController.java` |
| `GET` | `/v1/reports/appointments` | `ReportController.java` |
| `GET` | `/v1/reports/financial` | `ReportController.java` |
| `GET` | `/v1/reports/payroll` | `ReportController.java` |
| `DELETE` | `/v1/roles/{roleId}/permissions/{permissionId}` | `RoleController.java` |
| `GET` | `/v1/roles` | `RoleController.java` |
| `GET` | `/v1/roles/permissions` | `RoleController.java` |
| `POST` | `/v1/roles/{roleId}/permissions/{permissionId}` | `RoleController.java` |
| `DELETE` | `/v1/services/{id}` | `SalonServiceController.java` |
| `GET` | `/v1/services` | `SalonServiceController.java` |
| `GET` | `/v1/services/{id}` | `SalonServiceController.java` |
| `PATCH` | `/v1/services/{id}/reactivate` | `SalonServiceController.java` |
| `POST` | `/v1/services` | `SalonServiceController.java` |
| `PUT` | `/v1/services/{id}` | `SalonServiceController.java` |
| `DELETE` | `/v1/users/{id}` | `UserController.java` |
| `GET` | `/v1/users` | `UserController.java` |
| `GET` | `/v1/users/details/id/{id}` | `UserController.java` |
| `GET` | `/v1/users/me/cpf-info` | `UserController.java` |
| `PATCH` | `/v1/users/me/cpf` | `UserController.java` |
| `PATCH` | `/v1/users/{id}` | `UserController.java` |
| `PATCH` | `/v1/users/{id}/restore` | `UserController.java` |
| `POST` | `/v1/users` | `UserController.java` |

### Entidades / Tabelas (21 encontradas)

- `tb_audit_log`
- `tb_employee`
- `tb_salon_service`
- `tb_feature_flag`
- `tb_role`
- `tb_permission`
- `tb_user`
- `tb_product`
- `tb_appointment`
- `tb_cashflow`
- `tb_role (via V1__create_security_tables.sql)`
- `tb_permission (via V1__create_security_tables.sql)`
- `tb_role_permissions (via V1__create_security_tables.sql)`
- `tb_user (via V1__create_security_tables.sql)`
- `tb_audit_log (via V7__create_audit_log_table.sql)`
- `tb_service (via V3__create_business_tables.sql)`
- `tb_product (via V3__create_business_tables.sql)`
- `tb_employee (via V3__create_business_tables.sql)`
- `tb_appointment (via V3__create_business_tables.sql)`
- `tb_cashflow (via V3__create_business_tables.sql)`
- `tb_feature_flag (via V9__add_sysadmin_role_and_feature_flags.sql)`

### Migrations (23 arquivos)

- `V10__update_admin_password.sql`
- `V11__make_audit_log_fields_nullable.sql`
- `V12__add_active_column_to_product.sql`
- `V13__add_remuneration_to_employee.sql`
- `V14__add_commission_value_to_employee.sql`
- `V15__add_enable_customer_portal_feature_flag.sql`
- `V16__set_sysadmin_role_for_sysadmin_user.sql`
- `V17__cleanup_audit_logs.sql`
- `V18__add_payment_fields_to_appointment.sql`
- `V19__add_cpf_to_user.sql`
- `V1__create_security_tables.sql`
- `V20__remove_cpf_unique_constraint.sql`
- `V21__add_client_permissions_and_update_endpoints.sql`
- `V22__add_rbac_role_permissions.sql`
- `V23__update_default_passwords.sql`
- `V2__insert_roles_permissions.sql`
- `V3__create_business_tables.sql`
- `V4__rename_service_table.sql`
- `V5__flexible_price_and_appointment_request.sql`
- `V6__service_duration_estimate.sql`
- `V7__create_audit_log_table.sql`
- `V8__add_permissions_to_roles.sql`
- `V9__add_sysadmin_role_and_feature_flags.sql`

---

## 3. Análise Arquitetural

| Aspecto | Status | Observação |
|---------|--------|-----------|
| Arquitetura em camadas | ✅ | controller=✅  service=✅  repository=✅ |
| Testes automatizados | ✅ | 43 Java, 14 JS/TS, 0 Python |
| Migrations versionadas | ✅ | 23 migration(s) |
| Logging | ✅ | @Slf4j / LoggerFactory / logging.getLogger detectado |
| Autenticação / Segurança | ✅ | Spring Security / JWT / decorator detectado |
| DTOs / Separação de dados | ✅ | classes *DTO / *Request / *Response detectadas |
| Tratamento global de exceções | ✅ | @ControllerAdvice / @ExceptionHandler detectado |
| Documentação de API (OpenAPI) | ✅ | springdoc / @Operation detectado |
| Variáveis de ambiente | ✅ | .env / @Value / os.environ detectado |
| Dockerfile / docker-compose | ✅ | presente |

---

## 4. Contribuição por Usuário

### Resumo

| Usuário | Commits (main) | Commits (GitHub API) | Linhas adicionadas | Linhas no código atual | % código atual |
|---------|---------------|---------------------|-------------------|----------------------|----------------|
| Anna Gabriela De Moura Souza (@AnnaGabrielaMS) | 4 | **75** ⚠️ | 46.078 | 32.161 | 100% |
| Jose Elksandro Do Nascimento Silva (@Elksandro2) | 5 | **202** ⚠️ | 79 | 63 | 0% |
| *(sem login GitHub)* | 2 | 18% | — | — | — |

> **⚠️ Divergência entre commits locais e GitHub API:**
> - **@AnnaGabrielaMS**: 4 commit(s) na branch `main` vs **75** registrados na API GitHub (commits em branches não mergeadas ou absorvidos via squash-merge sem preservação de autoria).
> - **@Elksandro2**: 5 commit(s) na branch `main` vs **202** registrados na API GitHub (commits em branches não mergeadas ou absorvidos via squash-merge sem preservação de autoria).
>

### Contribuição por Camada

| Camada | Total linhas | Anna Gabriela De Moura Souza (@AnnaGabrielaMS) | Jose Elksandro Do Nascimento Silva (@Elksandro2) |
|--------|-------------|---------|---------|
| Controller | 4.086 | 100% | 0% |
| Frontend | 2.500 | 100% | 0% |
| Repository | 182 | 100% | 0% |
| Service | 10.106 | 100% | 0% |
| Test | 2.217 | 100% | 0% |

---

## 5. Contribuição por Funcionalidade

Baseado em `git blame` nos arquivos de controller e service.

| Arquivo | Total linhas | Anna Gabriela De Moura Souza (@AnnaGabrielaMS) | Jose Elksandro Do Nascimento Silva (@Elksandro2) |
|---------|-------------|---------|---------|
| `AppointmentServiceTest.java` | 1.338 | 100% | 0% |
| `UserServiceTest.java` | 953 | 100% | 0% |
| `AppointmentService.java` | 555 | 100% | 0% |
| `ReportServiceTest.java` | 487 | 100% | 0% |
| `EmployeeServiceTest.java` | 394 | 100% | 0% |
| `CashFlowServiceTest.java` | 388 | 100% | 0% |
| `EmailServiceTest.java` | 367 | 100% | 0% |
| `MercadoPagoWebhookIntegrationTest.java` | 349 | 100% | 0% |
| `AuthServiceTest.java` | 346 | 100% | 0% |
| `AuditLogServiceTest.java` | 321 | 100% | 0% |
| `AdminServices.tsx` | 318 | 100% | 0% |
| `api.test.ts` | 290 | 100% | 0% |
| `UserService.java` | 289 | 100% | 0% |
| `SalonServiceManagerTest.java` | 287 | 100% | 0% |
| `EmailService.java` | 254 | 100% | 0% |
| `ReportService.java` | 247 | 100% | 0% |
| `ProductServiceTest.java` | 241 | 100% | 0% |
| `FeatureFlagServiceTest.java` | 237 | 100% | 0% |
| `Router.tsx` | 232 | 100% | 0% |
| `AppointmentControllerTest.java` | 186 | 100% | 0% |
| `AuditLogService.java` | 173 | 100% | 0% |
| `RoleServiceTest.java` | 173 | 100% | 0% |
| `JwtServiceTest.java` | 163 | 100% | 0% |
| `UserControllerTest.java` | 162 | 100% | 0% |
| `appointment-request.html` | 159 | 100% | 0% |
| `AuthService.java` | 158 | 100% | 0% |
| `CashFlowService.java` | 156 | 100% | 0% |
| `api.ts` | 152 | 100% | 0% |
| `EmployeeService.java` | 150 | 100% | 0% |
| `appointment-cancellation.html` | 146 | 100% | 0% |
| `MercadoPagoPaymentService.java` | 142 | 100% | 0% |
| `payment-confirmation.html` | 136 | 100% | 0% |
| `appointment-confirmation.html` | 132 | 100% | 0% |
| `AdminServices.test.tsx` | 129 | 100% | 0% |
| `SalonServiceControllerTest.java` | 127 | 100% | 0% |
| `appointments.ts` | 126 | 100% | 0% |
| `AuthControllerTest.java` | 125 | 100% | 0% |
| `ErrorScenariosTest.java` | 121 | 100% | 0% |
| `EmployeeControllerTest.java` | 119 | 100% | 0% |
| `ProductControllerTest.java` | 119 | 100% | 0% |
| `WebhookControllerTest.java` | 117 | 100% | 0% |
| `AppointmentController.java` | 108 | 100% | 0% |
| `AuditControllerTest.java` | 107 | 100% | 0% |
| `SalonServiceManager.java` | 104 | 100% | 0% |
| `users.ts` | 103 | 100% | 0% |
| `AuditController.java` | 99 | 100% | 0% |
| `RoleService.java` | 97 | 100% | 0% |
| `UserController.java` | 93 | 100% | 0% |
| `PublicServices.tsx` | 91 | 100% | 0% |
| `JwtService.java` | 87 | 100% | 0% |
| `CashFlowControllerTest.java` | 86 | 100% | 0% |
| `RoleControllerTest.java` | 81 | 100% | 0% |
| `ProductService.java` | 76 | 100% | 0% |
| `ReportControllerTest.java` | 76 | 100% | 0% |
| `reports.ts` | 74 | 100% | 0% |
| `EmployeeController.java` | 72 | 100% | 0% |
| `FeatureFlagControllerTest.java` | 72 | 100% | 0% |
| `SalonServiceController.java` | 71 | 100% | 0% |
| `ProductController.java` | 71 | 100% | 0% |
| `FeatureFlagService.java` | 70 | 100% | 0% |
| `ClientControllerTest.java` | 62 | 100% | 0% |
| `AuthController.java` | 61 | 100% | 0% |
| `SecurityUserDetailsServiceTest.java` | 60 | 100% | 0% |
| `MercadoPagoWebhookController.java` | 59 | 100% | 0% |
| `V8__add_permissions_to_roles.sql` | 58 | 100% | 0% |
| `services.ts` | 56 | 100% | 0% |
| `RoleController.java` | 54 | 100% | 0% |
| `ReportController.java` | 54 | 100% | 0% |
| `CashFlowController.java` | 52 | 100% | 0% |
| `BaseControllerTest.java` | 49 | 100% | 0% |
| `rbac.ts` | 49 | 100% | 0% |
| `employees.ts` | 45 | 100% | 0% |
| `V22__add_rbac_role_permissions.sql` | 44 | 100% | 0% |
| `SalonService.java` | 43 | 100% | 0% |
| `clients.ts` | 43 | 100% | 0% |
| `FeatureFlagController.java` | 42 | 100% | 0% |
| `ClientController.java` | 42 | 100% | 0% |
| `products.ts` | 42 | 100% | 0% |
| `V3__create_business_tables.sql` | 41 | 100% | 0% |
| `V9__add_sysadmin_role_and_feature_flags.sql` | 37 | 100% | 0% |
| `cashflow.ts` | 35 | 100% | 0% |
| `PingControllerTest.java` | 33 | 100% | 0% |
| `V1__create_security_tables.sql` | 31 | 100% | 0% |
| `SalonApplicationTest.java` | 31 | 100% | 0% |
| `SalonServiceRequest.java` | 30 | 100% | 0% |
| `SalonServiceResponse.java` | 27 | 100% | 0% |
| `SalonApplication.java` | 26 | 69% | 31% |
| `V2__insert_roles_permissions.sql` | 25 | 100% | 0% |
| `V7__create_audit_log_table.sql` | 24 | 100% | 0% |
| `V21__add_client_permissions_and_update_endpoints.sql` | 24 | 100% | 0% |
| `featureFlags.ts` | 24 | 100% | 0% |
| `SpaRedirectController.java` | 23 | 100% | 0% |
| `PingController.java` | 22 | 100% | 0% |
| `profile.ts` | 22 | 100% | 0% |
| `SecurityUserDetailsService.java` | 21 | 100% | 0% |
| `V5__flexible_price_and_appointment_request.sql` | 10 | 100% | 0% |
| `SalonServiceRepository.java` | 9 | 100% | 0% |
| `V23__update_default_passwords.sql` | 9 | 100% | 0% |
| `V10__update_admin_password.sql` | 9 | 100% | 0% |
| `V6__service_duration_estimate.sql` | 8 | 100% | 0% |
| `BusinessException.java` | 7 | 100% | 0% |
| `ResourceNotFoundException.java` | 7 | 100% | 0% |
| `V18__add_payment_fields_to_appointment.sql` | 6 | 100% | 0% |
| `V11__make_audit_log_fields_nullable.sql` | 5 | 100% | 0% |
| `V4__rename_service_table.sql` | 5 | 100% | 0% |
| `V13__add_remuneration_to_employee.sql` | 4 | 100% | 0% |
| `V16__set_sysadmin_role_for_sysadmin_user.sql` | 4 | 100% | 0% |
| `V12__add_active_column_to_product.sql` | 4 | 100% | 0% |
| `V19__add_cpf_to_user.sql` | 3 | 100% | 0% |
| `V20__remove_cpf_unique_constraint.sql` | 3 | 100% | 0% |
| `V15__add_enable_customer_portal_feature_flag.sql` | 3 | 100% | 0% |
| `V14__add_commission_value_to_employee.sql` | 2 | 100% | 0% |
| `V17__cleanup_audit_logs.sql` | 1 | 100% | 0% |

---

*Relatório gerado automaticamente em 2026-06-25.*
*Os dados de contribuição são baseados em `git log --numstat` (linhas adicionadas) e `git blame` (linhas no código atual), excluindo commits de merge.*