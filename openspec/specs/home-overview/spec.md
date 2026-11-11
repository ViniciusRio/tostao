# home-overview Specification

## Purpose
Dar em uma tela a situação financeira atual e acesso rápido ao registro de transações, com formatação clara e acessível.

## Requirements

### Requirement: Painel inicial
A tela Início SHALL mostrar: saldo total; receitas e despesas do mês atual; pagamentos atrasados e próximos (até o fim do mês atual ou nos próximos 7 dias) com Pagar/Pular; e as 5 transações mais recentes com atalho para a lista completa.

#### Scenario: Primeiro uso
- **WHEN** não existe nenhuma conta
- **THEN** a Início mostra um estado vazio explicando o primeiro passo com o botão "Criar conta"

### Requirement: Saldo previsto
A tela Início SHALL mostrar o saldo previsto ao fim do mês atual, considerando só contas incluídas no total: saldo total, mais transações já registradas com data futura dentro do mês, mais receitas e menos despesas de **todas** as ocorrências planejadas pendentes até o último dia do mês, inclusive atrasadas e várias ocorrências da mesma regra, sem criá-las no extrato.

#### Scenario: Previsto com conta a pagar
- **WHEN** o saldo total é R$ 2.000,00 e há despesa planejada de R$ 500,00 neste mês
- **THEN** o saldo previsto é R$ 1.500,00

#### Scenario: Semanal e atrasado
- **WHEN** hoje é 10/11, o saldo é R$ 1.000,00 e uma despesa semanal de R$ 50,00 tem próxima ocorrência atrasada em 03/11
- **THEN** entram 03/11, 10/11, 17/11 e 24/11 e o saldo previsto é R$ 800,00

### Requirement: Formatação monetária
Todos os valores SHALL usar o formato brasileiro com símbolo antes ("R$ 1.234,56"); despesas MUST ter sinal de menos e receitas sinal de mais nas listas, além da cor.

#### Scenario: Saldo negativo
- **WHEN** o saldo total é -1234,50
- **THEN** o app mostra "-R$ 1.234,50" em cor de alerta

### Requirement: Ocultar valores
O app SHALL permitir ocultar os valores monetários da tela Início com um toque, mostrando "R$ •••••", e MUST lembrar a escolha entre aberturas.

#### Scenario: Ocultar saldo
- **WHEN** o usuário toca no ícone de olho
- **THEN** os valores da Início ficam ocultos até tocar de novo, inclusive depois de reabrir o app

### Requirement: Navegação e acessibilidade
O app SHALL ter barra inferior com Início, Transações, Planejamento (planejados e orçamentos) e Relatórios, mais um botão de adicionar (fora de Planejamento e Relatórios) que oferece Despesa, Receita e Transferência. Todo controle interativo MUST ter descrição para leitor de tela e área de toque mínima de 48 dp, e o tema MUST seguir o modo claro/escuro do sistema por padrão, com opção em Configurações para fixar Claro ou Escuro.

#### Scenario: Leitor de tela
- **WHEN** o TalkBack foca o botão de adicionar
- **THEN** anuncia "Adicionar transação"
