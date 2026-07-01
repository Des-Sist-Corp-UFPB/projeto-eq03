# Avaliação — EQ03 (DSC)

**Data:** 2026-07-01  
**Avaliador:** Prof. Rodrigo  
**Método:** verificação automática cruzando o que o `README.md` declara com evidências no código-fonte (leitura de `origin/main`).

> Esta é uma avaliação automática preliminar. O que não estiver documentado no README e commitado no repositório é considerado não atendido.

---

## 1. Log de Auditoria

✅ **Atendido** — documentado no README e com 516 evidência(s) no código.

---

## 2. Integração com Serviço Externo

- ✅ **Mercado Pago** — declarado no README e comprovado no código (80 ocorrência(s)).
  - Evidência: `salon-back/pom.xml:151:            <groupId>com.mercadopago</groupId>`
- ✅ **Resend** — declarado no README e comprovado no código (2 ocorrência(s)).
  - Evidência: `salon-back/src/main/resources/application-prod.yaml:19:  api-url: ${MAIL_API_URL:https://api.resend.com}`

_Detectado no código, mas **não documentado** no README (não pontua até ser descrito):_
- ℹ️ SMTP / e-mail

---

## 3. Cobertura de Testes (≥ 85%)

✅ **Atendido** — backend linhas 92.8% (instruções 93.6% · ramos 78.6%) [JaCoCo]; frontend linhas 99.15% (JS) (relatório em `cobertura/`, 334 arquivo(s)).

> Critério: **cobertura de linhas** ≥ 85% (conforme a orientação). As demais métricas (instruções/ramos) são informativas.

> Observação: a cobertura é lida do relatório commitado pela equipe; não é recalculada nesta avaliação.

---

*Avaliação gerada automaticamente em 2026-07-01. Consulte `ORIENTACOES-AVALIACAO-2026-06-29.md` para os critérios.*