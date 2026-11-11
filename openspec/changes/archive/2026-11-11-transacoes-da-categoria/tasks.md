# Tasks

## 1. Domínio

- [x] 1.1 `TransactionQueries.byCategory` com as regras e o agrupamento exatos de `Reports.month` e `accountId` opcional
- [x] 1.2 Testes de igualdade com a linha de `Reports.month`: mês inicial e outro mês, "Sem categoria" com órfã, futura no mesmo dia e em dia seguinte, ajuste, transferência, tipo oposto, receita em categoria de despesa, virada do mês no fuso; `byCategory` com `accountId` filtra pela conta (a igualdade com o relatório por conta fica na 4.2)

## 2. Interface

- [x] 2.1 `Route.CategoryTransactions(categoryId, type, month, accountId = null)` e entrada em `FinanceRoot`
- [x] 2.2 Tela: barra superior com voltar, cabeçalho (ícone, nome, total, quantidade), `MonthSwitcher`, lista por dia com `TransactionRow` e subtotal sem sinal, estado vazio, valores ocultos
- [x] 2.3 Voltar para Relatórios quando uma categoria não nula deixar de existir
- [x] 2.4 `ReportsScreen`: linha navega; fatia segue selecionando; texto de ajuda e ação de acessibilidade atualizados
- [x] 2.5 Mês e tipo de Relatórios no `MainViewModel`, preservados na volta

## 3. Verificação

- [x] 3.1 Testes unitários, `./gradlew :app:lintDebug` e build
- [x] 3.2 Conferir no aparelho: Mercado, Sem categoria, categoria em Outras, voltar mantendo mês/tipo, rotação, TalkBack com e sem valores ocultos
- [x] 3.3 Revisão do Codex (somente leitura) e correções

## 4. Integração com o PR #5

- [x] 4.1 Antes de arquivar, conferir que as specs do PR #5 continuam coerentes com esta tela (rota, `accountId`, linha abre e fatia destaca). O ajuste das specs já foi feito no próprio #5. Conferido em 11/11: rota e `accountId` iguais (grupos troca `categoryId` por `scope` mantendo `type`, `month` e `accountId`), linha abre e fatia destaca nos dois changes. Ao implementar, a conta de `relatorio-por-conta` deve seguir o mesmo ciclo de mês e tipo (fixada na ida e volta, zerada ao trocar de aba em `navigate`), e o modo grupo deve manter o mês na rota (`replaceTop`).
- [x] 4.2 Na implementação de `relatorio-por-conta`: teste de igualdade entre `byCategory` e `Reports.month` com a mesma conta. Transferida para `relatorio-por-conta` (já prevista nas notas do tasks.md de lá); não é trabalho deste change.
