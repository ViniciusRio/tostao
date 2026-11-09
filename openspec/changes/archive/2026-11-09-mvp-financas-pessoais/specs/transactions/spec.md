# Spec Delta

## Purpose
Registrar e consultar movimentações de dinheiro: receitas, despesas e transferências entre contas.

## ADDED Requirements

### Requirement: Registrar transação
O app SHALL registrar receita ou despesa com valor maior que zero (até R$ 999.999.999,99), conta, data e hora, categoria opcional, título opcional (até 60 caracteres) e observação opcional (até 500 caracteres). O valor MUST ser armazenado em centavos inteiros.

#### Scenario: Despesa rápida
- **WHEN** o usuário toca em "Despesa", digita 45,90, escolhe "Alimentação" e salva
- **THEN** a despesa é registrada na conta selecionada com data e hora atuais

#### Scenario: Valor inválido
- **WHEN** o valor está vazio ou é zero
- **THEN** o botão Salvar fica desabilitado

### Requirement: Transferência entre contas
O app SHALL registrar transferência com conta de origem, conta de destino diferente e valor maior que zero (até R$ 999.999.999,99), sem categoria.

#### Scenario: Mesma conta
- **WHEN** origem e destino são a mesma conta
- **THEN** o app impede salvar e explica o motivo

### Requirement: Conta padrão
O formulário SHALL pré-selecionar a última conta usada; sem histórico, a primeira conta da ordem. Sem nenhuma conta, o app MUST levar o usuário a criar uma antes de registrar.

#### Scenario: Sem contas
- **WHEN** não existe conta e o usuário toca em adicionar transação
- **THEN** o app explica que é preciso uma conta e abre o cadastro de conta

### Requirement: Editar e excluir com desfazer
O app SHALL permitir editar todos os campos de uma transação. Ao excluir, MUST mostrar uma mensagem com a ação "Desfazer" por alguns segundos, que restaura a transação idêntica.

#### Scenario: Ações sucessivas
- **WHEN** o usuário exclui uma transação e, antes de tocar em "Desfazer", exclui outra
- **THEN** a mensagem passa a se referir apenas à última exclusão; a primeira não pode mais ser desfeita

#### Scenario: Desfazer exclusão
- **WHEN** o usuário exclui uma despesa e toca em "Desfazer"
- **THEN** a despesa volta com os mesmos dados e os saldos voltam ao valor anterior

### Requirement: Lista de transações
Datas e horas de transações SHALL ser interpretadas no fuso atual do aparelho para agrupar por dia e por mês. O app SHALL listar transações efetivadas do mês selecionado, da mais recente para a mais antiga, agrupadas por dia com cabeçalho relativo ("Hoje", "Ontem" ou data como "17 de set."), mostrando o total do dia. O usuário MUST poder navegar entre meses e filtrar por tipo e por conta.

#### Scenario: Navegar meses
- **WHEN** o usuário toca na seta de mês anterior
- **THEN** a lista mostra as transações do mês anterior e o título do mês muda

### Requirement: Busca
O app SHALL buscar transações por título, observação, nome da categoria ou valor, ignorando maiúsculas e acentos, em todos os meses.

#### Scenario: Buscar sem acento
- **WHEN** o usuário busca "farmacia"
- **THEN** aparecem transações com título "Farmácia"
