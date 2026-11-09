# Spec Delta

## Purpose
Definir limites mensais de gasto por categoria e acompanhar o consumo.

## ADDED Requirements

### Requirement: Orçamentos mensais
O app SHALL permitir criar, editar e excluir orçamentos com nome, valor limite mensal maior que zero (até R$ 999.999.999,99) e escopo: "todas as despesas" (opção explícita) ou uma ou mais categorias de despesa. Um orçamento por categorias MUST NOT ficar sem categorias.

#### Scenario: Orçamento de mercado
- **WHEN** o usuário cria "Mercado" de R$ 800,00 com a categoria Mercado
- **THEN** o orçamento aparece com gasto do mês atual e quanto falta

### Requirement: Progresso do orçamento
O gasto de um orçamento SHALL ser a soma das despesas efetivadas do mês selecionado nas categorias do orçamento. O app MUST mostrar gasto, limite, restante e percentual, com estado normal (abaixo de 80%), atenção (80% a 100%) e excedido (acima de 100%), indicado por texto e não apenas por cor.

#### Scenario: Orçamento excedido
- **WHEN** o limite é R$ 800,00 e o gasto R$ 850,00
- **THEN** o app mostra "Excedido em R$ 50,00"

#### Scenario: Excluir a última categoria de um orçamento
- **WHEN** o usuário exclui a categoria "Mercado", única do orçamento "Mercado"
- **THEN** a confirmação avisa que o orçamento será excluído e, confirmada, o orçamento é removido; orçamentos com outras categorias apenas perdem "Mercado"

### Requirement: Ajustes fora do orçamento
Transações de ajuste de saldo MUST NOT contar no gasto de nenhum orçamento, nem no de "todas as despesas".

#### Scenario: Ajuste não consome orçamento
- **WHEN** um ajuste de saldo gera despesa de R$ 20,00
- **THEN** o gasto do orçamento "todas as despesas" não muda
