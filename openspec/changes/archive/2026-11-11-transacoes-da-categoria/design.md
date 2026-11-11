# Design

## Contexto

`ReportsScreen` guarda a seleção em `selectedKey` (`"slice:<i>"` ou `"cat:<id>"`). Linha e fatia hoje fazem a mesma coisa. A navegação é uma pilha simples em `MainViewModel.backStack` e `FinanceRoot` troca a tela num `when(route)`, sem `SaveableStateHolder`: ao sair de Relatórios, o `rememberSaveable` de mês e tipo é descartado e a tela volta ao mês atual e a Despesas.

`Reports.month` agrupa por `categoryId?.let(data.categoryById::get)`. Uma transação com categoria nula **ou** apontando para uma categoria inexistente cai em "Sem categoria".

## Decisões

### 1. Linha navega, fatia destaca
A linha da lista abre a tela; a fatia continua com a seleção atual. Assim o centro da rosca segue útil e o toque na lista tem uma ação clara. O texto de ajuda passa a ser "Toque numa fatia para ver o valor ou numa categoria para ver as transações." A ação de acessibilidade da linha passa de "Destacar no gráfico" para "Ver transações".

Descartado: botão "Ver transações" sob a rosca (um toque a mais, e o Vinícius pediu o toque direto).

Consequência: o requisito "Seleção de fatia" perde os cenários em que o toque na **linha** seleciona. O destaque da linha quando a fatia correspondente está selecionada continua (inclusive as linhas dentro de "Outras").

### 2. Rota com parâmetros explícitos
```kotlin
data class CategoryTransactions(
    val categoryId: String?,   // null = "Sem categoria"
    val type: CategoryType,
    val month: String,         // YearMonth.toString()
    val accountId: String? = null, // null = todas as contas
) : Route
```
`accountId` existe desde já para não mudar a rota depois: hoje Relatórios sempre passa `null`; quando `relatorio-por-conta` (PR #5) for implementado, passa a conta filtrada.
Não é `Tab`: a barra inferior some e o botão voltar do sistema volta a Relatórios. A tela tem barra superior com seta de voltar e o nome da categoria.

### 3. Consulta no domínio, mesmas regras do relatório
```kotlin
fun byCategory(data, categoryId: String?, type: CategoryType, month, zone, nowMillis, accountId: String? = null): List<Transaction>
```
Reproduz exatamente o recorte e o agrupamento de `Reports.month`: intervalo de `Periods.monthRange(month, zone)`, `dateTimeMillis <= nowMillis` (o instante, não o dia), tipo da transação igual ao pedido (transferências nunca entram), `!isAdjustment`, e a chave `categoryId?.let(data.categoryById::get)?.id`, para que órfãs entrem em "Sem categoria". Como o relatório, **não** confere `Category.type`: uma receita numa categoria de despesa aparece em Receitas, como já acontece na lista do relatório. Com `accountId`, filtra por `accountId` da transação. Ordem decrescente de data.

Contrato de total: para quaisquer dados, mês, tipo e conta, a soma de `byCategory` MUST ser igual ao `totalCents` da linha correspondente de `Reports.month` com os mesmos parâmetros (ou zero, se a linha não existir). A tela recalcula a partir dos dados atuais, então o contrato vale também depois de trocar o mês ou editar uma transação. Testes cobrem isso.

Instante de corte: Relatórios e a tela usam o mesmo `MainViewModel.reportAsOf`, renovado a cada minuto e a cada mudança de dados. Com chamadas separadas a `nowMillis()`, uma transação agendada que se efetivasse entre o cálculo da linha e a abertura da tela entraria só na tela.

Subtotal por dia: nesta tela os itens são todos do mesmo tipo, então o subtotal do dia é a soma em valor absoluto, sem sinal, como o total do cabeçalho e a linha do relatório. Não reaproveita o `netCents` assinado de `groupByDay`.

### 4. Troca de mês dentro da tela
A tela tem `MonthSwitcher` próprio, iniciado no mês da rota. Trocar o mês ali não altera Relatórios. Mês sem transações mostra estado vazio ("Nenhuma despesa em Mercado neste mês.").

### 5. Relatórios lembra mês e tipo
Mês e tipo de Relatórios saem do `rememberSaveable` local e vão para o `MainViewModel` como `MutableStateFlow` (`reportMonth`, `reportKind`). O `MainViewModel` não tem `SavedStateHandle` e a pilha de navegação vive só em memória. Resultado:
- ida e volta e rotação: mês e tipo preservados (o ViewModel sobrevive);
- trocar de aba pela barra inferior zera mês e tipo, como antes (só a ida e volta de uma rota filha preserva);
- morte do processo: o app reabre em Início, como hoje para qualquer tela, e Relatórios volta ao mês atual. Restaurar pilha e estado após morte do processo fica fora do escopo.

A seleção da fatia pode ser perdida na volta. Ao abrir uma categoria, o mês exibido em Relatórios é fixado em `reportMonth`, para que a virada do dia com a tela aberta não mude o mês na volta.

O mês escolhido dentro da tela nova vive na própria rota (`month`, trocada no topo da pilha por `replaceTop`), não em `rememberSaveable`: sem `SaveableStateHolder`, o estado salvo se perde quando a tela sai da composição para a edição de uma transação. Assim o mês sobrevive à ida e volta da edição e à rotação, sem afetar Relatórios.

Descartado: `SaveableStateHolder` em `FinanceRoot` para todas as rotas. Resolveria em geral, mas muda o comportamento das outras abas (hoje voltam ao mês atual) e é maior que o necessário.

### 6. Categoria excluída com a tela aberta
Só para `categoryId` não nulo: se a categoria deixar de existir em `data.categoryById` enquanto a tela está aberta, a tela chama `back()` e volta a Relatórios. A rota "Sem categoria" (`null`) nunca é expulsa. A restauração de backup não passa por aqui: quando conclui, ela já limpa a pilha e abre Início (`MainViewModel.restoreFrom`), e isso se mantém.

### 7. Valores ocultos
Com "ocultar valores", total e valores das linhas aparecem como "R$ •••••", como no resto do app.

### 8. Integração com o PR #5
O PR #5 já foi alinhado a esta tela (commits `a20c014` e seguintes em `docs/propostas-relatorio-conta-e-grupos`):
- `grupos-categorias-e-soma-no-valor`: tocar na linha de um grupo abre esta tela em modo grupo (rota com `scope` no lugar de `categoryId`); tocar numa categoria expandida abre a da categoria; expandir é um botão próprio; "Ver transações" saiu. O filtro por categoria/grupo da aba Transações continua, como função independente.
- `relatorio-por-conta`: passa a conta filtrada no `accountId` da rota e mantém o filtro na volta.

Por isso a rota já nasce com `accountId` e a consulta com o filtro opcional de conta. A parte de Relatórios do #5 depende deste change mesclado e arquivado (tarefa 0.1 de lá).

## Riscos

- Performance: a consulta percorre todas as transações em memória, como `TransactionQueries.filtered`. Mesmo custo do que já existe.
- Edição de uma transação a partir da tela nova pode mudar a categoria dela; ao voltar, ela some da lista. É o esperado.
