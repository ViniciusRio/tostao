# Tasks

## 1. Domínio

- [ ] 1.1 `TransactionQueries.byCategory` com as regras do relatório (efetivadas, tipo, sem ajustes, órfãs em "Sem categoria")
- [ ] 1.2 Testes: soma igual à linha de `Reports.month`, "Sem categoria" com órfã, futura fora, ajuste fora, tipo oposto fora

## 2. Interface

- [ ] 2.1 `Route.CategoryTransactions(categoryId, type, month)` e entrada em `FinanceRoot`
- [ ] 2.2 Tela: barra superior com voltar, cabeçalho (ícone, nome, total, quantidade), `MonthSwitcher`, lista por dia com `TransactionRow`, estado vazio, valores ocultos
- [ ] 2.3 Voltar para Relatórios quando a categoria deixar de existir
- [ ] 2.4 `ReportsScreen`: linha navega; fatia segue selecionando; texto de ajuda e ação de acessibilidade atualizados
- [ ] 2.5 Mês e tipo de Relatórios no `MainViewModel`, preservados na volta

## 3. Verificação

- [ ] 3.1 Testes unitários, `./gradlew :app:lintDebug` e build
- [ ] 3.2 Conferir no aparelho: Mercado, Sem categoria, categoria em Outras, voltar mantendo mês/tipo
- [ ] 3.3 Revisão do Codex (somente leitura) e correções

## 4. Integração com o PR #5

- [ ] 4.1 Ajustar `grupos-categorias-e-soma-no-valor`: "Ver transações" do grupo usa esta tela estendida para grupo
- [ ] 4.2 Ajustar `relatorio-por-conta`: a conta filtrada é repassada para esta tela
