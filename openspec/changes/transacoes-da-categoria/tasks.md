# Tasks

## 1. Domínio

- [ ] 1.1 `TransactionQueries.byCategory` com as regras e o agrupamento exatos de `Reports.month` e `accountId` opcional
- [ ] 1.2 Testes de igualdade com a linha de `Reports.month`: mês inicial e outro mês, "Sem categoria" com órfã, futura no mesmo dia e em dia seguinte, ajuste, transferência, tipo oposto, receita em categoria de despesa, virada do mês no fuso, com conta

## 2. Interface

- [ ] 2.1 `Route.CategoryTransactions(categoryId, type, month)` e entrada em `FinanceRoot`
- [ ] 2.2 Tela: barra superior com voltar, cabeçalho (ícone, nome, total, quantidade), `MonthSwitcher`, lista por dia com `TransactionRow` e subtotal sem sinal, estado vazio, valores ocultos
- [ ] 2.3 Voltar para Relatórios quando uma categoria não nula deixar de existir
- [ ] 2.4 `ReportsScreen`: linha navega; fatia segue selecionando; texto de ajuda e ação de acessibilidade atualizados
- [ ] 2.5 Mês e tipo de Relatórios no `MainViewModel`, preservados na volta

## 3. Verificação

- [ ] 3.1 Testes unitários, `./gradlew :app:lintDebug` e build
- [ ] 3.2 Conferir no aparelho: Mercado, Sem categoria, categoria em Outras, voltar mantendo mês/tipo, rotação, TalkBack com e sem valores ocultos
- [ ] 3.3 Revisão do Codex (somente leitura) e correções

## 4. Integração com o PR #5 (pré-requisito para implementar o PR #5)

- [ ] 4.1 Alterar a spec de `grupos-categorias-e-soma-no-valor` conforme a decisão 8 do design (linha de grupo, categoria expandida, "Outras", remover "Ver transações do item")
- [ ] 4.2 Alterar a spec de `relatorio-por-conta`: repassar a conta no `accountId` da rota, com cenário e teste de igualdade de total
