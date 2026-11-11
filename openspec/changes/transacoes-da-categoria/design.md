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
) : Route
```
Não é `Tab`: a barra inferior some e o botão voltar do sistema volta a Relatórios. A tela tem barra superior com seta de voltar e o nome da categoria.

### 3. Consulta no domínio, mesmas regras do relatório
```kotlin
fun byCategory(data, categoryId: String?, type: CategoryType, month, zone, nowMillis): List<Transaction>
```
Filtra como `Reports.month`: no mês, efetivadas (`<= nowMillis`), do tipo pedido, `!isAdjustment`, e com a mesma chave de categoria (`categoryId?.let(data.categoryById::get)?.id`), para que órfãs entrem em "Sem categoria". Ordem decrescente de data. O total exibido é a soma dessa lista e MUST bater com a linha do relatório; há teste para isso.

### 4. Troca de mês dentro da tela
A tela tem `MonthSwitcher` próprio, iniciado no mês da rota. Trocar o mês ali não altera Relatórios. Mês sem transações mostra estado vazio ("Nenhuma despesa em Mercado neste mês.").

### 5. Relatórios lembra mês e tipo
Mês e tipo de Relatórios saem do `rememberSaveable` local e vão para o `MainViewModel` (`reportMonth`, `reportKind`, em `SavedStateHandle` se já houver; senão em `MutableStateFlow`). Assim voltar da tela nova devolve o mesmo mês e tipo. A seleção da fatia pode ser perdida na volta.

Descartado: `SaveableStateHolder` em `FinanceRoot` para todas as rotas. Resolveria em geral, mas muda o comportamento das outras abas (hoje voltam ao mês atual) e é maior que o necessário.

### 6. Categoria excluída com a tela aberta
Se a categoria da rota deixar de existir enquanto a tela está aberta (excluída em outro lugar ou por restauração de backup), a tela volta para Relatórios. Não há caminho comum para isso hoje, mas a restauração de backup troca todos os dados.

### 7. Valores ocultos
Com "ocultar valores", total e valores das linhas aparecem como "R$ •••••", como no resto do app.

## Riscos

- Performance: a consulta percorre todas as transações em memória, como `TransactionQueries.filtered`. Mesmo custo do que já existe.
- Edição de uma transação a partir da tela nova pode mudar a categoria dela; ao voltar, ela some da lista. É o esperado.
