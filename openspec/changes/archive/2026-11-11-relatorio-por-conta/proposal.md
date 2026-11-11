# Proposal

## Why

O Vinícius usa mais de uma conta (ex.: BTG e Pix) e quer entender os gastos de cada uma separadamente. Hoje Relatórios sempre soma todas as contas, e não há como ver o resumo, a pizza ou a evolução de uma conta só.

## What Changes

- Uma linha de filtro por conta no topo de Relatórios: "Todas as contas" (padrão, comportamento atual) e um chip por conta.
- Com uma conta escolhida, todo o relatório passa a considerar só as receitas e despesas daquela conta: totais do mês, resultado, quantidade de transações, comparação com o mês anterior, evolução de 6 meses, pizza e lista por categoria.
- A conta escolhida é mantida ao trocar de mês ou de tipo (despesas/receitas).
- A Início continua mostrando o total de todas as contas.

## Capabilities

### New Capabilities

### Modified Capabilities
- `monthly-reports`: filtro do relatório e dos gráficos por conta.

## Impact

- Domínio: `Reports.month` e `Charts.trend` ganham um parâmetro opcional de conta. Sem a conta, o resultado é o mesmo de hoje, então a Início e os testes atuais não mudam.
- UI: só `ReportsScreen` (linha de chips de conta, como em Transações).
- Sem mudança de banco, de backup nem de dependências.
- Convive com `transacoes-da-categoria` e `grupos-categorias-e-soma-no-valor`: a tela de transações aberta a partir de Relatórios recebe a conta escolhida, e quem for implementado por último faz a ligação.
