# Proposal

## Why

Em Relatórios, tocar numa categoria só destaca a fatia na pizza. Para saber *o que* compôs os R$ 600,00 de Mercado, o Vinícius precisa ir a Transações, achar o mês e procurar uma a uma, porque lá não existe filtro por categoria.

## What Changes

- Tocar numa linha da lista "Por categoria" passa a abrir uma tela nova, **Transações da categoria**, com as transações daquela categoria no mês e no tipo (despesas ou receitas) do relatório.
- Tocar numa fatia da pizza continua só destacando, como hoje. O gráfico segue explorável sem sair da tela.
- A tela nova mostra a categoria no topo (ícone, nome, total e quantidade de transações), troca de mês e a lista agrupada por dia, no mesmo formato de Transações. Tocar numa transação abre a edição.
- Voltar de Transações da categoria devolve o relatório no mesmo mês e tipo em que estava.

Fora do escopo: filtro por categoria dentro da aba Transações, histórico de todos os meses de uma vez, abrir a tela a partir da fatia "Outras".

## Capabilities

### New Capabilities

### Modified Capabilities
- `monthly-reports`: o toque na linha da categoria abre a tela de transações da categoria; nova tela descrita como requisito de Relatórios.

## Impact

- Domínio: `TransactionQueries` ganha a consulta das transações de uma categoria no mês com as mesmas regras do relatório (efetivadas, sem transferências nem ajustes), para o total da tela bater com a linha do relatório.
- UI: nova rota `Route.CategoryTransactions` e nova tela; `ReportsScreen` passa a navegar no toque da linha; o mês e o tipo de Relatórios passam a sobreviver à ida e volta.
- Sem mudança de banco, backup ou dependências.
- Convive com o PR #5: o "Ver transações" de `grupos-categorias-e-soma-no-valor` deve reaproveitar esta tela (estendida para grupo) em vez de filtrar a aba Transações, e o filtro de conta de `relatorio-por-conta` deve ser repassado para ela. Isso fica anotado nas tarefas e será ajustado na spec do PR #5.
