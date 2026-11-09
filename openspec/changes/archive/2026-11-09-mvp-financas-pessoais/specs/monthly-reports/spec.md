# Spec Delta

## Purpose
Mostrar para onde o dinheiro foi e de onde veio em cada mês, sem configuração prévia.

## ADDED Requirements

### Requirement: Relatório mensal padrão
Ao abrir Relatórios, o app SHALL mostrar o mês atual sem exigir filtro: total de receitas, total de despesas, resultado (receitas menos despesas) e quantidade de transações. Transferências e ajustes de saldo MUST NOT entrar nesses totais nem na comparação mensal.

#### Scenario: Abrir relatório
- **WHEN** o usuário abre Relatórios
- **THEN** vê os totais do mês atual imediatamente

### Requirement: Despesas por categoria
O relatório SHALL listar despesas (e, em aba separada, receitas) agrupadas por categoria em ordem decrescente de valor, com valor, percentual do total e barra proporcional; transações sem categoria MUST aparecer em "Sem categoria".

#### Scenario: Ranking de categorias
- **WHEN** o mês tem R$ 600,00 em Mercado e R$ 400,00 em Lazer
- **THEN** Mercado aparece primeiro com 60% e Lazer com 40%

### Requirement: Comparação com mês anterior
O relatório SHALL mostrar a variação das despesas totais em relação ao mês anterior, em valor e percentual.

#### Scenario: Gasto maior
- **WHEN** o mês anterior teve R$ 1.000,00 de despesas e o atual R$ 1.200,00
- **THEN** o app mostra "+R$ 200,00 (+20%) em relação ao mês anterior"

#### Scenario: Mês anterior sem despesas
- **WHEN** o mês anterior não teve despesas
- **THEN** o app informa que não há base de comparação, sem mostrar percentual
