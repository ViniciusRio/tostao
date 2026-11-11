# Tasks

## 1. Domínio

- [x] 1.1 `Reports.month(..., accountId: String? = null)` e `Charts.trend(..., accountId: String? = null)`: com a conta, filtrar receitas/despesas por `accountId` no mês e no mês anterior. Verificar com testes em `BudgetsAndReportsTest`/`ChartsTest`: cenário BTG R$ 300,00 / Pix R$ 200,00, transferência BTG→Pix fora dos dois, ajuste de saldo fora, conta fora do saldo total incluída em "Todas", evolução por conta, e os testes atuais sem conta passando sem mudança.

## 2. Tela

- [x] 2.1 `ReportsScreen`: `LazyRow` de chips "Todas as contas" e uma por conta (como em Transações), conta guardada no `MainViewModel` junto com mês e tipo (como em `transacoes-da-categoria`), mantida ao trocar mês/tipo, ao tocar numa barra e na volta da tela de transações da categoria, volta para todas se a conta sumir, e a seleção da pizza limpa ao trocar de conta. Verificar no app com duas contas: filtrar Pix, trocar para BTG, voltar para todas, navegar meses e conferir totais e pizza; e com "ocultar valores" ligado.
  - Verificado no A26 com a release 0.4.0 (versionCode 4) instalada por cima: PIX R$ 2.841,58 + BTG R$ 2.965,04 = Todas R$ 5.806,62 (9 + 43 = 52 transações); o filtro se mantém ao trocar de mês e ao voltar de Mercado (R$ 65,00 na tela e na linha); a pizza mostra só a conta. Com "ocultar valores" ligado e PIX escolhido, os totais e o resultado aparecem como "R$ •••••", a comparação como "comparação oculta" e a lista diz "valor oculto" ao leitor de tela; depois a opção foi desligada de novo.
- [x] 2.2 Atualizar o README (Relatórios por conta) e rodar `./gradlew :app:testDebugUnitTest :app:assembleDebug`, tudo verde.
  - Lint (`:app:lintDebug`): 0 erros, 22 avisos, 2 dicas, sem issue nova em relação à `main`.

## Workflow follow-up

- A tela de transações da categoria/grupo (`transacoes-da-categoria` e `grupos-categorias-e-soma-no-valor`) recebe a conta escolhida aqui no `accountId` da rota. Quem for implementado por último faz a ligação, com teste de igualdade entre o total da tela e a linha do relatório filtrado pela mesma conta.
