# Memória do Projeto — Sistema de Chamados DSC/UFPB

## Identidade do Projeto
- **Nome**: Sistema de Chamados — Projeto Base DSC
- **Disciplina**: Desenvolvimento de Sistemas Corporativos
- **Professor**: Rodrigo Rebouças
- **Instituição**: Universidade Federal da Paraíba — Campus IV
- **Propósito**: Boilerplate educacional adaptado para gerenciamento de chamados com entidade Usuario
- **Status Atual**: Etapa 1 concluída - CRUD de Usuários implementado

## Stack Técnica
| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Linguagem | Java | 21 |
| Framework | Spring Boot | 3.4.5 |
| Build | Maven | 3.9+ |
| Templates | Thymeleaf + HTMX | 3.x + 2.0.4 |
| Frontend | Bootstrap | 5.3.3 |
| Banco | PostgreSQL | 16 |
| Migrations | Flyway | 11.x |
| Segurança | Spring Security | 6.x |
| Testes | JUnit 5 + Testcontainers | - |

## Estrutura de Pacotes
```
br.ufpb.dsc.chamados
├── config/          # Configurações Spring (Security, Web, etc.)
├── controller/      # Controllers MVC (recebem requests HTTP)
│   ├── AuthController.java      # Login/autenticação
│   └── UsuarioController.java   # CRUD de Usuários (NOVO)
├── domain/          # Entidades JPA (mapeamento objeto-relacional)
│   └── Usuario.java             # Entidade Principal (NOVO)
├── dto/             # Data Transfer Objects (Records Java)
│   └── UsuarioForm.java         # DTO para Usuários (NOVO)
├── exception/       # Exceções de domínio
│   └── UsuarioNaoEncontradoException.java  # (NOVO)
├── repository/      # Interfaces Spring Data JPA
│   └── UsuarioRepository.java   # (NOVO)
└── service/         # Lógica de negócio (@Transactional)
    └── UsuarioService.java      # (NOVO)
```

## Entidade Usuario (Etapa 1)
- **Tabela**: `usuario`
- **Campos**:
  - `id` (Long, PK, auto-incremento)
  - `matricula` (String, UNIQUE, NOT NULL)
  - `nomeCompleto` (String, NOT NULL)
  - `senha` (String, NOT NULL)
  - `email` (String, nullable)
  - `ativo` (Boolean, default true)
  - `created_at`, `updated_at` (timestamps)

## Comandos Essenciais

### Desenvolvimento
```bash
# Subir ambiente completo (banco + app + adminer)
docker compose -f docker/docker-compose.dev.yml up

# Só o banco (para rodar a app localmente com mvn)
docker compose -f docker/docker-compose.dev.yml up postgres adminer

# Rodar aplicação local (perfil dev)
mvn spring-boot:run

# Rodar testes (requer Docker para Testcontainers)
mvn test
```

### Build e Verificações
```bash
# Build sem testes
mvn clean package -DskipTests

# Build com testes
mvn clean verify

# SAST: SpotBugs + FindSecBugs + OWASP Dependency Check
mvn verify -Psecurity

# Verificar dependências desatualizadas
mvn versions:display-dependency-updates -Pversions

# Trivy local (scan filesystem)
docker compose -f docker/docker-compose.dev.yml --profile scan up trivy

# Trivy scan da imagem (depois de fazer o build)
docker build -f docker/Dockerfile -t chamados:latest .
docker run --rm aquasec/trivy image chamados:latest
```

### Produção
```bash
# Build imagem de produção
docker build -f docker/Dockerfile -t chamados:latest .

# Subir produção (requer .env configurado)
docker compose -f docker/docker-compose.prod.yml up -d
```

## Acesso Local
- **App**: http://localhost:8080
- **Login**: admin / admin123
- **Adminer (DB UI)**: http://localhost:8888
- **Health Check**: http://localhost:8080/actuator/health

## Decisões Arquiteturais

### Por que HTMX em vez de React/Vue?
HTMX permite interatividade Ajax sem JavaScript customizado. Para um projeto educacional, reduz a curva de aprendizado mantendo a aplicação no paradigma server-side que os alunos já conhecem.

### Por que Flyway para migrations?
Controle versionado do schema do banco. Cada alteração no banco deve ser uma migration nova (nunca editar migrations já aplicadas). Garante rastreabilidade e reversibilidade.

### Por que InMemoryUserDetailsManager?
Simplifica o onboarding dos alunos. Para projetos reais, trocar por UserDetailsService com banco de dados.

### Por que perfil 'security' separado?
SpotBugs e OWASP Dependency-Check são lentos. Separar em perfil permite que o build do dia-a-dia seja rápido, rodando segurança no CI.

## Convenções de Código
- Nomes em português no domínio (entidades, métodos de negócio)
- Endpoints REST em português
- Comentários em português
- Commits no padrão Conventional Commits: `feat:`, `fix:`, `docs:`, `refactor:`
- Records Java para DTOs (imutáveis por padrão)
- `@Transactional(readOnly = true)` em métodos de consulta

## Ferramentas de Segurança
| Ferramenta | Escopo | Comando |
|------------|--------|---------|
| SpotBugs + FindSecBugs | SAST bytecode Java | `mvn verify -Psecurity` |
| Semgrep | SAST código-fonte | `semgrep --config=auto src/` |
| Trivy (fs) | Vulnerabilidades em libs | docker compose `--profile scan` |
| Trivy (image) | Vulnerabilidades na imagem Docker | `trivy image chamados:latest` |
| OWASP Dependency-Check | CVEs em dependências | `mvn verify -Psecurity` |

## Para Alunos: Próximos Passos Sugeridos
1. Renomear `Produto` para sua entidade principal
2. Adicionar campos específicos do seu domínio
3. Criar novas migrations Flyway para as alterações no banco
4. Adicionar novos controllers seguindo o padrão HTMX
5. Configurar autenticação baseada em banco de dados
6. Adicionar testes para cada service criado
