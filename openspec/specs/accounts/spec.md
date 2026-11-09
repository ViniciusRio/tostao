# accounts Specification

## Purpose
Permitir que o usuário cadastre as contas onde o dinheiro está (banco, carteira, cartão) e veja o saldo de cada uma e o total.

## Requirements

### Requirement: Cadastro de contas
O app SHALL permitir criar, editar e excluir contas com nome (1 a 40 caracteres, sem repetir outro nome ignorando maiúsculas), cor, ícone, saldo inicial (positivo, zero ou negativo, em módulo até R$ 999.999.999,99) e a opção "incluir no saldo total".

#### Scenario: Criar conta com saldo inicial
- **WHEN** o usuário cria a conta "Nubank" com saldo inicial R$ 1.250,00
- **THEN** a conta aparece na lista com saldo R$ 1.250,00

#### Scenario: Nome repetido
- **WHEN** o usuário tenta criar "nubank" já existindo "Nubank"
- **THEN** o app recusa e informa que o nome já existe

#### Scenario: Excluir conta com transações
- **WHEN** o usuário exclui uma conta que tem transações
- **THEN** o app pede confirmação informando quantas transações (inclusive transferências de e para a conta) e regras planejadas serão excluídas e, confirmado, remove tudo isso; o saldo da outra conta de cada transferência excluída é recalculado sem ela, e a confirmação avisa disso

### Requirement: Saldo calculado
O saldo de uma conta SHALL ser o saldo inicial mais receitas, menos despesas, menos transferências enviadas, mais transferências recebidas, considerando apenas transações efetivadas até agora. Valores MUST ser exatos em centavos.

#### Scenario: Saldo com transferência
- **WHEN** a conta A tem saldo inicial R$ 100,00, despesa de R$ 30,10 e transferência de R$ 20,00 para B
- **THEN** o saldo de A é R$ 49,90 e o de B aumenta R$ 20,00

#### Scenario: Transação futura não altera saldo
- **WHEN** existe uma despesa com data posterior ao momento atual
- **THEN** ela não entra no saldo atual

### Requirement: Saldo total
O saldo total SHALL somar apenas as contas marcadas para inclusão; a tela de contas MUST mostrar separadamente o total das contas excluídas.

#### Scenario: Conta fora do total
- **WHEN** a conta "Investimentos" está marcada para não incluir no total
- **THEN** o saldo total não a considera e ela aparece em "Fora do total"

### Requirement: Ajuste de saldo
O app SHALL permitir informar o saldo real de uma conta; a diferença MUST ser registrada como uma transação de ajuste (receita ou despesa sem categoria, título "Ajuste de saldo", marcada como ajuste). Ajustes MUST contar no saldo e aparecer no extrato, mas MUST NOT entrar em relatórios e orçamentos.

#### Scenario: Ajustar saldo para baixo
- **WHEN** a conta mostra R$ 500,00 e o usuário informa saldo real R$ 480,00
- **THEN** é criada uma despesa de R$ 20,00 "Ajuste de saldo" e o saldo passa a R$ 480,00

### Requirement: Ordem das contas
O usuário SHALL poder reordenar as contas, e a ordem MUST ser mantida em todas as listas e seletores de conta.

#### Scenario: Reordenar
- **WHEN** o usuário move "PIX" para o topo
- **THEN** "PIX" aparece primeiro na tela de contas e no seletor de conta da nova transação
