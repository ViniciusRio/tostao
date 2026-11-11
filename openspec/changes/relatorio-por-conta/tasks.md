# Tasks

## 1. Domínio

- [ ] 1.1 `Reports.month(..., accountId: String? = null)` e `Charts.trend(..., accountId: String? = null)`: com a conta, filtrar receitas/despesas por `accountId` no mês e no mês anterior. Verificar com testes em `BudgetsAndReportsTest`/`ChartsTest`: cenário BTG R$ 300,00 / Pix R$ 200,00, transferência BTG→Pix fora dos dois, ajuste de saldo fora, conta fora do saldo total incluída em "Todas", evolução por conta, e os testes atuais sem conta passando sem mudança.

## 2. Tela

- [ ] 2.1 `ReportsScreen`: `LazyRow` de chips "Todas as contas" e uma por conta (como em Transações), estado `rememberSaveable` mantido ao trocar mês/tipo e ao tocar numa barra, volta para todas se a conta sumir, e a seleção da pizza limpa ao trocar de conta. Verificar no app com duas contas: filtrar Pix, trocar para BTG, voltar para todas, navegar meses e conferir totais e pizza; e com "ocultar valores" ligado.
- [ ] 2.2 Atualizar o README (Relatórios por conta) e rodar `./gradlew :app:testDebugUnitTest :app:assembleDebug`, tudo verde.

## Workflow follow-up

- A ação "Ver transações" (change `grupos-categorias-e-soma-no-valor`) leva a conta escolhida aqui; isso está no requisito daquele change, e quem for implementado por último faz a ligação.
