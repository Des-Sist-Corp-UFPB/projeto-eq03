# Ideia de Servidor MCP — EQ03

**Domínio:** Gestão de salão de beleza (agendamento, PIX)  
**Data:** 2026-07-01

## O que é

Um **servidor MCP (Model Context Protocol)** expõe as operações do seu sistema como *tools* e *resources* que qualquer assistente de IA (Claude Desktop, Cursor, etc.) pode chamar com segurança. Na prática, é uma camada fina sobre a **API que vocês já têm** — cada tool chama um endpoint/service existente. Assim o projeto deixa de ser só uma tela e passa a ser operável por um agente de IA.

## Servidor proposto: `salon-mcp`

### Tools sugeridas

- `horarios_disponiveis(profissional, data)` — slots livres
- `agendar(servico, cliente, horario)` — cria agendamento
- `cancelar(agendamentoId)` — cancela
- `relatorio_financeiro(periodo)` — faturamento do período

### Resources (somente leitura)

- catálogo de serviços; relatório financeiro

### Exemplos de uso com um LLM

- "Tem horário com a Cristiane quinta de manhã? Se sim, agenda pra mim."
- "Qual foi o faturamento da última semana?"

## Esqueleto para começar (Java / Spring AI)

```java
// pom.xml: org.springframework.ai:spring-ai-starter-mcp-server-webmvc
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class SalonTools {

    private final SeuService seuService;   // injete seus services/repositories

    public SalonTools(SeuService seuService) { this.seuService = seuService; }

    @Tool(description = "slots livres")
    public Object horarios_disponiveis(/* params */) {
        return seuService.suaOperacaoExistente();   // reaproveite sua lógica
    }
}
```
> Registre as tools com um `MethodToolCallbackProvider` (bean) apontando para esta classe.

## Boas práticas

- **Segurança:** cada tool que altera dados deve exigir autenticação e registrar no **log de auditoria** (o mesmo do requisito da disciplina).
- **Escopo mínimo:** exponha só o necessário; separe tools de leitura das de escrita.
- **Reaproveite:** as tools devem chamar seus *services*/*controllers* existentes, não reimplementar regra de negócio.

## Referências
- Documentação MCP: https://modelcontextprotocol.io
- SDKs: Python (`mcp`), TypeScript (`@modelcontextprotocol/sdk`), Java (Spring AI MCP Server).

*Sugestão gerada em 2026-07-01 para orientar a integração de LLMs ao projeto.*