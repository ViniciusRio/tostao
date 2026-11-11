# Spec Delta

## MODIFIED Requirements

### Requirement: Seleção de fatia
Tocar numa fatia SHALL destacar essa fatia e mostrar no centro o nome, o valor e o percentual da categoria, e MUST destacar na lista a linha (ou as linhas, no caso de "Outras") correspondente. Tocar de novo na mesma fatia, ou fora do anel, MUST voltar a mostrar o total. Trocar de mês ou de tipo MUST limpar a seleção. Tocar numa linha da lista MUST NOT alterar a seleção: abre as transações da categoria (ver "Transações da categoria").

#### Scenario: Tocar em Mercado
- **WHEN** o usuário toca na fatia de Mercado
- **THEN** a fatia fica destacada, a linha de Mercado fica destacada e o centro mostra "Mercado", "R$ 600,00" e "60%"

#### Scenario: Tocar numa categoria agrupada na lista
- **WHEN** o usuário toca na linha "Saúde" (1%), que está dentro de "Outras"
- **THEN** a seleção da pizza não muda e abre a tela de transações de Saúde

#### Scenario: Tocar no espaço entre fatias ou no centro
- **WHEN** o usuário toca no vão entre duas fatias ou no buraco da rosca
- **THEN** a seleção é limpa e o centro volta ao total

#### Scenario: Tocar em Outras
- **WHEN** o usuário toca na fatia "Outras"
- **THEN** o centro mostra "Outras", a soma das categorias agrupadas e o percentual somado, e as linhas dessas categorias ficam destacadas

## ADDED Requirements

### Requirement: Transações da categoria
Tocar numa linha da lista por categoria em Relatórios SHALL abrir a tela "Transações da categoria" com as transações daquela categoria no mês e no tipo do relatório, seguindo as mesmas regras dos totais (só efetivadas, sem transferências nem ajustes de saldo). A tela MUST mostrar no topo o ícone, o nome, o total e a quantidade de transações, e abaixo a lista agrupada por dia, mais recentes primeiro. Tocar numa transação MUST abrir a edição dela.

#### Scenario: Abrir Mercado
- **WHEN** novembro tem 3 despesas em Mercado somando R$ 600,00 e o usuário toca na linha Mercado
- **THEN** abre "Mercado" com R$ 600,00, "3 transações" e as 3 despesas agrupadas por dia

#### Scenario: Sem categoria
- **WHEN** o usuário toca na linha "Sem categoria"
- **THEN** a tela lista as transações do tipo sem categoria no mês, incluindo as que apontam para uma categoria que não existe mais

#### Scenario: Categoria agrupada em Outras
- **WHEN** Saúde (1%) está dentro da fatia "Outras" e o usuário toca na linha Saúde
- **THEN** a tela abre só com as transações de Saúde

#### Scenario: Só o tipo do relatório
- **WHEN** a categoria tem despesas e receitas no mês e o relatório está em Receitas
- **THEN** a tela lista só as receitas

#### Scenario: Valores ocultos
- **WHEN** "ocultar valores" está ligado
- **THEN** o total e os valores das transações aparecem como "R$ •••••"

#### Scenario: Leitor de tela na linha
- **WHEN** o TalkBack foca a linha Mercado em Relatórios
- **THEN** anuncia o nome, o valor e o percentual, com a ação "Ver transações"

#### Scenario: Leitor de tela com valores ocultos
- **WHEN** "ocultar valores" está ligado e o TalkBack foca a linha Mercado
- **THEN** anuncia o nome, "valor oculto" e o percentual, com a ação "Ver transações"

#### Scenario: Voltar
- **WHEN** o relatório estava em agosto, em Receitas, e o usuário abre uma categoria e volta
- **THEN** Relatórios mostra agosto, em Receitas

#### Scenario: Categoria removida
- **WHEN** a categoria deixa de existir enquanto a tela está aberta
- **THEN** o app volta para Relatórios

#### Scenario: Sem categoria não é removida
- **WHEN** a tela "Sem categoria" está aberta e os dados mudam
- **THEN** a tela continua aberta, recalculada

### Requirement: Total da tela da categoria
O total da tela "Transações da categoria" SHALL ser igual ao valor da linha da mesma categoria em Relatórios para o mês, o tipo e os dados atuais, inclusive depois de trocar o mês na tela ou editar uma transação. O subtotal de cada dia MUST ser exibido sem sinal, como o total.

#### Scenario: Subtotal do dia
- **WHEN** em 10/11 há duas despesas de Mercado de R$ 50,00 e R$ 30,00
- **THEN** o subtotal do dia aparece como R$ 80,00, sem sinal de menos

#### Scenario: Transação futura
- **WHEN** existe uma despesa de Mercado agendada para mais tarde hoje ou para depois de hoje no mês atual
- **THEN** ela não aparece na tela nem entra no total, como no relatório

#### Scenario: Trocar de mês na tela
- **WHEN** o usuário está em "Mercado" de novembro e toca na seta do mês anterior
- **THEN** a tela passa a mostrar Mercado de outubro; sem despesas, mostra o estado vazio

#### Scenario: Editar e voltar
- **WHEN** o usuário abre uma transação da tela, muda a categoria dela e salva
- **THEN** ao voltar a transação não aparece mais na lista e o total é recalculado
