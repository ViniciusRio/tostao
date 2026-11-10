# Spec Delta

## Purpose

Permitir registrar uma transação direto da tela inicial do celular, com um toque, sem passar pela navegação do app, voltando ao ponto de partida depois.

## ADDED Requirements

### Requirement: Widget de nova despesa
O app SHALL oferecer um widget de tela inicial de 1x1 com ícone e o rótulo "Despesa". Tocar nele MUST abrir o formulário de nova despesa já pronto para digitar o valor. O widget MUST ter descrição para leitor de tela ("Nova despesa no Tostão") e MUST NOT mostrar valores monetários.

#### Scenario: Registrar pelo widget
- **WHEN** o usuário toca no widget, digita 45,90, escolhe "Alimentação" e salva
- **THEN** a despesa é registrada na última conta usada e o formulário fecha

### Requirement: Atalhos do ícone do app
Ao pressionar e segurar o ícone do Tostão, o sistema SHALL mostrar os atalhos "Nova despesa", "Nova receita" e "Transferência". Cada um MUST abrir o formulário do tipo correspondente.

#### Scenario: Atalho de receita
- **WHEN** o usuário segura o ícone e toca em "Nova receita"
- **THEN** abre o formulário com o tipo Receita selecionado e só categorias de receita

### Requirement: Voltar para a origem
O formulário aberto por widget ou atalho SHALL rodar separado da navegação do app. Salvar, tocar em voltar ou usar o gesto de voltar MUST fechar o formulário e devolver o usuário para onde ele estava, sem abrir a Início do app e sem alterar a tela em que o app principal estava. A gravação MUST terminar antes de o formulário fechar.

#### Scenario: Salvar volta para a tela inicial
- **WHEN** o usuário salva uma despesa aberta pelo widget
- **THEN** o celular volta para a tela inicial e aparece a confirmação "Despesa registrada"

#### Scenario: Cancelar
- **WHEN** o usuário abre o formulário pelo atalho e toca em voltar sem salvar
- **THEN** nada é registrado e o celular volta para onde estava

#### Scenario: App aberto em outra tela
- **WHEN** o app está aberto em Relatórios em segundo plano e o usuário registra uma despesa pelo widget
- **THEN** ao voltar ao app, ele continua em Relatórios, já com a nova despesa nos números

#### Scenario: Não aparece nos recentes
- **WHEN** o usuário salva pelo atalho e abre a lista de apps recentes
- **THEN** o formulário rápido não aparece como uma tarefa separada

### Requirement: Atalho sem conta
Sem nenhuma conta cadastrada, o widget e os atalhos SHALL abrir o app no cadastro de conta, explicando que é preciso uma conta antes de registrar.

#### Scenario: Primeiro uso pelo widget
- **WHEN** não existe conta e o usuário toca no widget
- **THEN** o app abre no formulário de nova conta com a mensagem "Crie uma conta antes de registrar transações"
